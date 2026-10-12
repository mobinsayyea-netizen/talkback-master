import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
public class SchemeTests {
  static int fail = 0;
  static void check(boolean ok, String what) {
    if (!ok) { fail++; System.out.println("FAIL: " + what); }
  }
  public static void main(String[] a) {
    // round trip, empty scheme kept, off kept
    java.util.Map<String, AppGestureSchemeData.Scheme> m = new java.util.LinkedHashMap<>();
    AppGestureSchemeData.Scheme w = new AppGestureSchemeData.Scheme("com.whatsapp");
    w.gestures.put(9, new String[] {"ext", "Copy text"});
    w.gestures.put(5, new String[] {"tool", "Recorder"});
    AppGestureSchemeData.Scheme y = new AppGestureSchemeData.Scheme("com.google.android.youtube");
    y.enabled = false;
    y.gestures.put(9, new String[] {"ext", "Play pause"});
    AppGestureSchemeData.Scheme e = new AppGestureSchemeData.Scheme("com.empty.app");
    m.put(w.pkg, w); m.put(y.pkg, y); m.put(e.pkg, e);
    String text = AppGestureSchemeData.format(m);
    java.util.Map<String, AppGestureSchemeData.Scheme> r = AppGestureSchemeData.parse(text);
    check(r.size() == 3, "three schemes back");
    check(r.get("com.empty.app") != null && r.get("com.empty.app").gestures.isEmpty(), "empty scheme kept");
    check(!r.get("com.google.android.youtube").enabled, "off kept");
    check(r.get("com.whatsapp").enabled, "on kept");
    check(AppGestureSchemeData.format(r).equals(text), "format is stable");
    // lookup
    check(AppGestureSchemeData.lookup(r, "com.whatsapp", 9)[1].equals("Copy text"), "whatsapp swipe 9");
    check(AppGestureSchemeData.lookup(r, "com.whatsapp", 1) == null, "free gesture -> global");
    check(AppGestureSchemeData.lookup(r, "com.google.android.youtube", 9) == null, "off scheme not used");
    check(AppGestureSchemeData.lookup(r, "org.other", 9) == null, "app without scheme");
    check(AppGestureSchemeData.anyEnabled(r), "anyEnabled");
    java.util.Map<String, AppGestureSchemeData.Scheme> onlyEmpty = new java.util.LinkedHashMap<>();
    onlyEmpty.put("x.y", new AppGestureSchemeData.Scheme("x.y"));
    check(!AppGestureSchemeData.anyEnabled(onlyEmpty), "empty scheme alone: nothing to do");
    // rename and remove touch every scheme
    check(AppGestureSchemeData.rename(r, "ext", "Copy text", "Copy all"), "rename changed");
    check(AppGestureSchemeData.lookup(r, "com.whatsapp", 9)[1].equals("Copy all"), "renamed");
    check(!AppGestureSchemeData.rename(r, "tool", "Copy all", "zz"), "kind must match");
    check(AppGestureSchemeData.remove(r, "tool", "Recorder"), "remove changed");
    check(r.get("com.whatsapp").gestures.size() == 1, "tool gesture dropped");
    check(r.containsKey("com.empty.app"), "scheme stays after remove");
    check(!AppGestureSchemeData.remove(r, "tool", "Recorder"), "second remove no change");
    // bad input
    check(AppGestureSchemeData.parse(null).isEmpty(), "null");
    check(AppGestureSchemeData.parse("").isEmpty(), "empty text");
    java.util.Map<String, AppGestureSchemeData.Scheme> b =
        AppGestureSchemeData.parse("9\text\tOrphan\n@a.b\ton\nxx\text\tBad\n3\text\n4\text\tGood\n@\toff\n7\text\tLost\n");
    check(b.size() == 1 && b.get("a.b").gestures.size() == 1, "broken lines skipped, no-name scheme dropped");
    check(b.get("a.b").gestures.get(4)[1].equals("Good"), "good line kept");
    // tabs and newlines in names cannot break the format
    AppGestureSchemeData.Scheme t = new AppGestureSchemeData.Scheme("t.t");
    t.gestures.put(2, new String[] {"ext", "A\tB\nC"});
    java.util.Map<String, AppGestureSchemeData.Scheme> tm = new java.util.LinkedHashMap<>();
    tm.put("t.t", t);
    java.util.Map<String, AppGestureSchemeData.Scheme> tr = AppGestureSchemeData.parse(AppGestureSchemeData.format(tm));
    check(tr.get("t.t").gestures.get(2)[1].equals("A B C"), "tab/newline cleaned");
    System.out.println(fail == 0 ? "ALL PASS" : (fail + " FAILED"));
    if (fail != 0) System.exit(1);
  }
}

/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */


/**
 * Pure Java (no Android) text format of the per-application gesture schemes, so it can be tested on
 * a computer. One preference string:
 *
 * <pre>
 * \@com.whatsapp TAB on
 * 9 TAB ext TAB Copy text
 * 5 TAB tool TAB Recorder
 * \@com.google.android.youtube TAB off
 * </pre>
 *
 * A line starting with "@" starts a scheme (package name, then "on" or "off"). The lines below it
 * are "gestureId TAB kind TAB name" (kind is "ext" or "tool"). A scheme with no gesture lines is
 * still kept, so a scheme created but not filled in yet shows in the list.
 */
final class AppGestureSchemeData {
  private AppGestureSchemeData() {}

  /** One scheme: the app, whether it is turned on, and its gestures. */
  static final class Scheme {
    final String pkg;
    boolean enabled = true;
    final Map<Integer, String[]> gestures = new LinkedHashMap<>(); // id -> {kind, name}

    Scheme(String pkg) {
      this.pkg = pkg;
    }
  }

  static String clean(String s) {
    return s == null ? "" : s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim();
  }

  static Map<String, Scheme> parse(String text) {
    Map<String, Scheme> out = new LinkedHashMap<>();
    if (text == null) {
      return out;
    }
    Scheme cur = null;
    for (String line : text.split("\n")) {
      if (line.isEmpty()) {
        continue;
      }
      if (line.charAt(0) == '@') {
        String[] p = line.substring(1).split("\t", -1);
        String pkg = p[0].trim();
        if (pkg.isEmpty()) {
          cur = null;
          continue;
        }
        cur = new Scheme(pkg);
        cur.enabled = !(p.length > 1 && "off".equals(p[1].trim()));
        out.put(pkg, cur);
      } else if (cur != null) {
        String[] p = line.split("\t", -1);
        if (p.length == 3) {
          try {
            cur.gestures.put(Integer.parseInt(p[0].trim()), new String[] {p[1], p[2]});
          } catch (NumberFormatException ignored) {
            // skip a broken line
          }
        }
      }
    }
    return out;
  }

  static String format(Map<String, Scheme> all) {
    StringBuilder sb = new StringBuilder();
    for (Scheme s : all.values()) {
      sb.append('@').append(s.pkg).append('\t').append(s.enabled ? "on" : "off").append('\n');
      for (Map.Entry<Integer, String[]> e : s.gestures.entrySet()) {
        sb.append(e.getKey())
            .append('\t')
            .append(clean(e.getValue()[0]))
            .append('\t')
            .append(clean(e.getValue()[1]))
            .append('\n');
      }
    }
    return sb.toString();
  }

  /** The target on this gesture, or null when the app has no scheme, it is off, or the gesture is free. */
  static String[] lookup(Map<String, Scheme> all, String pkg, int gestureId) {
    Scheme s = all.get(pkg);
    if (s == null || !s.enabled) {
      return null;
    }
    return s.gestures.get(gestureId);
  }

  static boolean anyEnabled(Map<String, Scheme> all) {
    for (Scheme s : all.values()) {
      if (s.enabled && !s.gestures.isEmpty()) {
        return true;
      }
    }
    return false;
  }

  /** Renames an extension or tool in every scheme. Returns true when something changed. */
  static boolean rename(Map<String, Scheme> all, String kind, String oldName, String newName) {
    boolean changed = false;
    for (Scheme s : all.values()) {
      for (Map.Entry<Integer, String[]> e : s.gestures.entrySet()) {
        if (e.getValue()[0].equals(kind) && e.getValue()[1].equals(oldName)) {
          e.setValue(new String[] {kind, newName});
          changed = true;
        }
      }
    }
    return changed;
  }

  /** Removes an extension or tool from every scheme. The scheme itself stays. */
  static boolean remove(Map<String, Scheme> all, String kind, String name) {
    boolean changed = false;
    for (Scheme s : all.values()) {
      List<Integer> drop = new ArrayList<>();
      for (Map.Entry<Integer, String[]> e : s.gestures.entrySet()) {
        if (e.getValue()[0].equals(kind) && e.getValue()[1].equals(name)) {
          drop.add(e.getKey());
        }
      }
      for (Integer id : drop) {
        s.gestures.remove(id);
        changed = true;
      }
    }
    return changed;
  }
}


