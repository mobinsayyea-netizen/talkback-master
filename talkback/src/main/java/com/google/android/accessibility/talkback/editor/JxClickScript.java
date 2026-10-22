/*
 * Copyright 2026 Advanced Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Jieshuo "auto click" mini-language (service.click({...})). Pure Java, no Android classes, so it
 * can be tested on a computer. Rules come from JX_API_REFERENCE_zh.txt (items 59-107).
 *
 * <p>Structure: a list is in "alternatives" mode (first item that works wins). A list nested in an
 * alternatives list is a SEQUENCE (items are run one after another, stops at the first missing
 * item). A list nested in a sequence is an optional BRANCH (alternatives mode; failing is fine).
 */
final class JxClickScript {

  /** Everything the script needs from the phone. Android side: JxClickEnv. */
  interface Env {
    /** Visible nodes whose text/description matches the pattern ('*' = any characters). */
    List<Object> findAll(String pattern, boolean byLabel);

    boolean click(Object node);

    boolean locate(Object node);

    boolean rawTap(Object node, long ms);

    boolean screenContains(String pattern);

    /** Focus type check: editable, clickable, list, listitem, edit. Unknown type = false. */
    boolean focusIs(String type);

    boolean inApp(String name);

    boolean openApp(String name);

    /** Runs a built-in command (service.execute). */
    boolean command(String name);

    /** Calls a Lua function; null when no such function exists. */
    Boolean callFunction(String name, String args);

    boolean tap(double x, double y, long ms);

    boolean swipe(double x1, double y1, double x2, double y2, long ms);

    boolean tapFocus(long ms);

    void sleep(long ms) throws InterruptedException;

    long now();

    /**
     * Finds colour r,g,b (tolerance tol per channel) inside the rectangle and taps its centre.
     * Rectangle values below 1 are fractions of the screen. False = colour not found.
     */
    default boolean clickColor(
        double x1, double y1, double x2, double y2, int r, int g, int b, int tol) {
      return false;
    }
  }

  // Timing choices that Jieshuo's documentation does not state. Tune after testing on a phone.
  static final long SETTLE_MS = 350; // pause after a click before the next step
  static final long SEQUENCE_WAIT_MS = 1000; // a later step of a sequence may appear this late
  static final long BRANCH_WAIT_MS = 300; // optional branch steps
  static final long POLL_MS = 200;
  static final long MAX_RUN_MS = 5 * 60 * 1000L;
  static final int MAX_STEPS = 2000;

  private static final String SYMBOLS = "#&$<>@";
  private static final Pattern NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");
  private static final Pattern FUNC = Pattern.compile("^([^()\\s]+)\\s*\\((.*)\\)\\s*$", Pattern.DOTALL);

  private enum Kind { NAME, OPEN_APP, COMMAND, DIRECT, CHECK }

  private static final class Alt {
    Kind kind;
    String name = "";
    boolean byLabel;
    boolean locateOnly;
    Integer index;
    String onlyApp;
    List<String> conds = new ArrayList<>(); // "&x" or "&!x"
    long delayMs;
    int repeat = 1;
    long waitSec;
    String error;
  }

  private static final int OK = 0;
  private static final int NOT_FOUND = 1;
  private static final int FAILED = 2;

  private final Env env;
  private boolean halted;
  private int steps;
  private long startedAt;
  private String error;

  JxClickScript(Env env) {
    this.env = env;
  }

  String getError() {
    return error;
  }

  /** Runs the list. Returns true when a step chain finished. */
  boolean run(List<?> top) {
    halted = false;
    steps = 0;
    error = null;
    startedAt = env.now();
    try {
      return runAlternatives(top, 0);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      error = "interrupted";
      return false;
    }
  }

  // ------------------------------------------------------------------ structure

  private boolean runAlternatives(List<?> list, long waitMs) throws InterruptedException {
    for (Object e : list) {
      if (halted) {
        return false;
      }
      boolean ok;
      if (e instanceof List) {
        ok = runSequence((List<?>) e);
      } else {
        ok = runItem(String.valueOf(e), waitMs);
      }
      if (ok) {
        return true;
      }
    }
    return false;
  }

  private boolean runSequence(List<?> list) throws InterruptedException {
    int n = list.size();
    for (int i = 0; i < n; i++) {
      if (halted) {
        return false;
      }
      Object e = list.get(i);
      if (e instanceof List) {
        runAlternatives((List<?>) e, BRANCH_WAIT_MS); // optional branch, result ignored
        if (halted) {
          return false;
        }
        continue;
      }
      if (!runItem(String.valueOf(e), i > 0 ? SEQUENCE_WAIT_MS : 0)) {
        return false;
      }
    }
    return true;
  }

  // ------------------------------------------------------------------ one item

  private boolean runItem(String text, long waitMs) throws InterruptedException {
    List<Alt> alts = parseItem(text);
    if (alts.isEmpty()) {
      return true; // empty text between commas: nothing to do
    }
    long wait = waitMs;
    for (Alt a : alts) {
      if (a.error != null) {
        error = a.error;
        return false;
      }
      wait = Math.max(wait, a.waitSec * 1000L);
    }
    long end = env.now() + wait;
    while (true) {
      if (++steps > MAX_STEPS || env.now() - startedAt > MAX_RUN_MS) {
        error = "script too long";
        halted = true;
        return false;
      }
      for (Alt a : alts) {
        int r = tryAlt(a);
        if (halted) {
          return false;
        }
        if (r == OK) {
          if (a.delayMs > 0) {
            env.sleep(a.delayMs);
          } else {
            env.sleep(SETTLE_MS);
          }
          return true;
        }
        if (r == FAILED) {
          return false;
        }
      }
      if (env.now() >= end) {
        return false;
      }
      env.sleep(POLL_MS);
    }
  }

  private int tryAlt(Alt a) throws InterruptedException {
    switch (a.kind) {
      case CHECK:
        return env.focusIs(a.name) ? OK : NOT_FOUND;
      case OPEN_APP:
        return env.openApp(a.name) ? OK : NOT_FOUND;
      case COMMAND:
        return runCommand(a.name);
      case DIRECT:
        return runDirect(a);
      default:
        return runName(a);
    }
  }

  private int runName(Alt a) throws InterruptedException {
    if (a.onlyApp != null && !env.inApp(a.onlyApp)) {
      return NOT_FOUND;
    }
    for (String c : a.conds) {
      boolean negate = c.startsWith("!");
      String v = negate ? c.substring(1) : c;
      boolean has = isFocusType(v) ? env.focusIs(v) : env.screenContains(v);
      if (has == negate) {
        return NOT_FOUND;
      }
    }
    int times = Math.max(1, a.repeat);
    for (int i = 0; i < times; i++) {
      List<Object> found = env.findAll(a.name, a.byLabel);
      Object node = pick(found, a.index);
      if (node == null) {
        return i == 0 ? NOT_FOUND : OK; // later repeats: the target vanished, that is fine
      }
      boolean ok = a.locateOnly ? env.locate(node) : env.click(node);
      if (!ok) {
        return FAILED;
      }
      if (i < times - 1) {
        env.sleep(150);
      }
    }
    return OK;
  }

  private static Object pick(List<Object> found, Integer index) {
    if (found == null || found.isEmpty()) {
      return null;
    }
    int n = found.size();
    int i = 0;
    if (index != null) {
      i = index > 0 ? index - 1 : n + index;
    }
    return i >= 0 && i < n ? found.get(i) : null;
  }

  private static boolean isFocusType(String v) {
    switch (v.trim().toLowerCase()) {
      case "可编辑":
      case "可点击":
      case "列表":
      case "列表项目":
      case "编辑框":
      case "editable":
      case "clickable":
      case "list":
      case "listitem":
      case "edit":
        return true;
      default:
        return false;
    }
  }

  private int runCommand(String body) {
    String fn = body;
    String args = null;
    Matcher m = FUNC.matcher(body);
    if (m.matches()) {
      fn = m.group(1);
      args = m.group(2);
    }
    if ("speak".equalsIgnoreCase(fn) && args != null) { // Step 14: %speak(text) speaks and goes on
      String say = args.trim();
      if (say.length() >= 2 && (say.startsWith("\"") || say.startsWith("'"))
          && say.charAt(say.length() - 1) == say.charAt(0)) {
        say = say.substring(1, say.length() - 1);
      }
      com.google.android.accessibility.talkback.editor.LuaServiceApi.speakStatic(say);
      return OK;
    }
    Boolean r = env.callFunction(fn, args);
    if (r != null) {
      if (!r) {
        halted = true; // function said "stop"
        return FAILED;
      }
      return OK;
    }
    return env.command(body) ? OK : FAILED;
  }

  // ------------------------------------------------------------------ [ ... ] direct forms

  private static boolean isNum(String s) {
    return NUMBER.matcher(s).matches();
  }

  private boolean appOk(String a) {
    if (a == null || a.isEmpty()) {
      return true;
    }
    if (a.startsWith("@")) {
      return env.inApp(a.substring(1));
    }
    if (a.startsWith("&")) {
      return env.screenContains(a.substring(1));
    }
    if (a.startsWith("!")) {
      return !env.screenContains(a.substring(1));
    }
    return env.inApp(a);
  }

  private int runDirect(Alt a) throws InterruptedException {
    String[] raw = a.name.split(",", -1);
    List<String> t = new ArrayList<>();
    for (String s : raw) {
      t.add(s.trim());
    }
    int n = t.size();
    boolean allNum = true;
    for (String s : t) {
      allNum &= isNum(s);
    }
    if (n == 1) {
      String s = t.get(0);
      if (s.startsWith("%")) {
        return env.tapFocus(50) ? OK : FAILED;
      }
      if (isNum(s)) {
        return env.tapFocus((long) Double.parseDouble(s)) ? OK : FAILED;
      }
      List<Object> found = env.findAll(s, false);
      Object node = pick(found, null);
      if (node == null) {
        return NOT_FOUND;
      }
      return env.rawTap(node, 50) ? OK : FAILED;
    }
    if (n == 2 && allNum) {
      return env.tap(d(t, 0), d(t, 1), 50) ? OK : FAILED;
    }
    if (n == 3 && allNum) {
      return env.tap(d(t, 0), d(t, 1), (long) d(t, 2)) ? OK : FAILED;
    }
    if (n == 4 && isNum(t.get(0)) && isNum(t.get(1)) && isNum(t.get(2))) {
      int times = (int) d(t, 2);
      for (int i = 0; i < times; i++) {
        if (!appOk(t.get(3))) {
          return FAILED; // left the app: cancel
        }
        if (!env.tap(d(t, 0), d(t, 1), 50)) {
          return FAILED;
        }
        env.sleep(60);
      }
      return OK;
    }
    if ((n == 5 || n == 6 || n == 7) && numbers(t, 5)) {
      int times = n >= 6 && isNum(t.get(5)) ? (int) d(t, 5) : 1;
      String app = n == 7 ? t.get(6) : null;
      for (int i = 0; i < Math.max(1, times); i++) {
        if (!appOk(app)) {
          return FAILED;
        }
        if (!env.swipe(d(t, 0), d(t, 1), d(t, 2), d(t, 3), (long) d(t, 4))) {
          return FAILED;
        }
        env.sleep(100);
      }
      return OK;
    }
    if (n == 8 && numbers(t, 8)) {
      boolean hit =
          env.clickColor(
              d(t, 0), d(t, 1), d(t, 2), d(t, 3), (int) d(t, 4), (int) d(t, 5), (int) d(t, 6),
              (int) d(t, 7));
      return hit ? OK : NOT_FOUND; // colour not there yet: the item may wait and retry
    }
    error = "unknown [..] form: [" + a.name + "]";
    return FAILED;
  }

  private static boolean numbers(List<String> t, int count) {
    for (int i = 0; i < count && i < t.size(); i++) {
      if (!isNum(t.get(i))) {
        return false;
      }
    }
    return true;
  }

  private static double d(List<String> t, int i) {
    return Double.parseDouble(t.get(i));
  }

  // ------------------------------------------------------------------ parsing

  /** Returns a problem text when the step line cannot be understood, otherwise null. */
  static String checkItem(String text) {
    for (Alt a : parseItem(text)) {
      if (a.error != null) {
        return a.error;
      }
    }
    return null;
  }

  /** Splits "a|b|c" (not inside [..]) and parses every part. */
  static List<Alt> parseItem(String text) {
    List<Alt> out = new ArrayList<>();
    String s = text == null ? "" : text.trim();
    if (s.isEmpty()) {
      return out;
    }
    int depth = 0;
    int start = 0;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '[') {
        depth++;
      } else if (c == ']') {
        depth = Math.max(0, depth - 1);
      } else if (c == '|' && depth == 0) {
        addAlt(out, s.substring(start, i));
        start = i + 1;
      }
    }
    addAlt(out, s.substring(start));
    return out;
  }

  private static void addAlt(List<Alt> out, String part) {
    String s = part.trim();
    if (!s.isEmpty()) {
      out.add(parseAlt(s));
    }
  }

  static Alt parseAlt(String s) {
    Alt a = new Alt();
    try {
      if (s.startsWith("[")) {
        int close = s.lastIndexOf(']');
        if (close < 0) {
          a.error = "missing ] in " + s;
          a.kind = Kind.DIRECT;
          return a;
        }
        a.kind = Kind.DIRECT;
        a.name = s.substring(1, close);
        parseModifiers(a, s.substring(close + 1), false);
        return a;
      }
      if (s.startsWith("%")) {
        a.kind = Kind.COMMAND;
        int dollar = trailingDollar(s);
        a.name = (dollar >= 0 ? s.substring(1, dollar) : s.substring(1)).trim();
        if (dollar >= 0) {
          a.delayMs = Long.parseLong(s.substring(dollar + 1).trim());
        }
        return a;
      }
      if (s.startsWith(">")) {
        a.kind = Kind.OPEN_APP;
        int dollar = trailingDollar(s);
        a.name = (dollar >= 0 ? s.substring(1, dollar) : s.substring(1)).trim();
        if (dollar >= 0) {
          a.delayMs = Long.parseLong(s.substring(dollar + 1).trim());
        }
        return a;
      }
      if (s.startsWith("&") && isFocusType(s.substring(1))) {
        a.kind = Kind.CHECK;
        a.name = s.substring(1).trim();
        return a;
      }
      a.kind = Kind.NAME;
      int i = 0;
      while (i < s.length() && (s.charAt(i) == '#' || s.charAt(i) == '<')) {
        if (s.charAt(i) == '#') {
          a.byLabel = true;
        } else {
          a.locateOnly = true;
        }
        i++;
      }
      int j = i;
      while (j < s.length() && SYMBOLS.indexOf(s.charAt(j)) < 0) {
        j++;
      }
      a.name = s.substring(i, j).trim();
      parseModifiers(a, s.substring(j), true);
      if (a.name.isEmpty()) {
        a.error = "empty button name in: " + s;
      }
    } catch (NumberFormatException e) {
      a.error = "bad number in: " + s;
    }
    return a;
  }

  private static int trailingDollar(String s) {
    int d = s.lastIndexOf('$');
    if (d < 0) {
      return -1;
    }
    return isNum(s.substring(d + 1).trim()) ? d : -1;
  }

  /** Reads $ms, <n, >n, #n, @app, &cond groups from the rest of an item. */
  private static void parseModifiers(Alt a, String rest, boolean full) {
    int j = 0;
    while (j < rest.length()) {
      char sym = rest.charAt(j);
      if (SYMBOLS.indexOf(sym) < 0) {
        a.error = "unexpected text after item: " + rest.substring(j);
        return;
      }
      int k = j + 1;
      String stops = sym == '&' ? "$<>@" : SYMBOLS;
      while (k < rest.length() && stops.indexOf(rest.charAt(k)) < 0) {
        k++;
      }
      String val = rest.substring(j + 1, k).trim();
      switch (sym) {
        case '#':
          a.index = Integer.parseInt(val);
          break;
        case '@':
          a.onlyApp = val;
          break;
        case '$':
          a.delayMs = Long.parseLong(val);
          break;
        case '<':
          a.repeat = Integer.parseInt(val);
          break;
        case '>':
          a.waitSec = Long.parseLong(val);
          break;
        default:
          a.conds.add(val);
          break;
      }
      j = k;
    }
  }
}
