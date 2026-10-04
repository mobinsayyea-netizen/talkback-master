package com.google.android.accessibility.talkback.editor;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;
import com.androlua.LuaContext;
import com.androlua.LuaEnhancer;
import com.androlua.LuaGcable;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.utils.traversal.TraversalStrategy;
import com.google.android.accessibility.utils.Performance;
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions;
import com.luajava.LuaState;
import com.luajava.LuaStateFactory;
import dalvik.system.DexClassLoader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Lua "service" object: Lua scripts see it as the global `service`. It is also the LuaContext
 * that AndroLua's import.lua expects. One instance = one Lua state = one script run.
 */
public class LuaServiceApi implements LuaContext {
  private static final int MAX_DEPTH = 4;
  private static int depth = 0;

  private final Context ctx;
  private final File scriptDir;
  private final LuaState L;
  private final Handler ui = new Handler(Looper.getMainLooper());
  private final Map<String, Object> globalData = new HashMap<>();
  private final ArrayList<ClassLoader> loaders = new ArrayList<>();
  private final HashMap<String, String> libs = new HashMap<>();
  private final StringBuilder log = new StringBuilder();
  private String luaExtDir;
  private volatile boolean closed;

  /** Runs a script. Returns null on success, or an error text. Always call from a worker thread. */
  public static String run(Context ctx, File script, AccessibilityNodeInfo node) {
    return run(ctx, script, node, null);
  }

  /** Speaks a status text without needing a script (used for extension errors). */
  public static void speakStatic(String text) {
    TalkBackService s = TalkBackService.getInstance();
    if (s != null && text != null) {
      s.getSpeechController().speak(text, Performance.EVENT_ID_UNTRACKED, SpeakOptions.create());
    }
  }

  /**
   * Same as {@link #run(Context, File, AccessibilityNodeInfo)}; every entry of {@code extras}
   * becomes a global string in Lua (used for event_type, event_package, event_text).
   */
  public static String run(
      Context ctx,
      File script,
      AccessibilityNodeInfo node,
      java.util.Map<String, String> extras) {
    if (depth >= MAX_DEPTH) {
      return "Too many nested scripts";
    }
    depth++;
    LuaServiceApi api = null;
    try {
      api = new LuaServiceApi(ctx.getApplicationContext(), script.getParentFile());
      if (extras != null) {
        for (java.util.Map.Entry<String, String> e : extras.entrySet()) {
          api.L.pushString(e.getValue() == null ? "" : e.getValue());
          api.L.setGlobal(e.getKey());
        }
      }
      return api.runFile(script, node);
    } catch (Throwable t) {
      return "Error: " + t;
    } finally {
      depth--;
      if (api != null) {
        api.close();
      }
    }
  }

  private LuaServiceApi(Context c, File dir) throws Exception {
    ctx = c;
    scriptDir = dir;
    luaExtDir = LuaPaths.base(c).getAbsolutePath();
    LuaEnhancer.sContext = c;
    File luaLib = prepareLuaAssets();
    L = LuaStateFactory.newLuaState();
    L.openLibs();
    L.pushJavaObject(this);
    L.setGlobal("service");
    L.pushContext(this);
    L.getGlobal("luajava");
    L.pushString(luaExtDir);
    L.setField(-2, "luaextdir");
    L.pushString(scriptDir.getAbsolutePath());
    L.setField(-2, "luadir");
    L.pushString(getLuaPath());
    L.setField(-2, "luapath");
    L.pop(1);
    L.getGlobal("package");
    L.pushString(luaLib.getAbsolutePath() + "/?.lua;" + scriptDir.getAbsolutePath() + "/?.lua");
    L.setField(-2, "path");
    L.pushString(c.getApplicationInfo().nativeLibraryDir + "/lib?.so");
    L.setField(-2, "cpath");
    L.pop(1);
    L.LdoString(
        "local concat=table.concat\n"
            + "function print(...)\n"
            + "  local t={}\n"
            + "  for i=1,select('#',...) do t[#t+1]=tostring((select(i,...))) end\n"
            + "  service.log(concat(t,'\\t'))\n"
            + "end\n");
  }

  private File prepareLuaAssets() throws Exception {
    File out = new File(ctx.getFilesDir(), "lua");
    out.mkdirs();
    String[] names = ctx.getAssets().list("lua");
    if (names != null) {
      for (String n : names) {
        File f = new File(out, n);
        try (InputStream in = ctx.getAssets().open("lua/" + n)) {
          if (f.exists() && f.length() == in.available()) {
            continue;
          }
        }
        try (InputStream in = ctx.getAssets().open("lua/" + n);
            FileOutputStream os = new FileOutputStream(f)) {
          byte[] buf = new byte[8192];
          int r;
          while ((r = in.read(buf)) > 0) {
            os.write(buf, 0, r);
          }
        }
      }
    }
    return out;
  }

  private String runFile(File script, AccessibilityNodeInfo node) {
    if (node != null) {
      L.pushJavaObject(node);
      L.setGlobal("node");
    }
    int top = L.getTop();
    int ok = L.LloadFile(script.getAbsolutePath());
    if (ok == 0) {
      ok = L.pcall(0, 0, 0);
    }
    if (ok != 0) {
      String err = L.toString(-1);
      L.setTop(top);
      return "Error: " + err;
    }
    return null;
  }

  private void close() {
    closed = true;
    try {
      L.close();
    } catch (Throwable ignored) {
    }
  }

  // ---------------------------------------------------------------- service.* for scripts

  /** service.execute("Click") etc.: any English, Chinese or id spelling of Jieshuo's 173 commands. */
  public boolean execute(String name) {
    return name != null && JxCommands.run(this, ctx, name, null);
  }

  private boolean focusedAction(TalkBackService s, int action) {
    AccessibilityNodeInfo root = s.getRootInActiveWindow();
    if (root == null) {
      return false;
    }
    AccessibilityNodeInfo f = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
    if (f == null) {
      f = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
    }
    return f != null && f.performAction(action);
  }

  /** service.click("Button text"): clicks the first visible node with that text/description. */
  public boolean click(String text) {
    TalkBackService s = TalkBackService.getInstance();
    if (s == null || text == null) {
      return false;
    }
    AccessibilityNodeInfo root = s.getRootInActiveWindow();
    if (root == null) {
      return false;
    }
    List<AccessibilityNodeInfo> found = root.findAccessibilityNodeInfosByText(text);
    if (found != null) {
      for (AccessibilityNodeInfo n : found) {
        AccessibilityNodeInfo p = n;
        while (p != null) {
          if (p.isClickable() && p.isVisibleToUser() && p.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return true;
          }
          p = p.getParent();
        }
      }
    }
    return false;
  }

  /** service.startApp("App name" or "package.name"). */
  public boolean startApp(String labelOrPackage) {
    PackageManager pm = ctx.getPackageManager();
    Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
    for (ResolveInfo ri : pm.queryIntentActivities(main, 0)) {
      String label = String.valueOf(ri.loadLabel(pm));
      String pkg = ri.activityInfo.packageName;
      if (label.equalsIgnoreCase(labelOrPackage) || pkg.equals(labelOrPackage)) {
        Intent launch = pm.getLaunchIntentForPackage(pkg);
        if (launch != null) {
          launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          ctx.startActivity(launch);
          return true;
        }
      }
    }
    return false;
  }

  /** service.plugin("name", node): runs Extensions/name/main.lua */
  public boolean plugin(String name, Object node) {
    return runNamed(LuaPaths.extensions(ctx), name, node);
  }

  /** service.tool("name", node): runs Tools/name/main.lua */
  public boolean tool(String name, Object node) {
    return runNamed(LuaPaths.tools(ctx), name, node);
  }

  private boolean runNamed(File base, String name, Object node) {
    File main = new File(new File(base, name), "main.lua");
    if (!main.exists()) {
      return false;
    }
    String err =
        run(ctx, main, node instanceof AccessibilityNodeInfo ? (AccessibilityNodeInfo) node : null);
    if (err != null) {
      log(err);
    }
    return err == null;
  }

  // ======================================================================================
  // Jieshuo-compatible API (zip 14). Names and argument forms follow Jieshuo's service class.
  // Numbers: Lua integers >= 1 are pixels; a fractional value between 0 and 1 is a share of the
  // screen (Jieshuo doc: "integer = absolute, decimal = relative"). UNVERIFIED: not compiled.
  // ======================================================================================

  private static final int FOCUS_BACKWARD = 1; // View.FOCUS_BACKWARD
  private static final int FOCUS_FORWARD = 2; // View.FOCUS_FORWARD

  private float px(double v, boolean horizontal) {
    int size = horizontal ? getWidth() : getHeight();
    if (v > 0 && v < 1) {
      return (float) (v * size);
    }
    return (float) v;
  }

  private static android.graphics.Rect boundsOf(AccessibilityNodeInfo n) {
    android.graphics.Rect r = new android.graphics.Rect();
    n.getBoundsInScreen(r);
    return r;
  }

  AccessibilityNodeInfo focusedNode() {
    TalkBackService s = TalkBackService.getInstance();
    return s == null ? null : s.msFocusedNode();
  }

  AccessibilityNodeInfo inputNode() {
    TalkBackService s = TalkBackService.getInstance();
    if (s == null) {
      return null;
    }
    AccessibilityNodeInfo root = s.getRootInActiveWindow();
    if (root == null) {
      return null;
    }
    AccessibilityNodeInfo f = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
    if (f == null) {
      AccessibilityNodeInfo a = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
      if (a != null && a.isEditable()) {
        f = a;
      }
    }
    return f;
  }

  /** service.execute(cmd, node): the node is accepted for compatibility. */
  public boolean execute(String name, AccessibilityNodeInfo node) {
    return name != null && JxCommands.run(this, ctx, name, node);
  }

  /** service.postExecute(ms, cmd, node). */
  public void postExecute(long ms, final String name, final AccessibilityNodeInfo node) {
    ui.postDelayed(() -> execute(name, node), Math.max(0L, ms));
  }

  public String getText(AccessibilityNodeInfo n) {
    if (n == null) {
      return "";
    }
    CharSequence t = n.getText();
    if (t == null || t.length() == 0) {
      t = n.getContentDescription();
    }
    return t == null ? "" : t.toString();
  }

  public boolean setText(AccessibilityNodeInfo n, String text) {
    if (n == null || text == null) {
      return false;
    }
    android.os.Bundle args = new android.os.Bundle();
    args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
    return n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
  }

  public boolean setText(String text) {
    return setText(inputNode(), text);
  }

  private android.content.ClipboardManager clipboard() {
    return (android.content.ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
  }

  private String clipText() {
    android.content.ClipboardManager cm = clipboard();
    if (cm == null || !cm.hasPrimaryClip() || cm.getPrimaryClip().getItemCount() == 0) {
      return "";
    }
    CharSequence t = cm.getPrimaryClip().getItemAt(0).coerceToText(ctx);
    return t == null ? "" : t.toString();
  }

  public boolean copy(CharSequence text) {
    android.content.ClipboardManager cm = clipboard();
    if (cm == null || text == null) {
      return false;
    }
    cm.setPrimaryClip(android.content.ClipData.newPlainText("text", text));
    return true;
  }

  public boolean copy() {
    String t = getText(focusedNode());
    return t.length() > 0 && copy(t);
  }

  public boolean appendCopy(CharSequence text) {
    return text != null && copy(clipText() + text);
  }

  public boolean appendCopy() {
    String t = getText(focusedNode());
    return t.length() > 0 && appendCopy(t);
  }

  public boolean paste(AccessibilityNodeInfo n) {
    return n != null && n.performAction(AccessibilityNodeInfo.ACTION_PASTE);
  }

  public boolean paste() {
    return paste(inputNode());
  }

  public boolean paste(CharSequence text) {
    return copy(text) && paste();
  }

  public boolean paste(AccessibilityNodeInfo n, CharSequence text) {
    return copy(text) && paste(n);
  }

  private boolean moveFocus(final int direction) {
    final TalkBackService s = TalkBackService.getInstance();
    if (s == null) {
      return false;
    }
    ui.post(() -> s.msFocusFeedback(Feedback.focusDirection(direction)));
    return true;
  }

  public boolean toNext() {
    return moveFocus(TraversalStrategy.SEARCH_FOCUS_FORWARD);
  }

  public boolean toPrevious() {
    return moveFocus(TraversalStrategy.SEARCH_FOCUS_BACKWARD);
  }

  /** Focus moves relative to the given node. The "include children" flag is accepted only. */
  private boolean moveFrom(AccessibilityNodeInfo n, int viewDirection) {
    if (n == null) {
      return false;
    }
    AccessibilityNodeInfo t = n.focusSearch(viewDirection);
    return t != null && t.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
  }

  public boolean toNext(AccessibilityNodeInfo n, boolean withChildren) {
    return moveFrom(n, FOCUS_FORWARD);
  }

  public boolean toPrevious(AccessibilityNodeInfo n, boolean withChildren) {
    return moveFrom(n, FOCUS_BACKWARD);
  }

  public AccessibilityNodeInfo getRootNodeInfo() {
    TalkBackService s = TalkBackService.getInstance();
    return s == null ? null : s.getRootInActiveWindow();
  }

  private static String cls(AccessibilityNodeInfo n) {
    CharSequence c = n == null ? null : n.getClassName();
    return c == null ? "" : c.toString();
  }

  public boolean isEditView(AccessibilityNodeInfo n) {
    return n != null && (n.isEditable() || cls(n).contains("EditText"));
  }

  public boolean isSeekBar(AccessibilityNodeInfo n) {
    return n != null && (cls(n).contains("SeekBar") || n.getRangeInfo() != null);
  }

  public boolean isListView(AccessibilityNodeInfo n) {
    String c = cls(n);
    return n != null
        && (c.contains("ListView")
            || c.contains("RecyclerView")
            || c.contains("GridView")
            || n.getCollectionInfo() != null);
  }

  public boolean isInListView(AccessibilityNodeInfo n) {
    AccessibilityNodeInfo p = n == null ? null : n.getParent();
    int guard = 0;
    while (p != null && guard++ < 50) {
      if (isListView(p)) {
        return true;
      }
      p = p.getParent();
    }
    return false;
  }

  public boolean isInWebView(AccessibilityNodeInfo n) {
    AccessibilityNodeInfo p = n;
    int guard = 0;
    while (p != null && guard++ < 50) {
      if (cls(p).contains("WebView")) {
        return true;
      }
      p = p.getParent();
    }
    return false;
  }

  public boolean isClick(AccessibilityNodeInfo n) {
    return n != null && n.isClickable();
  }

  // ---- touch simulation (Android 7+ dispatchGesture)

  public boolean click(double x, double y) {
    return JxGesture.tap(TalkBackService.getInstance(), px(x, true), px(y, false), 50);
  }

  public boolean longClick(double x, double y) {
    return JxGesture.tap(
        TalkBackService.getInstance(),
        px(x, true),
        px(y, false),
        android.view.ViewConfiguration.getLongPressTimeout() + 150L);
  }

  public boolean press(double x, double y, int ms) {
    return JxGesture.tap(TalkBackService.getInstance(), px(x, true), px(y, false), ms);
  }

  public boolean swipe(double x1, double y1, double x2, double y2, int ms) {
    return JxGesture.swipe(
        TalkBackService.getInstance(), px(x1, true), px(y1, false), px(x2, true), px(y2, false), ms);
  }

  public boolean swipe(double x1, double y1, double x2, double y2, int ms, int times) {
    boolean ok = true;
    for (int i = 0; i < Math.max(1, times); i++) {
      ok = swipe(x1, y1, x2, y2, ms) && ok;
    }
    return ok;
  }

  boolean tapNode(AccessibilityNodeInfo n, long ms) {
    android.graphics.Rect r = boundsOf(n);
    if (r.isEmpty() || r.centerX() < 0 || r.centerY() < 0
        || r.centerX() > getWidth() || r.centerY() > getHeight()) {
      return false;
    }
    return JxGesture.tap(TalkBackService.getInstance(), r.centerX(), r.centerY(), ms);
  }

  /** Real tap at the centre first (Jieshuo doc), then the accessibility click as fallback. */
  public boolean toClick(AccessibilityNodeInfo n) {
    if (n == null) {
      return false;
    }
    if (tapNode(n, 50)) {
      return true;
    }
    AccessibilityNodeInfo p = n;
    int guard = 0;
    while (p != null && guard++ < 20) {
      if (p.isClickable() && p.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
        return true;
      }
      p = p.getParent();
    }
    return false;
  }

  public boolean toClick() {
    return toClick(focusedNode());
  }

  public boolean toClick(double x, double y) {
    return click(x, y);
  }

  public boolean toLongClick(AccessibilityNodeInfo n) {
    if (n == null) {
      return false;
    }
    if (tapNode(n, android.view.ViewConfiguration.getLongPressTimeout() + 150L)) {
      return true;
    }
    return n.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);
  }

  public boolean toLongClick() {
    return toLongClick(focusedNode());
  }

  public boolean longClick(AccessibilityNodeInfo n) {
    return toLongClick(n);
  }

  public boolean toBack() {
    return execute("back");
  }

  public boolean toHome() {
    return execute("home");
  }

  public boolean toNotification() {
    return execute("notification bar");
  }

  public boolean toRecents() {
    return execute("recents");
  }

  /** Jieshuo's own doc says toParent opens the recent-apps list; kept the same. */
  public boolean toParent() {
    return execute("recents");
  }

  private boolean scroll(AccessibilityNodeInfo n, int action) {
    AccessibilityNodeInfo p = n;
    int guard = 0;
    while (p != null && guard++ < 20) {
      if (p.isScrollable() && p.performAction(action)) {
        return true;
      }
      p = p.getParent();
    }
    return false;
  }

  public boolean scrollForward(AccessibilityNodeInfo n) {
    return scroll(n, AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
  }

  public boolean scrollBackward(AccessibilityNodeInfo n) {
    return scroll(n, AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
  }

  void postToUi(Runnable r) {
    ui.post(r);
  }

  /** Calls a global Lua function by name. Null = no such function (or the script already ended). */
  Boolean callLuaFunction(String name, String args) {
    if (closed || name == null) {
      return null;
    }
    synchronized (L) {
      int top = L.getTop();
      try {
        L.getGlobal(name);
        if (!L.isFunction(-1)) {
          L.setTop(top);
          return null;
        }
        int n = 0;
        if (args != null && !args.trim().isEmpty()) {
          for (String a : args.split(",")) {
            String t = a.trim();
            try {
              L.pushNumber(Double.parseDouble(t));
            } catch (NumberFormatException e) {
              L.pushString(t);
            }
            n++;
          }
        }
        int rc = L.pcall(n, 1, 0);
        if (rc != 0) {
          String err = L.toString(-1);
          L.setTop(top);
          log("Error in " + name + ": " + err);
          return Boolean.FALSE;
        }
        boolean r = L.isNil(-1) || L.toBoolean(-1); // a function that returns nothing = continue
        L.setTop(top);
        return r;
      } catch (Throwable t) {
        try {
          L.setTop(top);
        } catch (Throwable ignored) {
        }
        log("Error in " + name + ": " + t);
        return Boolean.FALSE;
      }
    }
  }

  /** Lua table -> nested Java lists of strings (what JxClickScript reads). */
  @SuppressWarnings({"rawtypes", "unchecked"})
  private static List<Object> toList(Object o) {
    List<Object> out = new ArrayList<>();
    if (!(o instanceof com.luajava.LuaTable)) {
      return out;
    }
    com.luajava.LuaTable t = (com.luajava.LuaTable) o;
    int n = t.length();
    for (int i = 1; i <= n; i++) {
      Object v = t.get(Long.valueOf(i));
      if (v instanceof com.luajava.LuaTable) {
        out.add(toList(v));
      } else {
        out.add(v == null ? "" : String.valueOf(v));
      }
    }
    return out;
  }

  /** service.click({...}): the auto-click language. True when a chain of steps finished. */
  public boolean click(com.luajava.LuaTable list) {
    JxClickScript script = new JxClickScript(new JxClickEnv(this, ctx));
    boolean ok = script.run(toList(list));
    if (script.getError() != null) {
      log(script.getError());
    }
    return ok;
  }

  /** service.click({...}, function(result) ... end): same, then calls the function with the result. */
  public boolean click(com.luajava.LuaTable list, com.luajava.LuaFunction done) {
    boolean ok = click(list);
    if (done != null) {
      try {
        done.call(ok);
      } catch (Throwable t) {
        log("Error in click callback: " + t);
      }
    }
    return ok;
  }

  /** service.postClick(ms, {...}): runs the click list later, on its own thread. */
  public void postClick(final long ms, com.luajava.LuaTable list) {
    final List<Object> parsed = toList(list);
    Thread th =
        new Thread(
            () -> {
              try {
                Thread.sleep(Math.max(0L, ms));
              } catch (InterruptedException e) {
                return;
              }
              JxClickScript script = new JxClickScript(new JxClickEnv(this, ctx));
              script.run(parsed);
            },
            "lua-postClick");
    th.setDaemon(true);
    th.start();
  }

  public void postClick(long ms, com.luajava.LuaTable list, com.luajava.LuaFunction done) {
    postClick(ms, list);
  }

  public void speak(String text) {
    TalkBackService s = TalkBackService.getInstance();
    if (s != null && text != null) {
      s.getSpeechController().speak(text, Performance.EVENT_ID_UNTRACKED, SpeakOptions.create());
    }
  }

  public void toast(final String text) {
    ui.post(() -> Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show());
  }

  public void log(String text) {
    synchronized (log) {
      log.append(text).append('\n');
    }
    speak(text);
  }

  public ClassLoader loadDex(String path) {
    DexClassLoader l =
        new DexClassLoader(path, ctx.getCacheDir().getAbsolutePath(), null, ctx.getClassLoader());
    loaders.add(l);
    return l;
  }

  public HashMap<String, String> getLibrarys() {
    return libs;
  }

  // ---------------------------------------------------------------- LuaContext

  @Override public ArrayList<ClassLoader> getClassLoaders() { return loaders; }
  @Override public void call(String func, Object... args) {}
  @Override public void set(String name, Object value) {
    try {
      L.pushObjectValue(value);
      L.setGlobal(name);
    } catch (Exception ignored) {
    }
  }
  @Override public String getLuaPath() { return scriptDir.getAbsolutePath(); }
  @Override public String getLuaPath(String path) { return new File(scriptDir, path).getAbsolutePath(); }
  @Override public String getLuaPath(String dir, String name) { return new File(new File(scriptDir, dir), name).getAbsolutePath(); }
  @Override public String getLuaDir() { return scriptDir.getAbsolutePath(); }
  @Override public String getLuaDir(String dir) { return new File(scriptDir, dir).getAbsolutePath(); }
  @Override public String getLuaExtDir() { return luaExtDir; }
  @Override public String getLuaExtDir(String dir) { return new File(luaExtDir, dir).getAbsolutePath(); }
  @Override public void setLuaExtDir(String dir) { luaExtDir = dir; }
  @Override public String getLuaExtPath(String path) { return new File(luaExtDir, path).getAbsolutePath(); }
  @Override public String getLuaExtPath(String dir, String name) { return new File(new File(luaExtDir, dir), name).getAbsolutePath(); }
  @Override public String getLuaLpath() { return scriptDir.getAbsolutePath() + "/?.lua"; }
  @Override public String getLuaCpath() { return ctx.getApplicationInfo().nativeLibraryDir + "/lib?.so"; }
  @Override public Context getContext() { return ctx; }
  @Override public LuaState getLuaState() { return L; }
  @Override public Object doFile(String path, Object... arg) {
    String err = runFile(new File(path), null);
    if (err != null) {
      log(err);
    }
    return null;
  }
  @Override public void sendMsg(String msg) { log(msg); }
  @Override public void sendError(String title, Exception msg) { log(title + ": " + msg); }
  @Override public int getWidth() { return ctx.getResources().getDisplayMetrics().widthPixels; }
  @Override public int getHeight() { return ctx.getResources().getDisplayMetrics().heightPixels; }
  @Override public Map getGlobalData() { return globalData; }
  @Override public Object getSharedData(String key) { return globalData.get(key); }
  @Override public Object getSharedData(String key, Object def) {
    Object v = globalData.get(key);
    return v == null ? def : v;
  }
  @Override public boolean setSharedData(String key, Object value) {
    globalData.put(key, value);
    return true;
  }
  @Override public void regGc(LuaGcable obj) {}
}
