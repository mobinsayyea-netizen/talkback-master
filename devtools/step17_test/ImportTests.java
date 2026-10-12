import java.util.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Run2 { public static void main(String[] a){
 int f=0;
 String code="-- title: x\n-- menu: on\nToast.makeText(service, \"Intent here\", 1).show()\nlocal i = Intent(\"a\")\nTextView(service)\nlocal s = String(\"x\") -- Bitmap.create()\nimport \"android.widget.TextView\"\n";
 List<String> m=LuaImportAnalyzer.missing(code);
 if(!m.equals(Arrays.asList("import \"android.widget.Toast\"","import \"android.content.Intent\""))){f++;System.out.println("FAIL "+m);} 
 String out=LuaImportAnalyzer.insert(code,m);
 if(!out.startsWith("-- title: x\n-- menu: on\nimport \"android.widget.Toast\"\nimport \"android.content.Intent\"\nToast")){f++;System.out.println("FAIL insert\n"+out);} 
 if(!LuaImportAnalyzer.missing(out).isEmpty()){f++;System.out.println("FAIL second pass");}
 System.out.println(f==0?"ALL PASS":"FAILS "+f);
}}
/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */









/**
 * "Analyze java package for imports": finds well-known Android/Java class names used in the code
 * (Toast, Intent, View ...) and adds the missing {@code import "package.Class"} lines at the top.
 * Pure Java. Only the classes listed below are known; others must be imported by hand.
 */
final class LuaImportAnalyzer {
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

/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */





/** "Format source code" for the Lua editor: re-indents by block keywords. Pure Java. */
final class LuaFormatter {
  private LuaFormatter() {}

  private static final Pattern WORD = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

  public static String format(String code) {
    String[] lines = code.replace("\r\n", "\n").split("\n", -1);
    StringBuilder out = new StringBuilder();
    int level = 0;
    boolean inLongString = false;
    for (int i = 0; i < lines.length; i++) {
      String raw = lines[i];
      if (inLongString) {
        out.append(raw);
        if (raw.contains("]]")) {
          inLongString = false;
        }
      } else {
        String t = raw.trim();
        int[] d = delta(t);
        int before = Math.max(0, level - d[0]); // closers at the start dedent this line
        out.append(t.isEmpty() ? "" : indent(before) + t);
        level = Math.max(0, level + d[1]);
        if (t.contains("[[") && !t.contains("]]")) {
          inLongString = true;
        }
      }
      if (i < lines.length - 1) {
        out.append('\n');
      }
    }
    return out.toString();
  }

  private static String indent(int n) {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < n; i++) {
      b.append("  ");
    }
    return b.toString();
  }

  /** Returns {leading dedent for this line, net change for the next lines}. */
  static int[] delta(String line) {
    String code = stripStringsAndComments(line);
    int net = 0;
    int leading = 0;
    int brNet = 0; // ( and { : one indent level per line at most, so "f({" is not double indented
    int brLeading = 0;
    boolean atStart = true;
    Matcher m = WORD.matcher(code);
    int pos = 0;
    int loopDo = 0; // "do" that belongs to while/for does not open a second block
    while (pos < code.length()) {
      char c = code.charAt(pos);
      if (c == '{' || c == '(') {
        brNet++;
        atStart = false;
        pos++;
      } else if (c == '}' || c == ')') {
        brNet--;
        if (atStart) {
          brLeading++;
        }
        pos++;
      } else if (Character.isLetter(c) || c == '_') {
        m.find(pos);
        String w = m.group();
        pos = m.end();
        switch (w) {
          case "function":
          case "if":
          case "repeat":
            net++;
            break;
          case "while":
          case "for":
            net++;
            loopDo++;
            break;
          case "do":
            if (loopDo > 0) {
              loopDo--;
            } else {
              net++;
            }
            break;
          case "end":
          case "until":
            net--;
            if (atStart) {
              leading++;
            }
            break;
          case "else":
          case "elseif":
            if (atStart) {
              leading++; // closes the previous branch; the new branch opens again (net 0)
            }
            break;
          case "then":
            break;
          default:
            break;
        }
        if (!w.equals("else") && !w.equals("elseif")) {
          atStart = atStart && (w.equals("end") || w.equals("until"));
        } else {
          atStart = false;
        }
      } else if (Character.isWhitespace(c)) {
        pos++;
      } else {
        atStart = false;
        pos++;
      }
    }
    brNet = Math.max(-1, Math.min(1, brNet));
    brLeading = Math.min(1, brLeading);
    return new int[] {leading + brLeading, net + brNet};
  }

  /** Removes "..." and '...' contents and -- comments so keywords inside them are ignored. */
  static String stripStringsAndComments(String line) {
    StringBuilder b = new StringBuilder();
    char quote = 0;
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (quote != 0) {
        if (c == '\\') {
          i++;
        } else if (c == quote) {
          quote = 0;
        }
        continue;
      }
      if (c == '"' || c == '\'') {
        quote = c;
        continue;
      }
      if (c == '-' && i + 1 < line.length() && line.charAt(i + 1) == '-') {
        break;
      }
      b.append(c);
    }
    return b.toString();
  }
}

/** Shares the string/comment stripper with other code helpers. */
final class LuaFormatterHelper {
  private LuaFormatterHelper() {}

  static String strip(String code) {
    StringBuilder b = new StringBuilder();
    for (String line : code.split("\n", -1)) {
      b.append(LuaFormatter.stripStringsAndComments(line)).append('\n');
    }
    return b.toString();
  }
}
