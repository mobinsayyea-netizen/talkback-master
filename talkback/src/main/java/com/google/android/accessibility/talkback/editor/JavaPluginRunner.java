package com.google.android.accessibility.talkback.editor;

import android.content.Context;
import android.view.accessibility.AccessibilityNodeInfo;
import dalvik.system.DexClassLoader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import ms.api.MsPlugin;

/**
 * Runs AI-made Java extensions and tools. A Java plugin is a folder with the usual main.lua stub
 * (only header lines), the source Main.java and the compiled plugin.jar.
 */
final class JavaPluginRunner {
  private JavaPluginRunner() {}

  private static final Map<String, Object[]> CACHE = new HashMap<>(); // path -> {stamp, MsPlugin}

  static File jarOf(File script) {
    File dir = script == null ? null : script.getParentFile();
    return dir == null ? null : new File(dir, "plugin.jar");
  }

  static boolean isJava(File script) {
    File jar = jarOf(script);
    return jar != null && jar.isFile();
  }

  private static String className(File script) {
    try (BufferedReader r = new BufferedReader(new FileReader(script))) {
      String line;
      int n = 0;
      while ((line = r.readLine()) != null && n++ < 20) {
        line = line.trim();
        if (line.startsWith("--") && line.substring(2).trim().toLowerCase().startsWith("java:")) {
          String v = line.substring(line.indexOf(':') + 1).trim();
          if (!v.isEmpty()) return v;
        }
      }
    } catch (Exception ignored) {
    }
    return "Main";
  }

  private static synchronized MsPlugin load(Context ctx, File script) throws Exception {
    File jar = jarOf(script);
    String key = jar.getAbsolutePath();
    String stamp = jar.lastModified() + ":" + jar.length();
    Object[] hit = CACHE.get(key);
    if (hit != null && stamp.equals(hit[0])) {
      return (MsPlugin) hit[1];
    }
    jar.setReadOnly(); // Android 14+ refuses to load writable code files
    File opt = new File(ctx.getCodeCacheDir(), "ms_plugin_" + Integer.toHexString(key.hashCode()));
    opt.mkdirs();
    DexClassLoader loader =
        new DexClassLoader(jar.getAbsolutePath(), opt.getAbsolutePath(), null, JavaPluginRunner.class.getClassLoader());
    Class<?> c = loader.loadClass(className(script));
    MsPlugin p = (MsPlugin) c.getDeclaredConstructor().newInstance();
    CACHE.put(key, new Object[] {stamp, p});
    return p;
  }

  /** Returns null on success or an error text. Worker thread only. */
  static String run(Context ctx, File script, AccessibilityNodeInfo node, Map<String, String> extras) {
    File dir = script.getParentFile();
    MsApiImpl api = null;
    try {
      MsPlugin p = load(ctx.getApplicationContext(), script);
      api = new MsApiImpl(ctx, dir, node);
      if (extras != null && extras.containsKey("event_type")) {
        p.onEvent(api, extras.get("event_type"), extras.get("event_package"), extras.get("event_text"));
      } else {
        p.run(api, api.focusedText());
      }
      return null;
    } catch (Throwable t) {
      return failed(dir, t);
    } finally {
      if (api != null) api.finish();
    }
  }

  /** Hook that returns nothing (notification, window, scroll, long click). Null = ok. */
  static String runHook(Context ctx, File script, String hook, String pkg, String text) {
    File dir = script.getParentFile();
    MsApiImpl api = null;
    try {
      MsPlugin p = load(ctx.getApplicationContext(), script);
      api = new MsApiImpl(ctx, dir, null);
      if ("onWindowStateChanged".equals(hook)) {
        p.onWindowStateChanged(api, pkg, text);
      } else if ("onNotification".equals(hook)) {
        p.onNotification(api, pkg, text);
      } else if ("onScrolled".equals(hook)) {
        p.onScrolled(api, pkg, text);
      } else if ("onViewLongClicked".equals(hook)) {
        p.onViewLongClicked(api, pkg, text);
      }
      return null;
    } catch (Throwable t) {
      return failed(dir, t);
    } finally {
      if (api != null) api.finish();
    }
  }

  /** Hook that may consume the event: key, gesture; volume always returns false. */
  static boolean runHookBoolean(Context ctx, File script, String hook, Object... args) {
    File dir = script.getParentFile();
    MsApiImpl api = null;
    try {
      MsPlugin p = load(ctx.getApplicationContext(), script);
      api = new MsApiImpl(ctx, dir, null);
      if ("onKeyEvent".equals(hook) && args != null && args.length > 0) {
        return p.onKeyEvent(api, (android.view.KeyEvent) args[0]);
      } else if ("onGestureDetected".equals(hook) && args != null && args.length > 0) {
        return p.onGestureDetected(api, ((Number) args[0]).intValue());
      } else if ("onVolumeChanged".equals(hook) && args != null && args.length > 3) {
        p.onVolumeChanged(
            api,
            ((Number) args[0]).intValue(),
            ((Number) args[1]).intValue(),
            ((Number) args[2]).intValue(),
            ((Number) args[3]).intValue());
      }
      return false;
    } catch (Throwable t) {
      failed(dir, t);
      return false;
    } finally {
      if (api != null) api.finish();
    }
  }

  private static String failed(File dir, Throwable t) {
    Throwable root =
        t instanceof java.lang.reflect.InvocationTargetException && t.getCause() != null
            ? t.getCause()
            : t;
    com.google.android.accessibility.talkback.MsErrorLog.log("Java plugin " + dir.getName(), root);
    try (FileWriter w = new FileWriter(new File(dir, "last_error.txt"))) {
      w.write(android.util.Log.getStackTraceString(root));
    } catch (Exception ignored) {
    }
    return "Error: " + root;
  }
}
