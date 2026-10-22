package com.google.android.accessibility.talkback.editor;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.BatteryManager;
import android.os.Bundle;
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
public class LuaServiceApi extends android.content.ContextWrapper implements LuaContext {
  private static final int MAX_PENDING_TASKS = 200;
  private static final long MAX_TASK_DELAY_MS = 10 * 60 * 1000L;
  private static final java.util.concurrent.ScheduledExecutorService TASKS =
      java.util.concurrent.Executors.newScheduledThreadPool(2);

  // Lua is single threaded: the main script, every callback and every task take the LuaState itself as the lock, one at a time
  // (com.androlua.* classes synchronize on getLuaState() too).

  private final java.util.concurrent.atomic.AtomicInteger pending =
      new java.util.concurrent.atomic.AtomicInteger();
  private volatile boolean mainDone;

  private static final int MAX_DEPTH = 4;
  private static int depth = 0;

  private final Context ctx;
  private final File scriptDir;
  private final LuaState L;
  private File luaLibDir;
  private final Handler ui = new Handler(Looper.getMainLooper());
  private final Map<String, Object> globalData = new HashMap<>();
  private final ArrayList<ClassLoader> loaders = new ArrayList<>();
  // Keep Lua alive while LuaAsyncTask/LuaThread/LuaTimerTask objects still depend on its state.
  private final java.util.Set<LuaGcable> gcables =
      java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<LuaGcable, Boolean>());
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

  /** Runs one execute() command by name, no script needed (used by gesture schemes). Worker thread. */
  public static boolean executeStatic(Context ctx, String name) {
    LuaServiceApi api = null;
    try {
      com.google.android.accessibility.talkback.MsErrorLog.crumb("command " + name);
      api = new LuaServiceApi(ctx.getApplicationContext(), LuaPaths.base(ctx));
      return name != null && JxCommands.run(api, api.ctx, name, null);
    } catch (Throwable t) {
      com.google.android.accessibility.talkback.MsErrorLog.log("Command " + name, t);
      return false;
    } finally {
      if (api != null) {
        api.finishMain();
      }
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
    if (JavaPluginRunner.isJava(script)) {
      return JavaPluginRunner.run(ctx, script, node, extras);
    }
    if (depth >= MAX_DEPTH) {
      return "Too many nested scripts";
    }
    depth++;
    LuaServiceApi api = null;
    try {
      com.google.android.accessibility.talkback.MsErrorLog.crumb(
          "script " + script.getParentFile().getName());
      api = new LuaServiceApi(ctx.getApplicationContext(), script.getParentFile());
      if (extras != null) {
        for (java.util.Map.Entry<String, String> e : extras.entrySet()) {
          api.L.pushString(e.getValue() == null ? "" : e.getValue());
          api.L.setGlobal(e.getKey());
        }
      }
      synchronized (api.L) {
        String runErr = api.runFile(script, node);
        if (runErr != null) {
          com.google.android.accessibility.talkback.MsErrorLog.log("Lua script " + script.getParentFile().getName(), runErr);
        }
        return runErr;
      }
    } catch (Throwable t) {
      com.google.android.accessibility.talkback.MsErrorLog.log("Lua script " + script.getParentFile().getName(), t);
      return "Error: " + t;
    } finally {
      depth--;
      if (api != null) {
        api.finishMain();
      }
    }
  }

  /**
   * Runs a script for a hook: the script runs once (so its functions exist), then the global Lua
   * function {@code hook} is called with (package, text). Returns null on success or an error text.
   * The global service_hook holds the hook name while the script runs. Worker thread only.
   */
  public static String runHook(Context ctx, File script, String hook, String pkg, String text) {
    if (JavaPluginRunner.isJava(script)) {
      return JavaPluginRunner.runHook(ctx, script, hook, pkg, text);
    }
    if (depth >= MAX_DEPTH) {
      return "Too many nested scripts";
    }
    depth++;
    LuaServiceApi api = null;
    try {
      com.google.android.accessibility.talkback.MsErrorLog.crumb(
          "hook " + hook + " in " + script.getParentFile().getName() + " (" + pkg + ")");
      api = new LuaServiceApi(ctx.getApplicationContext(), script.getParentFile());
      api.L.pushString(hook);
      api.L.setGlobal("service_hook");
      synchronized (api.L) {
        String err = api.runFile(script, null);
        if (err != null) {
          com.google.android.accessibility.talkback.MsErrorLog.log("Lua hook " + hook + " in " + script.getParentFile().getName(), err);
          return err;
        }
        String hookErr = api.callHook(hook, pkg, text);
        if (hookErr != null) {
          com.google.android.accessibility.talkback.MsErrorLog.log("Lua hook " + hook + " in " + script.getParentFile().getName(), hookErr);
        }
        return hookErr;
      }
    } catch (Throwable t) {
      com.google.android.accessibility.talkback.MsErrorLog.log("Lua hook " + hook + " in " + script.getParentFile().getName(), t);
      return "Error: " + t;
    } finally {
      depth--;
      if (api != null) {
        api.finishMain();
      }
    }
  }

  /** Runs an opt-in hook and returns true only when its Lua function explicitly returns true. */
  public static boolean runHookBoolean(Context ctx, File script, String hook, Object... args) {
    if (JavaPluginRunner.isJava(script)) {
      return JavaPluginRunner.runHookBoolean(ctx, script, hook, args);
    }
    if (depth >= MAX_DEPTH || script == null || !script.isFile() || hook == null) return false;
    depth++;
    LuaServiceApi api = null;
    try {
      com.google.android.accessibility.talkback.MsErrorLog.crumb(
          "hook " + hook + " in " + script.getParentFile().getName());
      api = new LuaServiceApi(ctx.getApplicationContext(), script.getParentFile());
      api.L.pushString(hook);
      api.L.setGlobal("service_hook");
      synchronized (api.L) {
        String err = api.runFile(script, null);
        if (err != null) return false;
        int top = api.L.getTop();
        api.L.getGlobal(hook);
        if (!api.L.isFunction(-1)) { api.L.setTop(top); return false; }
        int count = args == null ? 0 : args.length;
        if (args != null) for (Object arg : args) api.L.pushObjectValue(arg);
        int rc = api.L.pcall(count, 1, 0);
        boolean consumed = rc == 0 && api.L.isBoolean(-1) && api.L.toBoolean(-1);
        if (rc != 0) api.log("Error in " + hook + ": " + api.L.toString(-1));
        api.L.setTop(top);
        return consumed;
      }
    } catch (Throwable t) {
      return false;
    } finally {
      depth--;
      if (api != null) api.finishMain();
    }
  }

  private String callHook(String name, String pkg, String text) {
    int top = L.getTop();
    L.getGlobal(name);
    if (!L.isFunction(-1)) {
      L.setTop(top);
      return null;
    }
    L.pushString(pkg == null ? "" : pkg);
    L.pushString(text == null ? "" : text);
    int rc = L.pcall(2, 1, 0);
    if (rc != 0) {
      String err = L.toString(-1);
      L.setTop(top);
      return "Error in " + name + ": " + err;
    }
    L.setTop(top);
    return null;
  }

  LuaServiceApi(Context c, File dir) throws Exception {
    // Step 42: the Lua `service` is now a real Context (wrapping the accessibility service), so scripts can
    // pass it where Android needs a Context: LinearLayout(service), AlertDialog.Builder(service), ArrayAdapter(service, ...).
    super(c);
    ctx = c;
    scriptDir = dir;
    luaExtDir = LuaPaths.base(c).getAbsolutePath();
    LuaEnhancer.sContext = c;
    File luaLib = prepareLuaAssets();
    luaLibDir = luaLib;
    L = LuaStateFactory.newLuaState();
    L.openLibs();
    L.pushJavaObject(this);
    L.setGlobal("service");
    L.pushJavaObject(this);
    L.setGlobal("this"); // Jieshuo scripts call this.speak(...) like service.speak(...)
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
            + "end\n"
            + "__jx_tasks={}\n"
            + "__jx_id=0\n"
            + "local function reg(fn,...)\n"
            + "  __jx_id=__jx_id+1\n"
            + "  __jx_tasks[__jx_id]={fn,table.pack(...)}\n"
            + "  return __jx_id\n"
            + "end\n"
            + "function __jx_run(id)\n"
            + "  local t=__jx_tasks[id]\n"
            + "  __jx_tasks[id]=nil\n"
            + "  if t then return t[1](table.unpack(t[2],1,t[2].n)) end\n"
            + "end\n"
            + "function thread(fn,...)\n"
            + "  service.scheduleTask(reg(fn,...),0)\n"
            + "end\n"
            + "function task(a,b,...)\n"
            + "  if type(a)=='function' and type(b)=='number' then a,b=b,a end\n"
            + "  if type(a)=='number' then\n"
            + "    service.scheduleTask(reg(b,...),a)\n"
            + "  else\n"
            + "    local cb=b\n"
            + "    service.scheduleTask(reg(function(...)\n"
            + "      local r=table.pack(a(...))\n"
            + "      if cb then cb(table.unpack(r,1,r.n)) end\n"
            + "    end,...),0)\n"
            + "  end\n"
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
    return runFileWithArgs(script, node, new Object[0]);
  }

  /** Runs a Lua file with positional arguments, as expected by AndroLua's doFile API. */
  private String runFileWithArgs(File script, AccessibilityNodeInfo node, Object... args) {
    if (node != null) {
      L.pushJavaObject(node);
      L.setGlobal("node");
    }
    int top = L.getTop();
    int ok = L.LloadFile(script.getAbsolutePath());
    if (ok == 0) {
      try {
        if (args != null) {
          for (Object arg : args) {
            L.pushObjectValue(arg);
          }
        }
        ok = L.pcall(args == null ? 0 : args.length, 0, 0);
      } catch (Throwable t) {
        L.setTop(top);
        return "Error: " + t;
      }
    }
    if (ok != 0) {
      String err = L.toString(-1);
      L.setTop(top);
      return "Error: " + err;
    }
    L.setTop(top);
    return null;
  }

  private void close() {
    synchronized (L) {
      if (closed) {
        return;
      }
      closed = true;
      stopEventTimer();
      events.clear();
      synchronized (EVENT_OWNERS) {
        if (EVENT_OWNERS.get(scriptDir.getAbsolutePath()) == this) {
          EVENT_OWNERS.remove(scriptDir.getAbsolutePath());
        }
      }
      try {
        L.close();
      } catch (Throwable ignored) {
      }
    }
  }

  /** Main script ended: close only after scheduled tasks and registered Lua objects finish. */
  void finishMain() {
    mainDone = true;
    closeIfIdle();
  }

  private void closeIfIdle() {
    boolean idle;
    synchronized (gcables) {
      idle = mainDone && pending.get() == 0 && gcables.isEmpty() && events.isEmpty();
    }
    if (idle) close();
  }

  /** Used by Lua thread(...) / task(...) (see the prelude): run task {@code id} after ms. */
  public void scheduleTask(double id, double ms) {
    if (closed || pending.get() >= MAX_PENDING_TASKS) {
      log("task refused (closed or too many)");
      return;
    }
    long delay = Math.max(0L, Math.min((long) ms, MAX_TASK_DELAY_MS));
    final int taskId = (int) id;
    pending.incrementAndGet();
    try {
      TASKS.schedule(() -> runTask(taskId), delay, java.util.concurrent.TimeUnit.MILLISECONDS);
    } catch (RuntimeException e) {
      pending.decrementAndGet();
      log("task not scheduled: " + e);
    }
  }

  private void runTask(int id) {
    try {
      synchronized (L) {
        if (closed) {
          return;
        }
        int top = L.getTop();
        L.getGlobal("__jx_run");
        L.pushNumber(id);
        int rc = L.pcall(1, 0, 0);
        if (rc != 0) {
          log("Error in task: " + L.toString(-1));
        }
        L.setTop(top);
      }
    } catch (Throwable t) {
      log("Error in task: " + t);
    } finally {
      if (pending.decrementAndGet() == 0 && mainDone) {
        close();
      }
    }
  }

  // ---------------------------------------------------------------- Step 48: functions Jieshuo scripts call
  // Found by loading every extension and tool of the user's Jieshuo backup: these were missing, so the
  // scripts stopped with "attempt to call a nil value". Written from how the scripts call them.

  private static final java.util.Map<String, LuaServiceApi> EVENT_OWNERS = new HashMap<>();
  private final java.util.Map<String, com.luajava.LuaFunction> events =
      new java.util.concurrent.ConcurrentHashMap<>();
  private java.util.concurrent.ScheduledFuture<?> timerFuture;
  private volatile Boolean timerFlag; // null: the script never called setTimerMode
  private int timerErrors;

  /** Jieshuo: {@code service.handler.post(fn)} runs fn on the main thread. */
  public final LuaHandler handler = new LuaHandler();

  public final class LuaHandler {
    public boolean post(com.luajava.LuaFunction f) {
      return postDelayed(f, 0L);
    }

    public boolean postDelayed(com.luajava.LuaFunction f, long ms) {
      if (f == null || closed || pending.get() >= MAX_PENDING_TASKS) {
        return false;
      }
      pending.incrementAndGet();
      ui.postDelayed(
          new Runnable() {
            @Override
            public void run() {
              try {
                callFunction(f);
              } finally {
                pending.decrementAndGet();
                closeIfIdle();
              }
            }
          },
          Math.max(0L, Math.min(ms, MAX_TASK_DELAY_MS)));
      return true;
    }

    public boolean postDelayed(long ms, com.luajava.LuaFunction f) {
      return postDelayed(f, ms);
    }
  }

  /** Runs a Lua function of this state: one at a time, never throws. */
  private void callFunction(com.luajava.LuaFunction f, Object... args) {
    try {
      synchronized (L) {
        if (closed) {
          return;
        }
        f.call(args);
      }
    } catch (Throwable t) {
      log("Error in callback: " + t);
      com.google.android.accessibility.talkback.MsErrorLog.log("Lua callback", t);
    }
  }

  /** Jieshuo: {@code service.timer(fn, ms)} calls fn once after ms milliseconds. */
  public void timer(com.luajava.LuaFunction f, long ms) {
    handler.postDelayed(f, ms);
  }

  public void timer(long ms, com.luajava.LuaFunction f) {
    handler.postDelayed(f, ms);
  }

  /**
   * Jieshuo: {@code service.task(fn)} runs fn in the background in its own Lua state, so a slow web
   * request cannot block the screen reader. fn must not use variables from outside (same as AndroLua).
   */
  public void task(com.luajava.LuaFunction f) {
    if (f == null || closed) {
      return;
    }
    try {
      new com.androlua.LuaThread(this, f).start();
    } catch (Throwable t) {
      log("task failed: " + t);
      com.google.android.accessibility.talkback.MsErrorLog.log("Lua task", t);
    }
  }

  /**
   * Jieshuo: {@code service.regEvent("onTimer", fn)}. The timer event is called every interval (set with
   * setTimerInterval, default 1 second) until setTimerMode(false) or regEvent("onTimer", nil). Other
   * event names are accepted and ignored here (written to the script log).
   */
  public void regEvent(String name, com.luajava.LuaFunction f) {
    if (name == null || closed) {
      return;
    }
    if (f == null) {
      events.remove(name);
      if (events.isEmpty()) {
        stopEventTimer();
      }
      closeIfIdle();
      return;
    }
    if (!"onTimer".equals(name)) {
      log("regEvent: event \"" + name + "\" is not supported in Advanced Screen Reader");
      return;
    }
    String key = scriptDir.getAbsolutePath();
    LuaServiceApi older;
    synchronized (EVENT_OWNERS) {
      older = EVENT_OWNERS.put(key, this);
    }
    if (older != null && older != this) {
      older.events.clear(); // the same extension started again: only the new copy keeps running
      older.stopEventTimer();
      older.closeIfIdle();
    }
    events.put(name, f);
    startEventTimer();
  }

  private synchronized void startEventTimer() {
    if (timerFuture != null) {
      return;
    }
    int interval = 1000;
    try {
      interval = jxCompatPrefs().getInt("api.setTimerInterval", 1000);
    } catch (Throwable ignored) {
    }
    final long ms = Math.max(200, Math.min(interval, 60000));
    timerErrors = 0;
    timerFuture =
        TASKS.scheduleWithFixedDelay(
            new Runnable() {
              @Override
              public void run() {
                fireTimer();
              }
            },
            ms,
            ms,
            java.util.concurrent.TimeUnit.MILLISECONDS);
  }

  private synchronized void stopEventTimer() {
    if (timerFuture != null) {
      timerFuture.cancel(false);
      timerFuture = null;
    }
  }

  private void fireTimer() {
    com.luajava.LuaFunction f = events.get("onTimer");
    if (f == null || closed) {
      stopEventTimer();
      return;
    }
    if (Boolean.FALSE.equals(timerFlag)) {
      return;
    }
    try {
      synchronized (L) {
        if (closed) {
          return;
        }
        f.call();
      }
      timerErrors = 0;
    } catch (Throwable t) {
      timerErrors++;
      log("Error in onTimer: " + t);
      if (timerErrors >= 20) { // the same error again and again: stop it, never loop forever
        com.google.android.accessibility.talkback.MsErrorLog.log("Lua onTimer stopped after 20 errors", t);
        events.clear();
        stopEventTimer();
        closeIfIdle();
      }
    }
  }

  /** Jieshuo: stops what is being spoken. */
  public void stop() {
    TalkBackService s = TalkBackService.getInstance();
    if (s != null) {
      s.interruptAllFeedback(true);
    }
  }

  /** Jieshuo: all text of the window on screen, one line per element. */
  public String getAllText() {
    AccessibilityNodeInfo root = getRootNodeInfo();
    if (root == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    collectText(root, sb, 0, new int[1]);
    return sb.toString().trim();
  }

  public String getAllText(int ignored) {
    return getAllText();
  }

  private static void collectText(AccessibilityNodeInfo n, StringBuilder sb, int depth, int[] count) {
    if (n == null || depth > 40 || count[0] > 4000 || sb.length() > 200000) {
      return;
    }
    count[0]++;
    if (n.isVisibleToUser()) {
      CharSequence t = n.getText();
      if (t == null || t.length() == 0) {
        t = n.getContentDescription();
      }
      if (t != null && t.length() > 0) {
        if (sb.length() > 0) {
          sb.append('\n');
        }
        sb.append(t);
      }
    }
    for (int i = 0; i < n.getChildCount(); i++) {
      collectText(n.getChild(i), sb, depth + 1, count);
    }
  }

  /** Jieshuo: the element that has the focus (screen reader focus first, then input focus), or nil. */
  public AccessibilityNodeInfo getFocusView() {
    AccessibilityNodeInfo root = getRootNodeInfo();
    if (root == null) {
      return null;
    }
    AccessibilityNodeInfo f = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
    return f != null ? f : root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
  }

  /** Jieshuo: the label of an element: its text, else its description, else the labels of its children. */
  public String getNodeInfoLabel(AccessibilityNodeInfo n) {
    if (n == null) {
      return "";
    }
    String own = getText(n);
    if (own.length() > 0) {
      return own;
    }
    StringBuilder sb = new StringBuilder();
    labelOfChildren(n, sb, 0, new int[1]);
    return sb.toString().trim();
  }

  private void labelOfChildren(AccessibilityNodeInfo n, StringBuilder sb, int depth, int[] count) {
    if (n == null || depth > 3 || count[0] > 30) {
      return;
    }
    for (int i = 0; i < n.getChildCount(); i++) {
      AccessibilityNodeInfo c = n.getChild(i);
      if (c == null) {
        continue;
      }
      count[0]++;
      String t = getText(c);
      if (t.length() > 0) {
        if (sb.length() > 0) {
          sb.append(' ');
        }
        sb.append(t);
      } else {
        labelOfChildren(c, sb, depth + 1, count);
      }
    }
  }

  /**
   * Jieshuo: the first element on screen with this text, or nil. (Returning one element, not a list,
   * lets scripts write {@code if service.findAccessibilityNodeInfoByText("Skip") then}.)
   */
  public AccessibilityNodeInfo findAccessibilityNodeInfoByText(String text) {
    return findAccessibilityNodeInfoByText(text, 0);
  }

  public AccessibilityNodeInfo findAccessibilityNodeInfoByText(String text, int index) {
    AccessibilityNodeInfo root = getRootNodeInfo();
    if (root == null || text == null || text.length() == 0) {
      return null;
    }
    List<AccessibilityNodeInfo> found = root.findAccessibilityNodeInfosByText(text);
    if (found == null || index < 0 || index >= found.size()) {
      return null;
    }
    return found.get(index);
  }

  /** Jieshuo: asks Android to uninstall the app with this package name. */
  public boolean deleteApp(String packageName) {
    if (packageName == null || packageName.trim().length() == 0) {
      return false;
    }
    try {
      Intent i =
          new Intent(Intent.ACTION_DELETE, android.net.Uri.parse("package:" + packageName.trim()));
      i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      ctx.startActivity(i);
      return true;
    } catch (Throwable t) {
      log("deleteApp failed: " + t);
      return false;
    }
  }

  /** Jieshuo: opens the Android installer for the APK file at this path. */
  public boolean installApp(String path) {
    if (path == null || path.trim().length() == 0) {
      return false;
    }
    try {
      File apk = new File(path.trim());
      if (!apk.isFile()) {
        return false;
      }
      android.net.Uri uri =
          androidx.core.content.FileProvider.getUriForFile(
              ctx, ctx.getPackageName() + ".providers.FileProvider", apk);
      Intent i = new Intent(Intent.ACTION_VIEW);
      i.setDataAndType(uri, "application/vnd.android.package-archive");
      i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
      ctx.startActivity(i);
      return true;
    } catch (Throwable t) {
      log("installApp failed: " + t);
      return false;
    }
  }

  // ---------------------------------------------------------------- service.* for scripts

  // AccessibilityService methods exposed directly to Lua scripts for Jieshuo-style compatibility.
  // These forward to the live TalkBack service when available and fail safely otherwise.
  public boolean performGlobalAction(int action) {
    TalkBackService service = TalkBackService.getInstance();
    return service != null && service.performGlobalAction(action);
  }

  public AccessibilityNodeInfo getRootInActiveWindow() {
    TalkBackService service = TalkBackService.getInstance();
    return service == null ? null : service.getRootInActiveWindow();
  }

  public List<android.view.accessibility.AccessibilityWindowInfo> getWindows() {
    TalkBackService service = TalkBackService.getInstance();
    return service == null ? java.util.Collections.emptyList() : service.getWindows();
  }

  public android.accessibilityservice.AccessibilityServiceInfo getServiceInfo() {
    TalkBackService service = TalkBackService.getInstance();
    return service == null ? null : service.getServiceInfo();
  }

  public void setServiceInfo(android.accessibilityservice.AccessibilityServiceInfo info) {
    TalkBackService service = TalkBackService.getInstance();
    if (service != null && info != null) service.setServiceInfo(info);
  }

  public void startActivity(Intent intent) {
    if (intent == null) return;
    Intent safe = new Intent(intent);
    safe.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    try { ctx.startActivity(safe); } catch (Throwable t) { log("startActivity: " + t); }
  }

  public void startActivity(String action) {
    if (action == null || action.trim().isEmpty()) return;
    try { startActivity(new Intent(action)); } catch (Throwable t) { log("startActivity: " + t); }
  }

  public Context getApplicationContext() { return ctx.getApplicationContext(); }
  public String getPackageName() { return ctx.getPackageName(); }
  public PackageManager getPackageManager() { return ctx.getPackageManager(); }
  public android.content.res.Resources getResources() { return ctx.getResources(); }
  public Object getSystemService(String name) { return name == null ? null : ctx.getSystemService(name); }
  public android.content.ContentResolver getContentResolver() { return ctx.getContentResolver(); }

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

  // ---- Step 16a: the last Jieshuo functions of the official API list (JX_API_REFERENCE_zh.txt)

  public boolean toNextChar() {
    return execute("command_to_next_char");
  }

  public boolean toPreviousChar() {
    return execute("command_to_previous_char");
  }

  public boolean toNextLine() {
    return execute("command_to_next_row");
  }

  public boolean toPreviousLine() {
    return execute("command_to_previous_row");
  }

  /** In a text box: cursor to the beginning. Elsewhere: first visible item of the list. */
  public boolean toTextStart() {
    return textEdge(false);
  }

  /** In a text box: cursor to the end. Elsewhere: last visible item of the list. */
  public boolean toTextEnd() {
    return textEdge(true);
  }

  public boolean toListViewStart() {
    return listEdge(false);
  }

  public boolean toListViewEnd() {
    return listEdge(true);
  }

  private AccessibilityNodeInfo focusedOrInputNode() {
    TalkBackService s = TalkBackService.getInstance();
    AccessibilityNodeInfo root = s == null ? null : s.getRootInActiveWindow();
    if (root == null) {
      return null;
    }
    AccessibilityNodeInfo f = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
    return f != null ? f : root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
  }

  private boolean textEdge(boolean end) {
    AccessibilityNodeInfo f = focusedOrInputNode();
    if (f == null) {
      return false;
    }
    if (f.isEditable()) {
      CharSequence t = f.getText();
      int pos = end && t != null ? t.length() : 0;
      android.os.Bundle sel = new android.os.Bundle();
      sel.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, pos);
      sel.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, pos);
      return f.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, sel);
    }
    return listEdge(end);
  }

  /** Focus goes to the first (or last) visible child of the nearest scrollable parent. */
  private boolean listEdge(boolean end) {
    AccessibilityNodeInfo list = focusedOrInputNode();
    int guard = 0;
    while (list != null && guard++ < 25 && !(list.isScrollable() && list.getChildCount() > 0)) {
      list = list.getParent();
    }
    if (list == null) {
      return false;
    }
    int n = list.getChildCount();
    for (int k = 0; k < n; k++) {
      AccessibilityNodeInfo c = list.getChild(end ? n - 1 - k : k);
      if (c != null && c.isVisibleToUser()) {
        return c.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
      }
    }
    return false;
  }

  /**
   * service.ocr(node): text of that node's area read on the phone. Off the main thread it waits
   * (up to 12 s) and returns the text; on the main thread it cannot wait, so it speaks the result
   * and returns null. Needs Android 11+.
   */
  public String ocr(AccessibilityNodeInfo node) {
    TalkBackService s = TalkBackService.getInstance();
    if (s == null || node == null) {
      return null;
    }
    android.graphics.Rect r = new android.graphics.Rect();
    node.getBoundsInScreen(r);
    if (r.isEmpty()) {
      return null;
    }
    final boolean onMain = Looper.myLooper() == Looper.getMainLooper();
    final String[] out = new String[1];
    final java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
    com.google.android.accessibility.talkback.translate.OcrTool.recognize(
        s,
        r,
        (text, message) -> {
          out[0] = text;
          if (onMain) {
            speak(text != null ? text : message);
          }
          done.countDown();
        });
    if (onMain) {
      return null;
    }
    try {
      done.await(12, java.util.concurrent.TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return out[0];
  }

  public void setTimerMode(boolean on) {
    // Step 48: scripts (my, sinema, ded and time) use this as the on/off switch of regEvent("onTimer").
    // It no longer starts the stopwatch and says "Timer started"; the stopwatch stays on its own commands.
    timerFlag = on;
  }

  /** Reading mode: true starts reading from the top, false stops. */
  public void setAutoReadEnabled(boolean on) {
    TalkBackService s = TalkBackService.getInstance();
    if (s != null) {
      if (on) {
        s.msStartReading(true);
      } else {
        s.msStopReading();
      }
    }
  }

  /** Automatic browsing: true starts reading from the focus, false stops. */
  public void setAutoNext(boolean on) {
    TalkBackService s = TalkBackService.getInstance();
    if (s != null) {
      if (on) {
        s.msStartReading(false);
      } else {
        s.msStopReading();
      }
    }
  }

  /** Gesture packages (.gpk) do not exist in this app yet, so this only says so. */
  public boolean loadGesturePackage(String name) {
    log("loadGesturePackage: gesture packages are not supported yet (" + name + ")");
    return false;
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
        MsGestureTiming.longClickMs(ctx));
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
    if (tapNode(n, MsGestureTiming.longClickMs(ctx))) {
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

  /** Jieshuo: speaks without interrupting the main queue. Here: queued speech on the main voice. */
  public void asyncSpeak(String text) {
    TalkBackService s = TalkBackService.getInstance();
    if (s != null && text != null) {
      s.msSpeakMain(text, true);
    }
  }

  /** Jieshuo: the edit box that is focused, or null. */
  public AccessibilityNodeInfo getEditText() {
    return inputNode();
  }

  /** Jieshuo: inserts text at the cursor of an edit box (replaces the selection). */
  public boolean insertText(AccessibilityNodeInfo node, String text) {
    if (node == null || text == null) {
      return false;
    }
    CharSequence old = node.getText();
    String cur = old == null ? "" : old.toString();
    if (node.isShowingHintText()) {
      cur = "";
    }
    int a = Math.max(0, node.getTextSelectionStart());
    int b = Math.max(0, node.getTextSelectionEnd());
    int from = Math.min(Math.min(a, b), cur.length());
    int to = Math.min(Math.max(a, b), cur.length());
    String merged = cur.substring(0, from) + text + cur.substring(to);
    Bundle args = new Bundle();
    args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, merged);
    if (!node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
      return false;
    }
    Bundle sel = new Bundle();
    int pos = from + text.length();
    sel.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, pos);
    sel.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, pos);
    node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, sel);
    return true;
  }

  /** Opens a web address using Android's default handler. */
  public void openUrl(String url) {
    if (url == null || url.trim().isEmpty()) return;
    try {
      String value = url.trim();
      if (!value.matches("(?i)^[a-z][a-z0-9+.-]*:.*")) value = "https://" + value;
      Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(value));
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      ctx.startActivity(intent);
    } catch (RuntimeException e) {
      log("openUrl failed: " + e.getMessage());
    }
  }

  /** Jieshuo-compatible sound slots backed by TalkBack's configurable sound theme. */
  public void playSoundClock() {
    com.google.android.accessibility.talkback.soundtheme.ExtraEventSounds.fire("clock");
  }

  /** Compatibility overload; the legacy argument has no mapped TalkBack setting. */
  public void playSoundClock(int ignored) { playSoundClock(); }

  public void playSoundScroll() {
    com.google.android.accessibility.talkback.soundtheme.ExtraEventSounds.fire("scroll");
  }

  /** Compatibility overload; the legacy argument has no mapped TalkBack setting. */
  public void playSoundScroll(float ignored) { playSoundScroll(); }

  public void playSoundGestureBegin() {
    com.google.android.accessibility.talkback.soundtheme.ExtraEventSounds.fire("gesture_begin");
  }

  public void playSoundGestureEnd() {
    com.google.android.accessibility.talkback.soundtheme.ExtraEventSounds.fire("gesture_end");
  }

  public void playSoundActionItem() {
    com.google.android.accessibility.talkback.soundtheme.ExtraEventSounds.fire("action_item");
  }

  /** Jieshuo: short tick sound on the accessibility audio stream. */
  public void playSoundTick() {
    com.google.android.accessibility.talkback.ExtraSounds.play(
        ctx, com.google.android.accessibility.talkback.R.raw.jx_tick);
  }

  /** Compatibility overload; the legacy argument has no mapped TalkBack setting. */
  public void playSoundTick(int ignored) { playSoundTick(); }

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

  // ---------------------------------------------------------------- Jieshuo setter compatibility (Step 36)
  // Settings with an equivalent MobinSR feature are mapped below. Other values are retained
  // separately so imported scripts can call the API without crashing; retaining a value does
  // NOT claim that an unsupported Jieshuo feature has been implemented.
  private android.content.SharedPreferences jxCompatPrefs() {
    return ctx.getSharedPreferences("jx_compat_settings", Context.MODE_PRIVATE);
  }

  private void rememberCompatSetting(String name, Object value) {
    if (name == null) return;
    android.content.SharedPreferences p = jxCompatPrefs();
    android.content.SharedPreferences.Editor e = p.edit();
    String key = "api." + name;
    if (value == null) {
      e.remove(key);
    } else if (value instanceof Boolean) {
      e.putBoolean(key, (Boolean) value);
    } else if (value instanceof Integer) {
      e.putInt(key, (Integer) value);
    } else if (value instanceof Long) {
      e.putLong(key, (Long) value);
    } else if (value instanceof Float) {
      e.putFloat(key, (Float) value);
    } else if (value instanceof Double) {
      e.putFloat(key, ((Double) value).floatValue());
    } else if (value instanceof String) {
      e.putString(key, (String) value);
    } else {
      e.putString(key, String.valueOf(value));
    }
    e.apply();

    // Apply only settings with an existing MobinSR behavior. Do not silently treat a stored
    // compatibility placeholder as an implemented feature.
    if ("setDoubleFlingTime".equals(name) && value instanceof Integer) {
      // Multi-part gestures recognition time: the settings list stores it as a string (ms).
      com.google.android.accessibility.utils.SharedPreferencesUtils.getSharedPreferences(ctx)
          .edit().putString(MsMultiPartGestures.PREF_TIME, String.valueOf(value)).apply();
      return;
    }
    if (!(value instanceof Boolean)) {
      return;
    }
    boolean enabled = (Boolean) value;
    android.content.SharedPreferences featurePrefs =
        com.google.android.accessibility.utils.SharedPreferencesUtils.getSharedPreferences(ctx);
    if ("setUseOCR".equals(name)) {
      featurePrefs.edit().putBoolean(MsExtras.K_AUTO_OCR, enabled).apply();
    } else if ("setUseEdgeGesture".equals(name)) {
      featurePrefs.edit().putBoolean(MsExtras.K_NO_EDGE, !enabled).apply();
    } else if ("setUseMultiTouchMode".equals(name)) {
      featurePrefs.edit().putBoolean(MsExtras.K_NO_MULTI, !enabled).apply();
    } else if ("setUseAccessibilityVolume".equals(name)
        || "setAsyncUseAccessibilityVolume".equals(name)) {
      featurePrefs.edit().putBoolean("ms_async_tts_accessibility_volume", enabled).apply();
    } else if ("setUseLog".equals(name)) {
      featurePrefs.edit().putBoolean(MsExtras.K_LOG, enabled).apply();
    } else if ("setAutoTrans".equals(name)) {
      featurePrefs.edit().putBoolean(MsExtras.K_AUTO_TRANS, enabled).apply();
    } else if ("setUseHotkey".equals(name)) {
      // MobinSR stores the inverse: true means hotkeys are disabled.
      featurePrefs.edit().putBoolean("ms_cmd_disable_hotkey", !enabled).apply();
    } else if ("setIncallMode".equals(name)) {
      MsExtras.setEarpieceMode(ctx, enabled);
    } else if ("setUseFingerprintGesture".equals(name)) {
      featurePrefs.edit().putBoolean("ms_jx_use_fingerprint_gesture", enabled).apply();
    } else if ("setUseFloatMenu".equals(name)) {
      featurePrefs.edit().putBoolean("ms_jx_use_float_menu", enabled).apply();
    }
  }

  /** Returns the stored compatibility value, or null if no value was recorded. */
  public Object getCompatibilitySetting(String name) {
    if (name == null) return null;
    return jxCompatPrefs().getAll().get("api." + name);
  }

  public void setAcquireWakeLock(boolean arg0) { rememberCompatSetting("setAcquireWakeLock", arg0); }
  public void setAlarmEndHour(java.lang.Integer arg0) { rememberCompatSetting("setAlarmEndHour", arg0); }
  public void setAlarmInterval(int arg0) { rememberCompatSetting("setAlarmInterval", arg0); }
  public void setAlarmStartHour(java.lang.Integer arg0) { rememberCompatSetting("setAlarmStartHour", arg0); }
  public void setAsyncAudioFocus(boolean arg0) { rememberCompatSetting("setAsyncAudioFocus", arg0); }
  public void setAsyncSpeakEnabled(boolean arg0) { rememberCompatSetting("setAsyncSpeakEnabled", arg0); }
  public void setAsyncTTSSpeed(int arg0) { rememberCompatSetting("setAsyncTTSSpeed", arg0); }
  public void setAsyncTTSVolume(float arg0) { rememberCompatSetting("setAsyncTTSVolume", arg0); }
  public void setAsyncTtsEngine() { rememberCompatSetting("setAsyncTtsEngine", Boolean.TRUE); }
  public void setAsyncTtsPitch(int arg0) { rememberCompatSetting("setAsyncTtsPitch", arg0); }
  public void setAsyncTtsScale(float arg0) { rememberCompatSetting("setAsyncTtsScale", arg0); }
  public void setAsyncUseAccessibilityVolume(boolean arg0) { rememberCompatSetting("setAsyncUseAccessibilityVolume", arg0); }
  public void setAsyncUseProixmitySensor(boolean arg0) { rememberCompatSetting("setAsyncUseProixmitySensor", arg0); }
  public void setAsyncUseTouchStop(boolean arg0) { rememberCompatSetting("setAsyncUseTouchStop", arg0); }
  public void setAsyncUseWakeLock(boolean arg0) { rememberCompatSetting("setAsyncUseWakeLock", arg0); }
  public void setAudioFocus(boolean arg0) { rememberCompatSetting("setAudioFocus", arg0); }
  public void setAutoPrevious(boolean arg0) { rememberCompatSetting("setAutoPrevious", arg0); }
  public void setAutoRead(boolean arg0) { rememberCompatSetting("setAutoRead", arg0); }
  public void setAutoShowVirtualScreen() { rememberCompatSetting("setAutoShowVirtualScreen", Boolean.TRUE); }
  public void setAutoSpeech(boolean arg0) { rememberCompatSetting("setAutoSpeech", arg0); }
  public void setAutoSpeechEnabled(boolean arg0) { rememberCompatSetting("setAutoSpeechEnabled", arg0); }
  public void setAutoTrans(boolean arg0) { rememberCompatSetting("setAutoTrans", arg0); }
  public void setCharMode(boolean arg0) { rememberCompatSetting("setCharMode", arg0); }
  public void setCharModeGesture(boolean arg0) { rememberCompatSetting("setCharModeGesture", arg0); }
  public void setClearNode(boolean arg0) { rememberCompatSetting("setClearNode", arg0); }
  public void setCloudNodeInfoLabel(java.lang.String arg0, java.lang.String arg1, java.lang.String arg2) { rememberCompatSetting("setCloudNodeInfoLabel", java.util.Arrays.asList(arg0, arg1, arg2)); }
  public void setContentBlacklist(java.lang.String arg0) { rememberCompatSetting("setContentBlacklist", arg0); }
  public void setDimmed(boolean arg0) { rememberCompatSetting("setDimmed", arg0); }
  public void setDimmedEnabled(boolean arg0) { rememberCompatSetting("setDimmedEnabled", arg0); }
  public void setDoubleFling(boolean arg0) { rememberCompatSetting("setDoubleFling", arg0); }
  public void setDoubleFlingTime(int arg0) { rememberCompatSetting("setDoubleFlingTime", arg0); }
  public void setDoubleTapUserIntentTimeout(int arg0) { rememberCompatSetting("setDoubleTapUserIntentTimeout", arg0); }
  public void setEdgeGesture(boolean arg0) { rememberCompatSetting("setEdgeGesture", arg0); }
  public void setEdgeGestureWidth(java.lang.Integer arg0) { rememberCompatSetting("setEdgeGestureWidth", arg0); }
  public void setEnabled(boolean arg0) { rememberCompatSetting("setEnabled", arg0); }
  public void setFalseTouchPrevention(boolean arg0) { rememberCompatSetting("setFalseTouchPrevention", arg0); }
  public void setFalseTouchPreventionEnabled(boolean arg0) { rememberCompatSetting("setFalseTouchPreventionEnabled", arg0); }
  public void setForeground2(boolean arg0) { rememberCompatSetting("setForeground2", arg0); }
  public void setGameMode(boolean arg0) { rememberCompatSetting("setGameMode", arg0); }
  public void setInEdgeGesture(boolean arg0) { rememberCompatSetting("setInEdgeGesture", arg0); }
  public void setIncallMode() {
    rememberCompatSetting("setIncallMode", Boolean.TRUE);
    MsExtras.setEarpieceMode(ctx, true);
  }
  public void setIncallMode(boolean arg0) {
    rememberCompatSetting("setIncallMode", arg0);
    MsExtras.setEarpieceMode(ctx, arg0);
  }
  public void setInputMode(boolean arg0) { rememberCompatSetting("setInputMode", arg0); }
  public void setKeepFocusPosition(boolean arg0) { rememberCompatSetting("setKeepFocusPosition", arg0); }
  public void setLeftButton(java.lang.String arg0) { rememberCompatSetting("setLeftButton", arg0); }
  public void setLongClickTime(int arg0) { rememberCompatSetting("setLongClickTime", arg0); }
  public void setLongCopy(boolean arg0) { rememberCompatSetting("setLongCopy", arg0); }
  public void setMenuUpTap(boolean arg0) { rememberCompatSetting("setMenuUpTap", arg0); }
  public void setModeAudio(int arg0) { rememberCompatSetting("setModeAudio", arg0); }
  public void setMultiFingerGesturesEnabled(boolean arg0) { rememberCompatSetting("setMultiFingerGesturesEnabled", arg0); }
  public void setNaviUpTap(boolean arg0) { rememberCompatSetting("setNaviUpTap", arg0); }
  public void setNavigationGestureWidth(int arg0) { rememberCompatSetting("setNavigationGestureWidth", arg0); }
  public void setNoTouch(boolean arg0) { rememberCompatSetting("setNoTouch", arg0); }
  public void setNotificationAnnouncementSpeak(boolean arg0) { rememberCompatSetting("setNotificationAnnouncementSpeak", arg0); }
  public void setNotificationAppNameSpeak(boolean arg0) { rememberCompatSetting("setNotificationAppNameSpeak", arg0); }
  public void setNotificationBarSpeak(boolean arg0) { rememberCompatSetting("setNotificationBarSpeak", arg0); }
  public void setNotificationBlackList(java.lang.String arg0) { rememberCompatSetting("setNotificationBlackList", arg0); }
  public void setNotificationInAutoReadNoSpeak(boolean arg0) { rememberCompatSetting("setNotificationInAutoReadNoSpeak", arg0); }
  public void setNotificationInCallNoSpeak(boolean arg0) { rememberCompatSetting("setNotificationInCallNoSpeak", arg0); }
  public void setNotificationInTouchNoSpeak(boolean arg0) { rememberCompatSetting("setNotificationInTouchNoSpeak", arg0); }
  public void setNotificationSpeak(boolean arg0) { rememberCompatSetting("setNotificationSpeak", arg0); }
  public void setNotificationSummary(boolean arg0) { rememberCompatSetting("setNotificationSummary", arg0); }
  public void setNotificationToastSpeak(boolean arg0) { rememberCompatSetting("setNotificationToastSpeak", arg0); }
  public void setNotificationWhiteList(java.lang.String arg0) { rememberCompatSetting("setNotificationWhiteList", arg0); }
  public void setOnlyNotification(boolean arg0) { rememberCompatSetting("setOnlyNotification", arg0); }
  public void setPartialWakeLock(boolean arg0) { rememberCompatSetting("setPartialWakeLock", arg0); }
  public void setPauseGestureDetection(boolean arg0) { rememberCompatSetting("setPauseGestureDetection", arg0); }
  public void setProfiles() { rememberCompatSetting("setProfiles", Boolean.TRUE); }
  public void setProfiles(java.lang.String arg0) { rememberCompatSetting("setProfiles", arg0); }
  public void setRawSpeak(boolean arg0) { rememberCompatSetting("setRawSpeak", arg0); }
  public void setRightButton(java.lang.String arg0) { rememberCompatSetting("setRightButton", arg0); }
  public void setScreenOffSpeak(boolean arg0) { rememberCompatSetting("setScreenOffSpeak", arg0); }
  public void setSelectionMode(boolean arg0) { rememberCompatSetting("setSelectionMode", arg0); }
  public void setSensor(boolean arg0) { rememberCompatSetting("setSensor", arg0); }
  public void setServiceHandlesDoubleTap(boolean arg0) { rememberCompatSetting("setServiceHandlesDoubleTap", arg0); }
  public void setSingleTap(boolean arg0) { rememberCompatSetting("setSingleTap", arg0); }
  public void setSound(boolean arg0) { rememberCompatSetting("setSound", arg0); }
  public void setSoundVolume(float arg0) { rememberCompatSetting("setSoundVolume", arg0); }
  public void setSpeakListViewItem(boolean arg0) { rememberCompatSetting("setSpeakListViewItem", arg0); }
  public void setSpeaking(boolean arg0) { rememberCompatSetting("setSpeaking", arg0); }
  public void setSpeeching(boolean arg0) { rememberCompatSetting("setSpeeching", arg0); }
  public void setStreamVolume(int arg0) { rememberCompatSetting("setStreamVolume", arg0); }
  public void setSupperMode(boolean arg0) { rememberCompatSetting("setSupperMode", arg0); }
  public void setSupperModeAux(boolean arg0) { rememberCompatSetting("setSupperModeAux", arg0); }
  public void setTTSEnabled(boolean arg0) { rememberCompatSetting("setTTSEnabled", arg0); }
  public void setTTSSpeaker(java.lang.String arg0) { rememberCompatSetting("setTTSSpeaker", arg0); }
  public void setTTSSpeed(boolean arg0) { rememberCompatSetting("setTTSSpeed", arg0); }
  public void setTTSSpeed(int arg0) { rememberCompatSetting("setTTSSpeed", arg0); }
  public void setTTSVolume(float arg0) { rememberCompatSetting("setTTSVolume", arg0); }
  public void setTapMode(boolean arg0) { rememberCompatSetting("setTapMode", arg0); }
  public void setTimerInterval(int arg0) { rememberCompatSetting("setTimerInterval", arg0); }
  public void setTimerModeAux(boolean arg0) { rememberCompatSetting("setTimerModeAux", arg0); }
  public void setTimerModePause(boolean arg0) { rememberCompatSetting("setTimerModePause", arg0); }
  public void setTouchFocusUserIntentTimeout(int arg0) { rememberCompatSetting("setTouchFocusUserIntentTimeout", arg0); }
  public void setTouchMode(boolean arg0) { rememberCompatSetting("setTouchMode", arg0); }
  public void setTtsEngine() { rememberCompatSetting("setTtsEngine", Boolean.TRUE); }
  public void setTtsEngine(java.lang.String arg0) { rememberCompatSetting("setTtsEngine", arg0); }
  public void setTtsPitch(int arg0) { rememberCompatSetting("setTtsPitch", arg0); }
  public void setTtsScale(float arg0) { rememberCompatSetting("setTtsScale", arg0); }
  public void setTypingFocusUserIntentTimeout(int arg0) { rememberCompatSetting("setTypingFocusUserIntentTimeout", arg0); }
  public void setUnknownIndex(boolean arg0) { rememberCompatSetting("setUnknownIndex", arg0); }
  public void setUnknownLabel(boolean arg0) { rememberCompatSetting("setUnknownLabel", arg0); }
  public void setUpTap(boolean arg0) { rememberCompatSetting("setUpTap", arg0); }
  public void setUpTapEnabled(boolean arg0) { rememberCompatSetting("setUpTapEnabled", arg0); }
  public void setUsageHints(boolean arg0) { rememberCompatSetting("setUsageHints", arg0); }
  public void setUseAccessibilityVolume(boolean arg0) { rememberCompatSetting("setUseAccessibilityVolume", arg0); }
  public void setUseAccessibilityVolumeFix(boolean arg0) { rememberCompatSetting("setUseAccessibilityVolumeFix", arg0); }
  public void setUseAlarm(boolean arg0) { rememberCompatSetting("setUseAlarm", arg0); }
  public void setUseAppTTS(boolean arg0) { rememberCompatSetting("setUseAppTTS", arg0); }
  public void setUseAutoFocus(boolean arg0) { rememberCompatSetting("setUseAutoFocus", arg0); }
  public void setUseAutoViewType(boolean arg0) { rememberCompatSetting("setUseAutoViewType", arg0); }
  public void setUseBottomMenuEdgeGesture(boolean arg0) { rememberCompatSetting("setUseBottomMenuEdgeGesture", arg0); }
  public void setUseCloudLabel(boolean arg0) { rememberCompatSetting("setUseCloudLabel", arg0); }
  public void setUseContentChanged(boolean arg0) { rememberCompatSetting("setUseContentChanged", arg0); }
  public void setUseContentTreeChanged(boolean arg0) { rememberCompatSetting("setUseContentTreeChanged", arg0); }
  public void setUseCustomDictRegex(boolean arg0) { rememberCompatSetting("setUseCustomDictRegex", arg0); }
  public void setUseCustomDictSpeak(boolean arg0) { rememberCompatSetting("setUseCustomDictSpeak", arg0); }
  public void setUseCustomNodeName(boolean arg0) { rememberCompatSetting("setUseCustomNodeName", arg0); }
  public void setUseCustomSpeakClear(boolean arg0) { rememberCompatSetting("setUseCustomSpeakClear", arg0); }
  public void setUseCustomSpeakSymbolNumber(boolean arg0) { rememberCompatSetting("setUseCustomSpeakSymbolNumber", arg0); }
  public void setUseCustomSymbolSpeak(boolean arg0) { rememberCompatSetting("setUseCustomSymbolSpeak", arg0); }
  public void setUseDpadKey(boolean arg0) { rememberCompatSetting("setUseDpadKey", arg0); }
  public void setUseEdgeGesture(boolean arg0) { rememberCompatSetting("setUseEdgeGesture", arg0); }
  public void setUseEditHint(boolean arg0) { rememberCompatSetting("setUseEditHint", arg0); }
  public void setUseEffectPro(boolean arg0) { rememberCompatSetting("setUseEffectPro", arg0); }
  public void setUseEmojiSpeak(boolean arg0) { rememberCompatSetting("setUseEmojiSpeak", arg0); }
  public void setUseEmojiSpeakClear(boolean arg0) { rememberCompatSetting("setUseEmojiSpeakClear", arg0); }
  public void setUseFastMode(boolean arg0) { rememberCompatSetting("setUseFastMode", arg0); }
  public void setUseFingerprintGesture(boolean arg0) { rememberCompatSetting("setUseFingerprintGesture", arg0); }
  public void setUseFloatMenu(boolean arg0) { rememberCompatSetting("setUseFloatMenu", arg0); }
  public void setUseForeground(boolean arg0) { rememberCompatSetting("setUseForeground", arg0); }
  public void setUseForegroundPro(boolean arg0) { rememberCompatSetting("setUseForegroundPro", arg0); }
  public void setUseGameModeNoEdge(boolean arg0) { rememberCompatSetting("setUseGameModeNoEdge", arg0); }
  public void setUseGameModeNoSpeak(boolean arg0) { rememberCompatSetting("setUseGameModeNoSpeak", arg0); }
  public void setUseGestureDetection(boolean arg0) { rememberCompatSetting("setUseGestureDetection", arg0); }
  public void setUseHeadSetKey(boolean arg0) { rememberCompatSetting("setUseHeadSetKey", arg0); }
  public void setUseHotkey(boolean arg0) { rememberCompatSetting("setUseHotkey", arg0); }
  public void setUseImageFilter(boolean arg0) { rememberCompatSetting("setUseImageFilter", arg0); }
  public void setUseImageToText(boolean arg0) { rememberCompatSetting("setUseImageToText", arg0); }
  public void setUseInputMethodDoubleTap(boolean arg0) { rememberCompatSetting("setUseInputMethodDoubleTap", arg0); }
  public void setUseInputMethodGesture(boolean arg0) { rememberCompatSetting("setUseInputMethodGesture", arg0); }
  public void setUseInputMethodNoGesture(boolean arg0) { rememberCompatSetting("setUseInputMethodNoGesture", arg0); }
  public void setUseInputMethodNoTouch(boolean arg0) { rememberCompatSetting("setUseInputMethodNoTouch", arg0); }
  public void setUseInputMethodSpeakCandidate(boolean arg0) { rememberCompatSetting("setUseInputMethodSpeakCandidate", arg0); }
  public void setUseInputMethodSpeakCommit(boolean arg0) { rememberCompatSetting("setUseInputMethodSpeakCommit", arg0); }
  public void setUseInputMethodSpeakCommitSpace(boolean arg0) { rememberCompatSetting("setUseInputMethodSpeakCommitSpace", arg0); }
  public void setUseInputMethodSpeakKey(boolean arg0) { rememberCompatSetting("setUseInputMethodSpeakKey", arg0); }
  public void setUseInputMethodSpeakPassword(boolean arg0) { rememberCompatSetting("setUseInputMethodSpeakPassword", arg0); }
  public void setUseInputMethodSpeakWord(boolean arg0) { rememberCompatSetting("setUseInputMethodSpeakWord", arg0); }
  public void setUseKeepType(boolean arg0) { rememberCompatSetting("setUseKeepType", arg0); }
  public void setUseLeftRight(boolean arg0) { rememberCompatSetting("setUseLeftRight", arg0); }
  public void setUseListIndex(boolean arg0) { rememberCompatSetting("setUseListIndex", arg0); }
  public void setUseListItemIndex(boolean arg0) { rememberCompatSetting("setUseListItemIndex", arg0); }
  public void setUseListItemIndexRealTime(boolean arg0) { rememberCompatSetting("setUseListItemIndexRealTime", arg0); }
  public void setUseLog(boolean arg0) { rememberCompatSetting("setUseLog", arg0); }
  public void setUseLongClickVoiceImage(boolean arg0) { rememberCompatSetting("setUseLongClickVoiceImage", arg0); }
  public void setUseLongClickVoiceInput(boolean arg0) { rememberCompatSetting("setUseLongClickVoiceInput", arg0); }
  public void setUseLoopMove(boolean arg0) { rememberCompatSetting("setUseLoopMove", arg0); }
  public void setUseLoopMoveSpace(boolean arg0) { rememberCompatSetting("setUseLoopMoveSpace", arg0); }
  public void setUseMoreNode(boolean arg0) { rememberCompatSetting("setUseMoreNode", arg0); }
  public void setUseMoveListViewItem(boolean arg0) { rememberCompatSetting("setUseMoveListViewItem", arg0); }
  public void setUseMoveWindow(boolean arg0) { rememberCompatSetting("setUseMoveWindow", arg0); }
  public void setUseMultiTouchMode(boolean arg0) { rememberCompatSetting("setUseMultiTouchMode", arg0); }
  public void setUseNavigationGesture(boolean arg0) { rememberCompatSetting("setUseNavigationGesture", arg0); }
  public void setUseNewTextMoveType(boolean arg0) { rememberCompatSetting("setUseNewTextMoveType", arg0); }
  public void setUseNewWebMove(boolean arg0) { rememberCompatSetting("setUseNewWebMove", arg0); }
  public void setUseNoBluetooth(boolean arg0) { rememberCompatSetting("setUseNoBluetooth", arg0); }
  public void setUseNode(boolean arg0) { rememberCompatSetting("setUseNode", arg0); }
  public void setUseNodeInfo(boolean arg0) { rememberCompatSetting("setUseNodeInfo", arg0); }
  public void setUseOCR(boolean arg0) { rememberCompatSetting("setUseOCR", arg0); }
  public void setUseOfflineTTS(boolean arg0) { rememberCompatSetting("setUseOfflineTTS", arg0); }
  public void setUseOldPrevious(boolean arg0) { rememberCompatSetting("setUseOldPrevious", arg0); }
  public void setUsePagerScroll(boolean arg0) { rememberCompatSetting("setUsePagerScroll", arg0); }
  public void setUsePinyinSpeak(boolean arg0) { rememberCompatSetting("setUsePinyinSpeak", arg0); }
  public void setUsePowerSave(boolean arg0) { rememberCompatSetting("setUsePowerSave", arg0); }
  public void setUseProgress(boolean arg0) { rememberCompatSetting("setUseProgress", arg0); }
  public void setUseProixmitySensor(boolean arg0) { rememberCompatSetting("setUseProixmitySensor", arg0); }
  public void setUseProximityFalseTouchPrevention(boolean arg0) { rememberCompatSetting("setUseProximityFalseTouchPrevention", arg0); }
  public void setUseProximityFalseTouchPreventionOnlyLock(boolean arg0) { rememberCompatSetting("setUseProximityFalseTouchPreventionOnlyLock", arg0); }
  public void setUseRawClick(boolean arg0) { rememberCompatSetting("setUseRawClick", arg0); }
  public void setUseRawTap(boolean arg0) { rememberCompatSetting("setUseRawTap", arg0); }
  public void setUseRingtoneVolume(boolean arg0) { rememberCompatSetting("setUseRingtoneVolume", arg0); }
  public void setUseScreenOffSpeak(boolean arg0) { rememberCompatSetting("setUseScreenOffSpeak", arg0); }
  public void setUseScreenOnSpeak(boolean arg0) { rememberCompatSetting("setUseScreenOnSpeak", arg0); }
  public void setUseScreenOnSpeakBattery(boolean arg0) { rememberCompatSetting("setUseScreenOnSpeakBattery", arg0); }
  public void setUseScreenOnSpeakNotification(boolean arg0) { rememberCompatSetting("setUseScreenOnSpeakNotification", arg0); }
  public void setUseScrollListContent(boolean arg0) { rememberCompatSetting("setUseScrollListContent", arg0); }
  public void setUseScrollListIndex(boolean arg0) { rememberCompatSetting("setUseScrollListIndex", arg0); }
  public void setUseScrollListKeepPosition(boolean arg0) { rememberCompatSetting("setUseScrollListKeepPosition", arg0); }
  public void setUseShowDisabledQuickMenu(boolean arg0) { rememberCompatSetting("setUseShowDisabledQuickMenu", arg0); }
  public void setUseShowText(boolean arg0) { rememberCompatSetting("setUseShowText", arg0); }
  public void setUseSingleTTS(boolean arg0) { rememberCompatSetting("setUseSingleTTS", arg0); }
  public void setUseSlowSpeed(boolean arg0) { rememberCompatSetting("setUseSlowSpeed", arg0); }
  public void setUseSmallFilter(boolean arg0) { rememberCompatSetting("setUseSmallFilter", arg0); }
  public void setUseSpeakKey(boolean arg0) { rememberCompatSetting("setUseSpeakKey", arg0); }
  public void setUseSpeakQqChanged(boolean arg0) { rememberCompatSetting("setUseSpeakQqChanged", arg0); }
  public void setUseSpeakState(boolean arg0) { rememberCompatSetting("setUseSpeakState", arg0); }
  public void setUseSpeakWindowAllChanged(boolean arg0) { rememberCompatSetting("setUseSpeakWindowAllChanged", arg0); }
  public void setUseSupperFilter(boolean arg0) { rememberCompatSetting("setUseSupperFilter", arg0); }
  public void setUseTextFormatCharNumber(boolean arg0) { rememberCompatSetting("setUseTextFormatCharNumber", arg0); }
  public void setUseTextFormatCharUpper(boolean arg0) { rememberCompatSetting("setUseTextFormatCharUpper", arg0); }
  public void setUseTextFormatFirst(boolean arg0) { rememberCompatSetting("setUseTextFormatFirst", arg0); }
  public void setUseTextFormatReadWord(boolean arg0) { rememberCompatSetting("setUseTextFormatReadWord", arg0); }
  public void setUseTouchMode(boolean arg0) { rememberCompatSetting("setUseTouchMode", arg0); }
  public void setUseTouchStop(boolean arg0) { rememberCompatSetting("setUseTouchStop", arg0); }
  public void setUseUpLongClick(boolean arg0) { rememberCompatSetting("setUseUpLongClick", arg0); }
  public void setUseUpLongQuickMenu(boolean arg0) { rememberCompatSetting("setUseUpLongQuickMenu", arg0); }
  public void setUseViewMove(boolean arg0) { rememberCompatSetting("setUseViewMove", arg0); }
  public void setUseViewName(boolean arg0) { rememberCompatSetting("setUseViewName", arg0); }
  public void setUseViewTypeMoveMode(boolean arg0) { rememberCompatSetting("setUseViewTypeMoveMode", arg0); }
  public void setUseVolumeKeyEnabled(boolean arg0) { rememberCompatSetting("setUseVolumeKeyEnabled", arg0); }
  public void setUseVolumeKeyMoveText(boolean arg0) { rememberCompatSetting("setUseVolumeKeyMoveText", arg0); }
  public void setUseWakeLock(boolean arg0) { rememberCompatSetting("setUseWakeLock", arg0); }
  public void setUseWakeLockPro(boolean arg0) { rememberCompatSetting("setUseWakeLockPro", arg0); }
  public void setUseWebMove(boolean arg0) { rememberCompatSetting("setUseWebMove", arg0); }
  public void setUseWebViewName(boolean arg0) { rememberCompatSetting("setUseWebViewName", arg0); }
  public void setUseWindowTitleChanged(boolean arg0) { rememberCompatSetting("setUseWindowTitleChanged", arg0); }
  public void setVibrate(boolean arg0) { rememberCompatSetting("setVibrate", arg0); }
  public void setVibrateIntensity(int arg0) { rememberCompatSetting("setVibrateIntensity", arg0); }
  public void setVoiceHelperMediaMuteMode(boolean arg0) { rememberCompatSetting("setVoiceHelperMediaMuteMode", arg0); }
  public void setVoiceInputAppendMode(boolean arg0) { rememberCompatSetting("setVoiceInputAppendMode", arg0); }
  public void setVolumeKeyEnabled(boolean arg0) { rememberCompatSetting("setVolumeKeyEnabled", arg0); }
  public void setWakeLock(boolean arg0) { rememberCompatSetting("setWakeLock", arg0); }
  public void setWakeLock(long arg0) { rememberCompatSetting("setWakeLock", arg0); }
  public void setWakeLock_aux(boolean arg0) { rememberCompatSetting("setWakeLock_aux", arg0); }
  public void setup(java.lang.String arg0) { rememberCompatSetting("setup", arg0); }

  // ---------------------------------------------------------------- LuaContext

  @Override public ArrayList<ClassLoader> getClassLoaders() { return loaders; }
  @Override public void call(String func, Object... args) {
    if (func == null || func.isEmpty()) return;
    synchronized (L) {
      if (closed) return;
      int top = L.getTop();
      try {
        L.getGlobal(func);
        if (!L.isFunction(-1)) return;
        if (args != null) {
          for (Object arg : args) L.pushObjectValue(arg);
        }
        int rc = L.pcall(args == null ? 0 : args.length, 0, 0);
        if (rc != 0) log("Lua callback " + func + ": " + L.toString(-1));
      } catch (Throwable t) {
        log("Lua callback " + func + ": " + t);
      } finally {
        L.setTop(top);
      }
    }
  }
  @Override public void set(String name, Object value) {
    if (name == null || name.isEmpty()) return;
    synchronized (L) {
      if (closed) return;
      try {
        L.pushObjectValue(value);
        L.setGlobal(name);
      } catch (Exception ignored) {
      }
    }
  }
  @Override public String getLuaPath() { return scriptDir.getAbsolutePath(); }
  @Override public String getLuaPath(String path) { return new File(scriptDir, path).getAbsolutePath(); }
  @Override public String getLuaPath(String dir, String name) { return new File(new File(scriptDir, dir), name).getAbsolutePath(); }
  @Override public String getLuaDir() { return scriptDir.getAbsolutePath(); }
  @Override public String getLuaDir(String dir) { return new File(scriptDir, dir).getAbsolutePath(); }
  @Override public String getLuaExtDir() { return luaExtDir; }
  @Override public String getLuaExtDir(String dir) { return LuaPaths.alias(ctx, dir).getAbsolutePath(); }
  @Override public void setLuaExtDir(String dir) { luaExtDir = dir; }
  @Override public String getLuaExtPath(String path) {
    if (path == null || path.isEmpty()) return luaExtDir;
    String normalized = path.replace('\\', '/');
    int slash = normalized.indexOf('/');
    String first = slash < 0 ? normalized : normalized.substring(0, slash);
    File base = LuaPaths.alias(ctx, first);
    return (slash < 0 ? base : new File(base, normalized.substring(slash + 1))).getAbsolutePath();
  }
  @Override public String getLuaExtPath(String dir, String name) { return new File(LuaPaths.alias(ctx, dir), name == null ? "" : name).getAbsolutePath(); }
  @Override public String getLuaLpath() {
    // Same path as the main state, so a task/thread/timer state can require "import" and the other bundled modules.
    return (luaLibDir != null ? luaLibDir.getAbsolutePath() + "/?.lua;" : "") + scriptDir.getAbsolutePath() + "/?.lua";
  }
  @Override public String getLuaCpath() { return ctx.getApplicationInfo().nativeLibraryDir + "/lib?.so"; }
  @Override public Context getContext() { return ctx; }
  @Override public LuaState getLuaState() { return L; }
  @Override public Object doFile(String path, Object... arg) {
    String err;
    synchronized (L) {
      if (closed) return null;
      err = runFileWithArgs(new File(path), null, arg);
    }
    if (err != null) log(err);
    return null;
  }
  @Override public void sendMsg(String msg) { log(msg); }
  @Override public void sendError(String title, Exception msg) { log(title + ": " + msg); }
  @Override public int getWidth() { return ctx.getResources().getDisplayMetrics().widthPixels; }
  @Override public int getHeight() { return ctx.getResources().getDisplayMetrics().heightPixels; }
  @Override public Map getGlobalData() { return globalData; }
  // Step 42: service.setSharedData/getSharedData now survive script runs and app restarts.
  // Strings, booleans and numbers go to SharedPreferences "lua_shared_data"; any other object (a Lua table,
  // a Java object) cannot be saved, so it stays in memory for this script run only, as before.
  private static final String SHARED_DATA_PREFS = "lua_shared_data";
  private static final String DOUBLE_MARK = "\u0000d";

  private android.content.SharedPreferences sharedDataPrefs() {
    return ctx.getSharedPreferences(SHARED_DATA_PREFS, Context.MODE_PRIVATE);
  }

  @Override public Object getSharedData(String key) { return getSharedData(key, null); }

  @Override public Object getSharedData(String key, Object def) {
    if (key == null) return def;
    try {
      Object saved = sharedDataPrefs().getAll().get(key);
      if (saved instanceof String && ((String) saved).startsWith(DOUBLE_MARK)) {
        try {
          return Double.valueOf(((String) saved).substring(DOUBLE_MARK.length()));
        } catch (NumberFormatException e) {
          return def;
        }
      }
      if (saved != null) return saved;
    } catch (Throwable t) {
      log("getSharedData: " + t);
    }
    Object v = globalData.get(key);
    return v == null ? def : v;
  }

  @Override public boolean setSharedData(String key, Object value) {
    if (key == null) return false;
    try {
      android.content.SharedPreferences.Editor e = sharedDataPrefs().edit();
      if (value == null) {
        e.remove(key);
        globalData.remove(key);
      } else if (value instanceof String) {
        e.putString(key, (String) value);
        globalData.remove(key);
      } else if (value instanceof Boolean) {
        e.putBoolean(key, (Boolean) value);
        globalData.remove(key);
      } else if (value instanceof Byte || value instanceof Short
          || value instanceof Integer || value instanceof Long) {
        e.putLong(key, ((Number) value).longValue());
        globalData.remove(key);
      } else if (value instanceof Number) {
        double d = ((Number) value).doubleValue();
        if (!Double.isNaN(d) && !Double.isInfinite(d) && d == Math.rint(d) && Math.abs(d) < 9.0e15) {
          e.putLong(key, (long) d);
        } else {
          e.putString(key, DOUBLE_MARK + Double.toString(d));
        }
        globalData.remove(key);
      } else {
        e.remove(key);
        globalData.put(key, value);
      }
      e.apply();
      return true;
    } catch (Throwable t) {
      log("setSharedData: " + t);
      globalData.put(key, value);
      return false;
    }
  }
  @Override public void regGc(LuaGcable obj) {
    if (obj == null) return;
    boolean reject;
    synchronized (gcables) {
      reject = closed;
      if (!reject) gcables.add(obj);
    }
    // Never leave a newly-created task holding a Lua state that has already closed.
    if (reject) {
      try { obj.gc(); } catch (Throwable ignored) { }
    }
  }

  @Override public void unregGc(LuaGcable obj) {
    if (obj == null) return;
    synchronized (gcables) { gcables.remove(obj); }
    closeIfIdle();
  }
}
