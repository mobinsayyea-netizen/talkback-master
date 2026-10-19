/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-application options in the actions menu ("App options" group): gesture scheme of the app,
 * notification read / whitelist / blacklist, own speech rate, own volume, copy package name. The
 * settings of an app are stored as text lines "package TAB key TAB value" in the preference
 * "ms_app_profiles". Speech rate and volume are applied when that app comes to the front and put
 * back when another app without its own value comes. Not compiled yet.
 */
public final class MsAppProfiles {
  private MsAppProfiles() {}

  static final String KEY = "ms_app_profiles";
  private static final String SAVED_RATE = "ms_app_prof_saved_rate";
  private static final String SAVED_VOL = "ms_app_prof_saved_vol";
  private static final long AFTER_CLOSE_MS = 600;
  private static final Handler MAIN = new Handler(Looper.getMainLooper());
  private static final int[] RATES = {50, 75, 100, 125, 150, 200, 250, 300};
  private static final int[] VOLS = {20, 40, 60, 80, 100};

  private static String lastPkg = "";

  public static final int[] IDS = {
    R.id.ms_app_scheme,
    R.id.ms_app_notif_read,
    R.id.ms_app_notif_white,
    R.id.ms_app_notif_black,
    R.id.ms_app_rate,
    R.id.ms_app_volume,
    R.id.ms_app_copy_pkg,
  };

  public static boolean isAppId(int itemId) {
    for (int id : IDS) {
      if (id == itemId) {
        return true;
      }
    }
    return false;
  }

  // ---- storage

  private static SharedPreferences prefs(Context c) {
    return SharedPreferencesUtils.getSharedPreferences(c);
  }

  private static Map<String, Map<String, String>> load(Context c) {
    Map<String, Map<String, String>> out = new LinkedHashMap<>();
    String text = prefs(c).getString(KEY, "");
    for (String line : text.split("\n")) {
      String[] p = line.split("\t", -1);
      if (p.length == 3 && !p[0].isEmpty()) {
        Map<String, String> m = out.get(p[0]);
        if (m == null) {
          m = new LinkedHashMap<>();
          out.put(p[0], m);
        }
        m.put(p[1], p[2]);
      }
    }
    return out;
  }

  private static void save(Context c, Map<String, Map<String, String>> all) {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, Map<String, String>> e : all.entrySet()) {
      for (Map.Entry<String, String> kv : e.getValue().entrySet()) {
        sb.append(e.getKey()).append('\t').append(kv.getKey()).append('\t')
            .append(kv.getValue().replace('\t', ' ').replace('\n', ' ')).append('\n');
      }
    }
    prefs(c).edit().putString(KEY, sb.toString()).apply();
  }

  static String get(Context c, String pkg, String key) {
    Map<String, String> m = load(c).get(pkg);
    return m == null ? null : m.get(key);
  }

  /** Sets a value; null removes it. */
  static void put(Context c, String pkg, String key, String value) {
    Map<String, Map<String, String>> all = load(c);
    Map<String, String> m = all.get(pkg);
    if (m == null) {
      if (value == null) {
        return;
      }
      m = new LinkedHashMap<>();
      all.put(pkg, m);
    }
    if (value == null) {
      m.remove(key);
      if (m.isEmpty()) {
        all.remove(pkg);
      }
    } else {
      m.put(key, value);
    }
    save(c, all);
  }

  // ---- notifications (called from MsNotificationController)

  /** False when notifications of this app must not be spoken. */
  public static boolean notificationAllowed(Context c, CharSequence pkgCs) {
    if (pkgCs == null) {
      return true;
    }
    String pkg = pkgCs.toString();
    Map<String, Map<String, String>> all = load(c);
    Map<String, String> m = all.get(pkg);
    if (m != null) {
      if ("1".equals(m.get("black")) || "off".equals(m.get("notif"))) {
        return false;
      }
      if ("on".equals(m.get("notif"))) {
        return true;
      }
    }
    boolean whiteExists = false;
    for (Map<String, String> v : all.values()) {
      if ("1".equals(v.get("white"))) {
        whiteExists = true;
        break;
      }
    }
    return !whiteExists || (m != null && "1".equals(m.get("white")));
  }

  // ---- apply rate / volume when the app in front changes (called from TalkBackService)

  public static void onEvent(TalkBackService s, AccessibilityEvent event) {
    if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        || event.getPackageName() == null) {
      return;
    }
    String pkg = event.getPackageName().toString();
    if (pkg.equals(lastPkg) || pkg.equals(s.getPackageName()) || pkg.equals("com.android.systemui")
        || pkg.contains("inputmethod") || pkg.contains("keyboard")) {
      return;
    }
    lastPkg = pkg;
    String rate = get(s, pkg, "rate");
    String vol = get(s, pkg, "vol");
    SharedPreferences p = prefs(s);
    if (rate != null) {
      if (!p.contains(SAVED_RATE)) {
        p.edit()
            .putString(SAVED_RATE, p.getString(s.getString(R.string.pref_speech_rate_key), "1.0"))
            .apply();
      }
      setRate(s, Integer.parseInt(rate));
    } else if (p.contains(SAVED_RATE)) {
      float f;
      try {
        f = Float.parseFloat(p.getString(SAVED_RATE, "1.0"));
      } catch (NumberFormatException e) {
        f = 1f;
      }
      setRate(s, Math.round(f * 100));
      p.edit().remove(SAVED_RATE).apply();
    }
    AudioManager am = (AudioManager) s.getSystemService(Context.AUDIO_SERVICE);
    if (am == null) {
      return;
    }
    if (vol != null) {
      if (!p.contains(SAVED_VOL)) {
        p.edit().putInt(SAVED_VOL, am.getStreamVolume(AudioManager.STREAM_ACCESSIBILITY)).apply();
      }
      setVolume(am, Integer.parseInt(vol));
    } else if (p.contains(SAVED_VOL)) {
      am.setStreamVolume(AudioManager.STREAM_ACCESSIBILITY, p.getInt(SAVED_VOL, 1), 0);
      p.edit().remove(SAVED_VOL).apply();
    }
  }

  private static void setRate(Context c, int percent) {
    prefs(c)
        .edit()
        .putString(c.getString(R.string.pref_speech_rate_key), Float.toString(percent / 100f))
        .putInt(c.getString(R.string.pref_speech_rate_seekbar_key_int), percent)
        .apply();
  }

  private static void setVolume(AudioManager am, int percent) {
    int max = Math.max(am.getStreamMaxVolume(AudioManager.STREAM_ACCESSIBILITY), 1);
    am.setStreamVolume(
        AudioManager.STREAM_ACCESSIBILITY, Math.max(1, Math.round(max * percent / 100f)), 0);
  }

  // ---- menu click

  /** Called when one of the "App options" entries is clicked. */
  public static void run(final TalkBackService s, final int itemId) {
    MAIN.postDelayed(() -> runNow(s, itemId), AFTER_CLOSE_MS);
  }

  private static void runNow(TalkBackService s, int itemId) {
    AccessibilityNodeInfo focus = s.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
    String pkg = targetPackage(s, focus);
    if (pkg == null) {
      LuaServiceApi.speakStatic("No application found for this item");
      return;
    }
    String name = appLabel(s, pkg);
    if (itemId == R.id.ms_app_scheme) {
      AppGestureSchemes.create(s, pkg);
      ExtensionGesturesActivity.showForApp(s, pkg);
    } else if (itemId == R.id.ms_app_notif_read) {
      boolean allowed = notificationAllowed(s, pkg);
      put(s, pkg, "black", null);
      put(s, pkg, "notif", allowed ? "off" : "on");
      LuaServiceApi.speakStatic(name + (allowed ? ": notifications will not be read" : ": notifications will be read"));
    } else if (itemId == R.id.ms_app_notif_white) {
      boolean now = !"1".equals(get(s, pkg, "white"));
      put(s, pkg, "white", now ? "1" : null);
      if (now) {
        put(s, pkg, "black", null);
      }
      LuaServiceApi.speakStatic(name + (now ? " added to whitelist" : " removed from whitelist"));
    } else if (itemId == R.id.ms_app_notif_black) {
      boolean now = !"1".equals(get(s, pkg, "black"));
      put(s, pkg, "black", now ? "1" : null);
      if (now) {
        put(s, pkg, "white", null);
      }
      LuaServiceApi.speakStatic(name + (now ? " added to blacklist" : " removed from blacklist"));
    } else if (itemId == R.id.ms_app_rate) {
      String cur = get(s, pkg, "rate");
      int next = nextValue(RATES, cur);
      put(s, pkg, "rate", next < 0 ? null : String.valueOf(next));
      lastPkg = "";
      if (next >= 0) {
        setRate(s, next);
      }
      LuaServiceApi.speakStatic(
          next < 0 ? name + ": speech rate is the common one" : name + ": speech rate " + next + " percent");
    } else if (itemId == R.id.ms_app_volume) {
      String cur = get(s, pkg, "vol");
      int next = nextValue(VOLS, cur);
      put(s, pkg, "vol", next < 0 ? null : String.valueOf(next));
      lastPkg = "";
      AudioManager am = (AudioManager) s.getSystemService(Context.AUDIO_SERVICE);
      if (next >= 0 && am != null) {
        setVolume(am, next);
      }
      LuaServiceApi.speakStatic(
          next < 0 ? name + ": volume is the common one" : name + ": volume " + next + " percent");
    } else if (itemId == R.id.ms_app_copy_pkg) {
      ClipboardManager cm = (ClipboardManager) s.getSystemService(Context.CLIPBOARD_SERVICE);
      if (cm != null) {
        cm.setPrimaryClip(ClipData.newPlainText("package", pkg));
      }
      LuaServiceApi.speakStatic("Copied " + pkg);
    }
  }

  /** Next preset after the current one; -1 (back to the common value) after the last one. */
  private static int nextValue(int[] list, String cur) {
    if (cur == null) {
      return list[0];
    }
    int v;
    try {
      v = Integer.parseInt(cur);
    } catch (NumberFormatException e) {
      return list[0];
    }
    for (int i = 0; i < list.length; i++) {
      if (list[i] > v) {
        return list[i];
      }
    }
    return -1;
  }

  // ---- which app is the focused item

  /**
   * The package of the app the focused item stands for. In a launcher / app list the item belongs
   * to the launcher, so the item's text is matched with the names of installed apps.
   */
  static String targetPackage(AccessibilityService s, AccessibilityNodeInfo node) {
    if (node == null) {
      return AppGestureSchemes.foregroundPackage(s);
    }
    CharSequence own = node.getPackageName();
    String pkg = own == null ? null : own.toString();
    PackageManager pm = s.getPackageManager();
    boolean launcher = false;
    Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
    List<ResolveInfo> homes = pm.queryIntentActivities(home, 0);
    for (ResolveInfo r : homes) {
      if (r.activityInfo.packageName.equals(pkg)) {
        launcher = true;
      }
    }
    if (!launcher) {
      return pkg;
    }
    String label = labelOf(node);
    if (label == null) {
      return pkg;
    }
    Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
    for (ResolveInfo r : pm.queryIntentActivities(main, 0)) {
      String l = r.loadLabel(pm).toString();
      if (label.equalsIgnoreCase(l) || label.toLowerCase().startsWith(l.toLowerCase() + ",")) {
        return r.activityInfo.packageName;
      }
    }
    return pkg;
  }

  private static String labelOf(AccessibilityNodeInfo n) {
    CharSequence t = n.getContentDescription();
    if (t == null || t.length() == 0) {
      t = n.getText();
    }
    return t == null ? null : t.toString().trim();
  }

  private static String appLabel(Context c, String pkg) {
    try {
      PackageManager pm = c.getPackageManager();
      return pm.getApplicationInfo(pkg, 0).loadLabel(pm).toString();
    } catch (PackageManager.NameNotFoundException e) {
      return pkg;
    }
  }
}
