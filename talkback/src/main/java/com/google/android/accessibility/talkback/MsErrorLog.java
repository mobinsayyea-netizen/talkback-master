package com.google.android.accessibility.talkback;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.Toast;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * One place for every problem the app finds (Lua errors, commands that did not run, restore
 * failures, crashes, process deaths) and the recent setting changes. Each error is saved, shown as
 * a short message on the screen and copied to the clipboard. Open it from Advanced settings >
 * Backup and restore > Error log. Never throws.
 */
public final class MsErrorLog {

  private static final String ERR_FILE = "ms_error_log.txt";
  private static final String SET_FILE = "ms_setting_changes.txt";
  private static final int MAX_ERR = 200_000;
  private static final int MAX_SET = 30_000;
  private static final int MAX_ENTRY = 3000;
  private static final Object LOCK = new Object();
  private static final Handler MAIN = new Handler(Looper.getMainLooper());

  private static Context app;
  private static SharedPreferences.OnSharedPreferenceChangeListener setListener;
  private static String lastKey = "";
  private static long lastKeyTime;
  private static long lastToast;

  private MsErrorLog() {}

  public static void init(Context c) {
    try {
      if (c != null && app == null) {
        Context a = c.getApplicationContext();
        app = a != null ? a : c;
      }
    } catch (Throwable ignored) {
    }
  }

  /** Call once when the service connects: process-death reasons + watch every setting change. */
  public static void onServiceStart(Context c) {
    init(c);
    try {
      if (Build.VERSION.SDK_INT >= 30) {
        logExitReasons(c);
      }
    } catch (Throwable t) {
      log("MsErrorLog exit reasons", t);
    }
    try {
      if (setListener == null) {
        final SharedPreferences sp = SharedPreferencesUtils.getSharedPreferences(c);
        setListener =
            (p, key) -> {
              try {
                if (key == null) {
                  return;
                }
                String k = key.toLowerCase(Locale.US);
                String v;
                if (k.contains("key") || k.contains("token") || k.contains("secret")
                    || k.contains("pass")) {
                  v = "(hidden)";
                } else {
                  v = String.valueOf(p.getAll().get(key));
                  if (v.length() > 80) {
                    v = v.substring(0, 80) + "...";
                  }
                }
                append(SET_FILE, MAX_SET, stamp() + "  " + key + " = " + v + "\n");
              } catch (Throwable ignored) {
              }
            };
        sp.registerOnSharedPreferenceChangeListener(setListener);
      }
    } catch (Throwable t) {
      log("MsErrorLog settings watcher", t);
    }
  }

  public static void log(String where, Object err) {
    try {
      String msg;
      if (err instanceof Throwable) {
        StringWriter sw = new StringWriter();
        ((Throwable) err).printStackTrace(new PrintWriter(sw));
        msg = sw.toString();
      } else {
        msg = String.valueOf(err);
      }
      if (msg.length() > MAX_ENTRY) {
        msg = msg.substring(0, MAX_ENTRY) + "\n...(cut)";
      }
      String key = where + "|" + (msg.length() > 120 ? msg.substring(0, 120) : msg);
      long now = System.currentTimeMillis();
      synchronized (LOCK) {
        if (key.equals(lastKey) && now - lastKeyTime < 2000) {
          return;
        }
        lastKey = key;
        lastKeyTime = now;
      }
      String entry = "=== " + stamp() + " | " + where + "\n" + msg + "\n\n";
      append(ERR_FILE, MAX_ERR, entry);
      showAndCopy(where, err instanceof Throwable ? String.valueOf(err) : msg, entry);
    } catch (Throwable ignored) {
    }
  }

  /** Crash path: only writes (the process is dying). */
  public static void logCrash(Thread thread, Throwable error) {
    try {
      StringWriter sw = new StringWriter();
      error.printStackTrace(new PrintWriter(sw));
      String s = sw.toString();
      if (s.length() > MAX_ENTRY) {
        s = s.substring(0, MAX_ENTRY) + "\n...(cut)";
      }
      append(
          ERR_FILE,
          MAX_ERR,
          "=== " + stamp() + " | CRASH in thread " + (thread == null ? "?" : thread.getName())
              + "\n" + s + "\n\n");
    } catch (Throwable ignored) {
    }
  }

  private static void showAndCopy(final String where, String shortMsg, final String entry) {
    final Context c = app;
    if (c == null) {
      return;
    }
    String first = shortMsg == null ? "" : shortMsg.split("\n")[0];
    if (first.length() > 160) {
      first = first.substring(0, 160);
    }
    final String text = "Problem in " + where + ": " + first + ". Saved in Error log and copied.";
    MAIN.post(
        () -> {
          try {
            ClipboardManager cm = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
              cm.setPrimaryClip(ClipData.newPlainText("MS Screen Reader error", entry));
            }
            long now = System.currentTimeMillis();
            if (now - lastToast > 3000) {
              lastToast = now;
              Toast.makeText(c, text, Toast.LENGTH_LONG).show();
            }
          } catch (Throwable ignored) {
          }
        });
  }

  private static void logExitReasons(Context c) {
    ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
    if (am == null) {
      return;
    }
    List<ApplicationExitInfo> list = am.getHistoricalProcessExitReasons(c.getPackageName(), 0, 8);
    SharedPreferences p = c.getSharedPreferences("ms_errlog", Context.MODE_PRIVATE);
    long last = p.getLong("last_exit_ts", 0);
    long max = last;
    for (ApplicationExitInfo i : list) {
      long ts = i.getTimestamp();
      if (ts <= last) {
        continue;
      }
      max = Math.max(max, ts);
      int r = i.getReason();
      if (r == ApplicationExitInfo.REASON_EXIT_SELF
          || r == ApplicationExitInfo.REASON_USER_REQUESTED
          || r == ApplicationExitInfo.REASON_USER_STOPPED
          || r == ApplicationExitInfo.REASON_PERMISSION_CHANGE) {
        continue;
      }
      StringBuilder sb = new StringBuilder();
      sb.append(reasonName(r))
          .append(" at ")
          .append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date(ts)))
          .append("\ndescription: ")
          .append(i.getDescription())
          .append("\nstatus: ")
          .append(i.getStatus())
          .append(", importance: ")
          .append(i.getImportance());
      if (r == ApplicationExitInfo.REASON_CRASH_NATIVE || r == ApplicationExitInfo.REASON_ANR) {
        try (InputStream in = i.getTraceInputStream()) {
          if (in != null) {
            byte[] b = new byte[8000];
            int n = in.read(b);
            StringBuilder t = new StringBuilder();
            for (int k = 0; k < Math.max(n, 0); k++) {
              char ch = (char) (b[k] & 0xff);
              if (ch == '\n' || (ch >= 32 && ch < 127)) {
                t.append(ch);
              }
            }
            String ts2 = t.toString();
            if (ts2.length() > 2500) {
              ts2 = ts2.substring(0, 2500);
            }
            sb.append("\ntrace:\n").append(ts2);
          }
        } catch (Throwable ignored) {
        }
      }
      log("Process ended (" + reasonName(r) + ")", sb.toString());
    }
    p.edit().putLong("last_exit_ts", max).apply();
  }

  private static String reasonName(int r) {
    switch (r) {
      case ApplicationExitInfo.REASON_CRASH:
        return "CRASH (Java)";
      case ApplicationExitInfo.REASON_CRASH_NATIVE:
        return "CRASH_NATIVE (C code, for example Lua engine)";
      case ApplicationExitInfo.REASON_ANR:
        return "ANR (app not responding)";
      case ApplicationExitInfo.REASON_LOW_MEMORY:
        return "LOW_MEMORY";
      case ApplicationExitInfo.REASON_SIGNALED:
        return "SIGNALED (killed)";
      case ApplicationExitInfo.REASON_INITIALIZATION_FAILURE:
        return "INITIALIZATION_FAILURE";
      case ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE:
        return "EXCESSIVE_RESOURCE_USAGE";
      case ApplicationExitInfo.REASON_DEPENDENCY_DIED:
        return "DEPENDENCY_DIED";
      default:
        return "REASON_" + r;
    }
  }

  // ---------------------------------------------------------------- files

  private static String stamp() {
    return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
  }

  private static File file(String name) {
    Context c = app;
    return c == null ? null : new File(c.getFilesDir(), name);
  }

  private static void append(String name, int max, String text) {
    synchronized (LOCK) {
      try {
        File f = file(name);
        if (f == null) {
          return;
        }
        String old = f.exists() ? read(f) : "";
        String all = old + text;
        if (all.length() > max) {
          all = all.substring(all.length() - (max * 3 / 4));
        }
        try (FileOutputStream out = new FileOutputStream(f)) {
          out.write(all.getBytes(StandardCharsets.UTF_8));
        }
      } catch (Throwable ignored) {
      }
    }
  }

  private static String read(File f) {
    try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
      byte[] b = new byte[(int) f.length()];
      int n = in.read(b);
      return new String(b, 0, Math.max(n, 0), StandardCharsets.UTF_8);
    } catch (Throwable t) {
      return "";
    }
  }

  /** Whole report: device line, errors, and the last setting changes. */
  public static String readAll(Context c) {
    init(c);
    StringBuilder sb = new StringBuilder();
    sb.append("MS Screen Reader report, ").append(stamp()).append('\n');
    sb.append("Android ").append(Build.VERSION.RELEASE).append(" (SDK ").append(Build.VERSION.SDK_INT)
        .append("), ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n');
    try {
      sb.append("App version: ")
          .append(c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName)
          .append('\n');
    } catch (Throwable ignored) {
    }
    File e = file(ERR_FILE);
    String errs = e != null && e.exists() ? read(e) : "";
    sb.append("\n## Errors\n").append(errs.isEmpty() ? "(none)\n" : errs);
    File s = file(SET_FILE);
    String sets = s != null && s.exists() ? read(s) : "";
    sb.append("\n## Recent setting changes\n").append(sets.isEmpty() ? "(none)\n" : sets);
    return sb.toString();
  }

  public static void clear(Context c) {
    init(c);
    synchronized (LOCK) {
      File e = file(ERR_FILE);
      File s = file(SET_FILE);
      if (e != null) {
        e.delete();
      }
      if (s != null) {
        s.delete();
      }
    }
  }

  /** Writes the report as a new text file in Downloads. Returns a message for the user. */
  public static String saveToDownloads(Context c) {
    try {
      String name =
          "MS Screen Reader error log "
              + new SimpleDateFormat("yyyy-MM-dd HH-mm-ss", Locale.US).format(new Date())
              + ".txt";
      byte[] bytes = readAll(c).getBytes(StandardCharsets.UTF_8);
      if (Build.VERSION.SDK_INT >= 29) {
        ContentValues v = new ContentValues();
        v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        v.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
        v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
        Uri u = c.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
        if (u == null) {
          return "Could not save to Downloads.";
        }
        try (OutputStream o = c.getContentResolver().openOutputStream(u)) {
          o.write(bytes);
        }
      } else {
        File d = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        d.mkdirs();
        try (FileOutputStream o = new FileOutputStream(new File(d, name))) {
          o.write(bytes);
        }
      }
      return "Saved in Downloads: " + name;
    } catch (Throwable t) {
      return "Could not save to Downloads: " + t;
    }
  }
}
