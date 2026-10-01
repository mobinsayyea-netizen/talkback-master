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
import com.google.android.accessibility.talkback.TalkBackService;
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

  /** Runs a script. Returns null on success, or an error text. Always call from a worker thread. */
  public static String run(Context ctx, File script, AccessibilityNodeInfo node) {
    if (depth >= MAX_DEPTH) {
      return "Too many nested scripts";
    }
    depth++;
    LuaServiceApi api = null;
    try {
      api = new LuaServiceApi(ctx.getApplicationContext(), script.getParentFile());
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
    try {
      L.close();
    } catch (Throwable ignored) {
    }
  }

  // ---------------------------------------------------------------- service.* for scripts

  /** service.execute("Notification bar") etc. */
  public boolean execute(String name) {
    TalkBackService s = TalkBackService.getInstance();
    if (s == null || name == null) {
      return false;
    }
    switch (name.trim().toLowerCase()) {
      case "speak time and battery level":
        BatteryManager bm = (BatteryManager) ctx.getSystemService(Context.BATTERY_SERVICE);
        int pct = bm == null ? -1 : bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        String time = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date());
        speak(time + (pct >= 0 ? ", battery " + pct + "%" : ""));
        return true;
      case "screen reader settings":
        Intent i = new Intent(s, com.android.talkback.TalkBackPreferencesActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        s.startActivity(i);
        return true;
      case "notification bar":
        return s.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS);
      case "quick settings":
        return s.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS);
      case "take a screenshot":
        return s.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT);
      case "back":
        return s.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
      case "home":
        return s.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
      case "recents":
        return s.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
      case "copy":
        return focusedAction(s, AccessibilityNodeInfo.ACTION_COPY);
      case "paste":
        return focusedAction(s, AccessibilityNodeInfo.ACTION_PASTE);
      default:
        return false;
    }
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
