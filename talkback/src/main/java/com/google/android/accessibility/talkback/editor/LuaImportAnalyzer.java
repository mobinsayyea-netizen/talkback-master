/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "Analyze java package for imports": finds well-known Android/Java class names used in the code
 * (Toast, Intent, View ...) and adds the missing {@code import "package.Class"} lines at the top.
 * Pure Java. Only the classes listed below are known; others must be imported by hand.
 */
public final class LuaImportAnalyzer {
  private LuaImportAnalyzer() {}

  private static final String[][] KNOWN = {
    {"android.widget", "Toast TextView EditText Button ImageView LinearLayout FrameLayout RelativeLayout ScrollView ListView ArrayAdapter CheckBox Switch RadioButton RadioGroup Spinner ProgressBar SeekBar ImageButton GridView HorizontalScrollView TableLayout TableRow ToggleButton ArrayAdapter BaseAdapter SimpleAdapter AdapterView"},
    {"android.app", "AlertDialog Activity Dialog ProgressDialog Notification NotificationManager PendingIntent AlarmManager DatePickerDialog TimePickerDialog"},
    {"android.content", "Intent Context ClipData ClipboardManager SharedPreferences ComponentName BroadcastReceiver IntentFilter ContentValues ContentResolver DialogInterface"},
    {"android.content.pm", "PackageManager PackageInfo ApplicationInfo ResolveInfo"},
    {"android.view", "View ViewGroup Gravity LayoutInflater MotionEvent KeyEvent Window WindowManager Menu MenuItem"},
    {"android.view.accessibility", "AccessibilityNodeInfo AccessibilityEvent AccessibilityManager"},
    {"android.os", "Handler Looper Bundle Build Environment Vibrator VibrationEffect PowerManager BatteryManager SystemClock Message"},
    {"android.net", "Uri ConnectivityManager NetworkInfo"},
    {"android.net.wifi", "WifiManager WifiInfo"},
    {"android.media", "AudioManager MediaPlayer SoundPool MediaRecorder RingtoneManager ToneGenerator"},
    {"android.graphics", "Bitmap Canvas Color Paint Rect Point Matrix BitmapFactory Typeface"},
    {"android.graphics.drawable", "ColorDrawable Drawable BitmapDrawable GradientDrawable"},
    {"android.text", "TextUtils Editable TextWatcher Html"},
    {"android.util", "Log TypedValue DisplayMetrics"},
    {"android.provider", "Settings ContactsContract MediaStore"},
    {"android.telephony", "TelephonyManager SmsManager"},
    {"android.location", "LocationManager Location"},
    {"java.io", "File FileInputStream FileOutputStream InputStream OutputStream BufferedReader InputStreamReader FileReader FileWriter"},
    {"java.net", "URL HttpURLConnection URLEncoder URLDecoder"},
    {"java.util", "ArrayList HashMap HashSet List Map Set Date Calendar Locale Arrays Collections Random Timer TimerTask Iterator LinkedList"},
    {"java.text", "SimpleDateFormat DecimalFormat"},
    {"java.lang", "Thread Runnable StringBuilder System Math Integer Long Double Boolean String Object"},
    {"java.lang.reflect", "Method Field Modifier"}
  };

  private static final Pattern IMPORT_LINE = Pattern.compile("^\\s*import\\s+[\"'](.+?)[\"']", Pattern.MULTILINE);
  private static final Pattern WORD = Pattern.compile("(?<![A-Za-z0-9_.])([A-Z][A-Za-z0-9_]*)\\s*[.(]");

  /** Needed import lines (full text of each line) that the code does not have yet. */
  public static List<String> missing(String code) {
    Set<String> have = new LinkedHashSet<>();
    Matcher im = IMPORT_LINE.matcher(code);
    while (im.find()) {
      have.add(im.group(1));
    }
    String body = LuaFormatterHelper.strip(code);
    Set<String> used = new LinkedHashSet<>();
    Matcher m = WORD.matcher(body);
    while (m.find()) {
      used.add(m.group(1));
    }
    List<String> result = new ArrayList<>();
    for (String cls : used) {
      String pkg = packageOf(cls);
      if (pkg == null || pkg.equals("java.lang")) {
        continue; // java.lang names are available without import in Lua code of this kind
      }
      String full = pkg + "." + cls;
      boolean covered = have.contains(full) || have.contains(pkg + ".*") || endsWith(have, "." + cls);
      if (!covered) {
        result.add("import \"" + full + "\"");
      }
    }
    return result;
  }

  private static boolean endsWith(Set<String> have, String suffix) {
    for (String h : have) {
      if (h.endsWith(suffix)) {
        return true;
      }
    }
    return false;
  }

  static String packageOf(String cls) {
    for (String[] row : KNOWN) {
      for (String c : row[1].split(" ")) {
        if (c.equals(cls)) {
          return row[0];
        }
      }
    }
    return null;
  }

  /** Puts the lines after the leading "--" comment block (or at the very top). */
  public static String insert(String code, List<String> lines) {
    if (lines.isEmpty()) {
      return code;
    }
    String[] src = code.split("\n", -1);
    int at = 0;
    while (at < src.length && src[at].trim().startsWith("--")) {
      at++;
    }
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < at; i++) {
      b.append(src[i]).append('\n');
    }
    for (String l : lines) {
      b.append(l).append('\n');
    }
    for (int i = at; i < src.length; i++) {
      b.append(src[i]);
      if (i < src.length - 1) {
        b.append('\n');
      }
    }
    return b.toString();
  }
}
