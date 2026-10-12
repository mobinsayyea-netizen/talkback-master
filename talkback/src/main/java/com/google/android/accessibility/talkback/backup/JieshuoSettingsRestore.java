package com.google.android.accessibility.talkback.backup;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import com.google.android.accessibility.talkback.editor.JxGestureApply;
import com.google.android.accessibility.talkback.editor.LuaPaths;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.json.JSONObject;

/**
 * Step 45: restores the settings and the gestures of a Jieshuo backup. What is mapped, and what is
 * left out and why, is decided by {@link JxSettingsMapper} and the gesture planner; this class reads
 * the files, applies the result and writes a full report next to the extensions.
 */
final class JieshuoSettingsRestore {
  private static final String TAG = "JieshuoSettings";
  private static final String REPORT_NAME = "Jieshuo restore report.txt";

  private JieshuoSettingsRestore() {}

  static void restore(Context context, File work, File settingsDat, JieshuoRestore.Result result) {
    Map<String, Object> jx = readSettings(settingsDat, result);
    if (jx == null) {
      return;
    }
    StringBuilder report = new StringBuilder("Jieshuo restore report\n\n");
    applySettings(context, jx, result, report);
    applyGestures(context, work, jx, result, report);
    File base = LuaPaths.base(context);
    File f = new File(base, REPORT_NAME);
    try (FileOutputStream out = new FileOutputStream(f)) {
      out.write(report.toString().getBytes(StandardCharsets.UTF_8));
      result.notes.add("The full list of what was restored and what was left out: " + f.getPath());
    } catch (Exception e) {
      Log.w(TAG, "Could not write the report", e);
      com.google.android.accessibility.talkback.MsErrorLog.log("Restore: " + "Could not write the report", e);
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> readSettings(File dat, JieshuoRestore.Result result) {
    if (!dat.isFile()) {
      return null;
    }
    try (ObjectInputStream in =
        new JieshuoRestore.SafeObjectInputStream(new FileInputStream(dat))) {
      Object o = in.readObject();
      if (o instanceof Map) {
        return new LinkedHashMap<String, Object>((Map<String, Object>) o);
      }
    } catch (Exception e) {
      Log.w(TAG, "Could not read the settings file", e);
      com.google.android.accessibility.talkback.MsErrorLog.log("Restore: " + "Could not read the settings file", e);
      result.notes.add("Could not read the settings file; settings and gestures were skipped.");
    }
    return null;
  }

  private static void applySettings(
      final Context context, Map<String, Object> jx, JieshuoRestore.Result result, StringBuilder report) {
    final PackageManager pm = context.getPackageManager();
    JxSettingsMapper.Plan plan =
        JxSettingsMapper.plan(
            jx,
            new JxSettingsMapper.Installed() {
              @Override
              public boolean has(String packageName) {
                try {
                  pm.getApplicationInfo(packageName, 0);
                  return true;
                } catch (PackageManager.NameNotFoundException | RuntimeException e) {
                  return false;
                }
              }
            },
            new JxSettingsMapper.Commands() {
              @Override
              public String idOf(String name) {
                return JxGestureApply.commandId(name);
              }
            });
    SharedPreferences.Editor ed = SharedPreferencesUtils.getSharedPreferences(context).edit();
    for (Map.Entry<String, Object> e : plan.apply.entrySet()) {
      Object v = e.getValue();
      if (v instanceof Boolean) {
        ed.putBoolean(e.getKey(), (Boolean) v);
      } else if (v instanceof Set) {
        java.util.Set<String> copy = new java.util.HashSet<>();
        for (Object o : (Set<?>) v) {
          copy.add(String.valueOf(o));
        }
        ed.putStringSet(e.getKey(), copy);
      } else {
        ed.putString(e.getKey(), String.valueOf(v));
      }
    }
    ed.apply();
    result.settingsApplied = plan.apply.size();
    result.settingsRejected = plan.rejected.size();
    result.settingsNoMatch = plan.noMatch.size();
    result.settingsPrivate = plan.privateSkipped;
    report.append("SETTINGS: ").append(plan.apply.size()).append(" restored, ")
        .append(plan.rejected.size()).append(" not restored (value does not fit), ")
        .append(plan.noMatch.size()).append(" have no setting in MS, ")
        .append(plan.privateSkipped).append(" private (keys, tokens, accounts) never read.\n\n");
    report.append("Restored (Jieshuo -> MS):\n");
    for (Map.Entry<String, String> e : plan.from.entrySet()) {
      report.append("  ").append(e.getKey()).append(" -> ").append(e.getValue()).append('\n');
    }
    report.append("\nNot restored, value does not fit:\n");
    for (Map.Entry<String, String> e : plan.rejected.entrySet()) {
      report.append("  ").append(e.getKey()).append(": ").append(e.getValue()).append('\n');
    }
    report.append("\nHandled by the gesture restore or without an equal in MS:\n");
    for (String k : plan.elsewhere) {
      report.append("  ").append(k).append('\n');
    }
    report.append("\nNo setting in MS for these Jieshuo settings:\n");
    for (String k : plan.noMatch) {
      report.append("  ").append(k).append('\n');
    }
    for (Map.Entry<String, String> e : plan.rejected.entrySet()) {
      if (result.settingsRejectedShown.size() < 8) {
        result.settingsRejectedShown.add(e.getKey() + ": " + e.getValue());
      }
    }
  }

  private static void applyGestures(
      Context context,
      File work,
      Map<String, Object> jx,
      JieshuoRestore.Result result,
      StringBuilder report) {
    File gestures = new File(work, "Gestures");
    if (!gestures.isDirectory()) {
      return;
    }
    Object gp = jx.get("gesture_package");
    String globalName = gp instanceof String ? ((String) gp).trim() : "";
    Map<String, String> global = new LinkedHashMap<>();
    Map<String, Map<String, String>> perApp = new LinkedHashMap<>();
    File[] schemes = gestures.listFiles();
    if (schemes != null) {
      java.util.Arrays.sort(schemes);
      for (File dir : schemes) {
        File cfg = new File(dir, "config");
        if (!dir.isDirectory() || !cfg.isFile()) {
          continue;
        }
        Map<String, String> m = readConfig(cfg);
        if (dir.getName().trim().equalsIgnoreCase(globalName)) {
          global.putAll(m);
        } else {
          perApp.put(dir.getName().trim(), m);
        }
      }
    }
    final Map<String, List<String>> byName = installedAppsByName(context);
    JxGestureApply.Summary s =
        JxGestureApply.restore(
            context,
            global,
            globalName,
            perApp,
            jx,
            new JxGestureApply.Apps() {
              @Override
              public String packageOf(String label) {
                List<String> l = byName.get(label == null ? "" : label.trim().toLowerCase(Locale.ROOT));
                return l == null || l.isEmpty() ? null : l.get(0);
              }
            });
    result.gesturesGlobal = s.global;
    result.gesturesPerApp = s.perApp;
    result.gesturesMultiPart = s.multiPart;
    result.gesturesSkipped = s.skipped.size();
    report.append("\nGESTURES: ").append(s.global).append(" in the global scheme \"")
        .append(globalName).append("\", ").append(s.perApp).append(" in app schemes, ")
        .append(s.multiPart).append(" multi-part, ").append(s.launchers)
        .append(" small \"Open app\" extensions made, ").append(s.skipped.size()).append(" left out.\n");
    report.append("Left out:\n");
    for (String k : s.skipped) {
      report.append("  ").append(k).append('\n');
      if (result.gesturesSkippedShown.size() < 8) {
        result.gesturesSkippedShown.add(k);
      }
    }
  }

  private static Map<String, String> readConfig(File f) {
    Map<String, String> out = new LinkedHashMap<>();
    try (FileInputStream in = new FileInputStream(f)) {
      byte[] b = new byte[(int) Math.min(f.length(), 1_000_000L)];
      int n = 0;
      while (n < b.length) {
        int r = in.read(b, n, b.length - n);
        if (r < 0) {
          break;
        }
        n += r;
      }
      JSONObject o = new JSONObject(new String(b, 0, n, StandardCharsets.UTF_8));
      java.util.Iterator<String> it = o.keys();
      while (it.hasNext()) {
        String k = it.next();
        out.put(k, o.optString(k, ""));
      }
    } catch (Exception e) {
      Log.w(TAG, "Could not read a gesture config", e);
      com.google.android.accessibility.talkback.MsErrorLog.log("Restore: " + "Could not read a gesture config", e);
    }
    return out;
  }

  private static Map<String, List<String>> installedAppsByName(Context context) {
    Map<String, List<String>> map = new HashMap<>();
    PackageManager pm = context.getPackageManager();
    try {
      for (ApplicationInfo info : pm.getInstalledApplications(0)) {
        CharSequence label = pm.getApplicationLabel(info);
        if (label == null) {
          continue;
        }
        String key = label.toString().trim().toLowerCase(Locale.ROOT);
        List<String> list = map.get(key);
        if (list == null) {
          list = new ArrayList<>();
          map.put(key, list);
        }
        list.add(info.packageName);
      }
    } catch (Exception e) {
      Log.w(TAG, "Could not list installed apps", e);
      com.google.android.accessibility.talkback.MsErrorLog.log("Restore: " + "Could not list installed apps", e);
    }
    return map;
  }
}
