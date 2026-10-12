package com.google.android.accessibility.talkback.editor;

import android.content.Context;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MS Screen Reader: runs Lua extensions (folders Extensions/NAME/main.lua) from the main menu and
 * from events.
 *
 * <p>Header lines at the top of main.lua (first 20 lines) control an extension:
 *
 * <pre>
 * -- title: My extension          (name shown in the menu; default = folder name)
 * -- menu: off                    (do not show in the menu)
 * -- event: app_opened, notification
 * -- hook: onNotification, onWindowStateChanged
 * </pre>
 *
 * Hooks (opt-in with the "-- hook:" line): the script is run, then the Lua function with that name
 * is called with (package, text). The top-level code of main.lua runs every time too, so guard it
 * with {@code if service_hook == nil then ... end}. Names: onWindowStateChanged, onNotification,
 * onScrolled, onViewLongClicked, onKeyEvent, onGestureDetected, onVolumeChanged. Key and gesture hooks run synchronously; returning true consumes the event. Their arguments and the "return true = consumed" rule of Jieshuo
 * are NOT confirmed (Jieshuo's real signatures cannot be read from its obfuscated code).
 *
 * Events: window_changed, app_opened, notification, service_started. When an extension runs from
 * an event, the Lua globals event_type, event_package and event_text are set. When it runs from the
 * menu, the global node is the item that has screen reader focus.
 */
public final class LuaExtensionManager {

  /** Menu item ids for extensions start here (far away from resource ids). */
  public static final int MENU_ID_BASE = 0x4D530000;

  public static final int MENU_ID_MAX = MENU_ID_BASE + 500;

  private static final long MIN_GAP_MS = 1500L;

  /** One extension found on disk. */
  public static final class Extension {
    public final String folder;
    public final String title;
    public final boolean inMenu;
    public final Set<String> events;
    public final Set<String> hooks;
    public final File main;

    Extension(
        String folder, String title, boolean inMenu, Set<String> events, Set<String> hooks, File main) {
      this.folder = folder;
      this.title = title;
      this.inMenu = inMenu;
      this.events = events;
      this.hooks = hooks;
      this.main = main;
    }
  }

  private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
  private static final Map<String, Long> lastRun = new HashMap<>();
  private static List<Extension> menuList = new ArrayList<>();
  private static List<Extension> cachedList = new ArrayList<>();
  private static long cachedAt = 0L;
  private static String lastPackage = "";

  private LuaExtensionManager() {}

  /** Same as list() but remembered for 3 seconds, for frequent events. Call on the worker thread. */
  private static synchronized List<Extension> cached(Context context) {
    long now = System.currentTimeMillis();
    if (now - cachedAt > 3000L) {
      cachedList = list(context);
      cachedAt = now;
    }
    return cachedList;
  }

  public static boolean isMenuId(int id) {
    return id >= MENU_ID_BASE && id < MENU_ID_MAX;
  }

  /** Reads the Extensions folder. Cheap: only the top lines of each main.lua are read. */
  public static List<Extension> list(Context context) {
    List<Extension> result = new ArrayList<>();
    File[] dirs;
    try {
      dirs = LuaPaths.extensions(context).listFiles();
    } catch (Exception e) {
      return result;
    }
    if (dirs == null) {
      return result;
    }
    java.util.Arrays.sort(dirs);
    for (File dir : dirs) {
      File main = new File(dir, "main.lua");
      if (!dir.isDirectory() || !main.isFile()) {
        continue;
      }
      String title = dir.getName();
      boolean inMenu = false; // only extensions with "-- menu: on" appear in the screen reader menu
      Set<String> events = new HashSet<>();
      Set<String> hooks = new HashSet<>();
      try (BufferedReader r = new BufferedReader(new FileReader(main))) {
        String line;
        int n = 0;
        while ((line = r.readLine()) != null && n++ < 20) {
          line = line.trim();
          if (!line.startsWith("--")) {
            continue;
          }
          String body = line.substring(2).trim();
          int colon = body.indexOf(':');
          if (colon < 0) {
            continue;
          }
          String key = body.substring(0, colon).trim().toLowerCase(Locale.ROOT);
          String value = body.substring(colon + 1).trim();
          if (key.equals("title") && !value.isEmpty()) {
            title = value;
          } else if (key.equals("menu")) {
            inMenu = !value.equalsIgnoreCase("off") && !value.equalsIgnoreCase("no");
          } else if (key.equals("hook")) {
            for (String h : value.split("[,\\s]+")) {
              if (!h.isEmpty()) {
                hooks.add(h);
              }
            }
          } else if (key.equals("event")) {
            for (String e : value.split("[,\\s]+")) {
              if (!e.isEmpty()) {
                events.add(e.toLowerCase(Locale.ROOT));
              }
            }
          }
        }
      } catch (Exception ignored) {
        // Unreadable header: still list the extension with defaults.
      }
      result.add(new Extension(dir.getName(), title, inMenu, events, hooks, main));
    }
    return result;
  }

  /**
   * Returns the extensions to show in the menu, in order. The position in this list is used to
   * build menu item ids: id = MENU_ID_BASE + position.
   */
  public static List<Extension> prepareMenuList(Context context) {
    List<Extension> shown = new ArrayList<>();
    for (Extension e : list(context)) {
      if (e.inMenu) {
        shown.add(e);
      }
    }
    menuList = shown;
    return shown;
  }

  /** Runs the extension behind a menu item id, with the focused item as global "node". */
  public static void runFromMenu(Context context, int menuId, AccessibilityNodeInfo focused) {
    int index = menuId - MENU_ID_BASE;
    if (index < 0 || index >= menuList.size()) {
      return;
    }
    final Extension ext = menuList.get(index);
    final Context app = context.getApplicationContext();
    final AccessibilityNodeInfo node = focused == null ? null : AccessibilityNodeInfo.obtain(focused);
    WORKER.execute(
        () -> {
          String err = LuaServiceApi.run(app, ext.main, node, null);
          if (err != null) {
            LuaServiceApi.speakStatic("Extension " + ext.title + ": " + err);
          }
        });
  }

  // ---------------------------------------------------------------------------------------------
  // Events

  /** Called for every accessibility event from the service. Must stay cheap. */
  public static void onAccessibilityEvent(Context context, AccessibilityEvent event) {
    int type = event.getEventType();
    String pkg = event.getPackageName() == null ? "" : event.getPackageName().toString();
    if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
      String text = describe(event);
      dispatch(context, "window_changed", pkg, text);
      if (!pkg.isEmpty() && !pkg.equals(lastPackage)) {
        lastPackage = pkg;
        dispatch(context, "app_opened", pkg, text);
      }
    } else if (type == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
      dispatch(context, "notification", pkg, describe(event));
    }
    String hook = null;
    if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
      hook = "onWindowStateChanged";
    } else if (type == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
      hook = "onNotification";
    } else if (type == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
      hook = "onScrolled";
    } else if (type == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED) {
      hook = "onViewLongClicked";
    }
    if (hook != null) {
      dispatchHook(context, hook, pkg, describe(event));
    }
  }

  /** Synchronous opt-in key hook. Returning true from Lua consumes the key event. */
  public static boolean handleKeyEvent(Context context, android.view.KeyEvent event) {
    if (event == null || context == null) return false;
    List<Extension> all;
    try { all = cached(context.getApplicationContext()); } catch (Throwable t) { return false; }
    for (Extension ext : all) {
      if (!ext.hooks.contains("onKeyEvent")) continue;
      try {
        if (LuaServiceApi.runHookBoolean(context, ext.main, "onKeyEvent", event)) return true;
      } catch (Throwable ignored) { }
    }
    return false;
  }

  /** Dispatches an opt-in volume hook with (streamType, current, previous, percent). */
  public static void onVolumeChanged(Context context, int type, int value, int previous, int percent) {
    if (context == null) return;
    List<Extension> all;
    try { all = cached(context.getApplicationContext()); } catch (Throwable t) { return; }
    for (Extension ext : all) {
      if (!ext.hooks.contains("onVolumeChanged")) continue;
      WORKER.execute(() -> {
        try { LuaServiceApi.runHookBoolean(context, ext.main, "onVolumeChanged", type, value, previous, percent); }
        catch (Throwable ignored) { }
      });
    }
  }

  /** Synchronous opt-in gesture hook. Returning true from Lua consumes the gesture. */
  public static boolean handleGesture(Context context, int gestureId) {
    if (context == null) return false;
    List<Extension> all;
    try { all = cached(context.getApplicationContext()); } catch (Throwable t) { return false; }
    for (Extension ext : all) {
      if (!ext.hooks.contains("onGestureDetected")) continue;
      try {
        if (LuaServiceApi.runHookBoolean(context, ext.main, "onGestureDetected", gestureId)) return true;
      } catch (Throwable ignored) { }
    }
    return false;
  }

  private static void dispatchHook(
      Context context, final String hook, final String pkg, final String text) {
    final Context app = context.getApplicationContext();
    final long now = System.currentTimeMillis();
    WORKER.execute(
        () -> {
          List<Extension> all;
          try {
            all = cached(app);
          } catch (Throwable t) {
            return;
          }
          for (Extension ext : all) {
            if (!ext.hooks.contains(hook)) {
              continue;
            }
            String key = ext.folder + "|hook|" + hook;
            synchronized (lastRun) {
              Long last = lastRun.get(key);
              if (last != null && now - last < 500L) {
                continue;
              }
              lastRun.put(key, now);
            }
            String err = LuaServiceApi.runHook(app, ext.main, hook, pkg, text);
            if (err != null) {
              LuaServiceApi.speakStatic("Extension " + ext.title + ": " + err);
            }
          }
        });
  }

  public static void onServiceStarted(Context context) {
    if (!MsExtSettings.autostart(context)) {
      return;
    }
    dispatch(context, "service_started", "", "");
  }

  private static String describe(AccessibilityEvent event) {
    StringBuilder sb = new StringBuilder();
    List<CharSequence> texts = event.getText();
    if (texts != null) {
      for (CharSequence t : texts) {
        if (t != null && t.length() > 0) {
          if (sb.length() > 0) {
            sb.append(' ');
          }
          sb.append(t);
        }
      }
    }
    if (sb.length() == 0 && event.getContentDescription() != null) {
      sb.append(event.getContentDescription());
    }
    return sb.toString();
  }

  private static void dispatch(
      Context context, final String eventType, final String pkg, final String text) {
    final Context app = context.getApplicationContext();
    final long now = System.currentTimeMillis();
    // Listing reads small files, so do it on the worker thread, not on the service thread.
    WORKER.execute(
        () -> {
          List<Extension> all;
          try {
            all = list(app);
          } catch (Throwable t) {
            return;
          }
          for (Extension ext : all) {
            if (!ext.events.contains(eventType)) {
              continue;
            }
            String key = ext.folder + "|" + eventType;
            synchronized (lastRun) {
              Long last = lastRun.get(key);
              if (last != null && now - last < MIN_GAP_MS) {
                continue;
              }
              lastRun.put(key, now);
            }
            Map<String, String> extras = new HashMap<>();
            extras.put("event_type", eventType);
            extras.put("event_package", pkg);
            extras.put("event_text", text);
            String err = LuaServiceApi.run(app, ext.main, null, extras);
            if (err != null) {
              LuaServiceApi.speakStatic("Extension " + ext.title + ": " + err);
            }
          }
        });
  }
}
