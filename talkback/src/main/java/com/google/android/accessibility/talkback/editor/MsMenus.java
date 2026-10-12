/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityNodeInfo;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Step 21: separate menus like Jieshuo's. Main menu (a list you can customize, ends with "More"),
 * Multi-column menu (rows and columns, ends with "More"), a page with all functions, the
 * Functions menu (13 fixed entries plus the focused item's own actions), the Recognition menu and
 * the floating menu strip. Every entry runs an existing command, extension, tool or app. Not
 * compiled yet.
 */
public final class MsMenus {
  private MsMenus() {}

  public static final String K_MAIN = "ms_jx_main_menu_items";
  public static final String K_MULTI = "ms_jx_menu_items";
  public static final String K_FLOAT = "ms_jx_float_menu_items";
  static final int COLUMNS = 3;
  private static final long AFTER_CLOSE_MS = 600;
  private static final Handler MAIN = new Handler(Looper.getMainLooper());

  /** One menu entry. kind: cmd (command name), ext, tool, app (package), kbd, act (action id), more. */
  public static final class Item {
    public final String label;
    public final String kind;
    public final String name;

    public Item(String label, String kind, String name) {
      this.label = label;
      this.kind = kind;
      this.name = name;
    }
  }

  // ---- entries added to TalkBack's own grouped Functions menu (see TalkbackMenuProcessor)
  public static final int[] FN_IDS = {
    R.id.ms_fn_clear,
    R.id.ms_fn_select,
    R.id.ms_fn_paste,
    R.id.ms_fn_clipboard,
    R.id.ms_fn_favorites,
    R.id.ms_fn_granular,
    R.id.ms_fn_list_copy,
    R.id.ms_fn_keyboard,
  };
  public static final String[] FN_LABELS = {
    "Clear", "Select", "Paste", "Clipboard history", "Favorites", "Granular editing mode",
    "List copying", "Open keyboard"
  };
  private static final String[] FN_KINDS = {
    "cmd", "cmd", "cmd", "cmd", "cmd", "cmd", "cmd", "kbd"
  };
  private static final String[] FN_NAMES = {
    "Clear", "Full text selection", "Paste", "Clipboard history", "Favorites",
    "Granular editing mode", "List copying", "kbd"
  };

  public static boolean isFunctionId(int itemId) {
    for (int id : FN_IDS) {
      if (id == itemId) {
        return true;
      }
    }
    return false;
  }

  /** Called when one of the 8 extra entries of the grouped menu is clicked. */
  public static void runFunctionId(final Context c, int itemId) {
    for (int i = 0; i < FN_IDS.length; i++) {
      if (FN_IDS[i] == itemId) {
        final int k = i;
        MAIN.postDelayed(
            () -> runNow(c.getApplicationContext(), FN_KINDS[k], FN_NAMES[k], FN_LABELS[k]),
            AFTER_CLOSE_MS);
        return;
      }
    }
  }

  static SharedPreferences prefs(Context c) {
    return SharedPreferencesUtils.getSharedPreferences(c);
  }

  // ---------------------------------------------------------------- stored lists

  public static List<Item> load(Context c, String key) {
    String s = prefs(c).getString(key, null);
    List<Item> out = new ArrayList<>();
    if (s != null && !s.trim().isEmpty()) {
      for (String line : s.split("\n")) {
        String[] p = line.split("\t", -1);
        if (p.length == 3 && !p[0].isEmpty() && !p[2].isEmpty()) {
          out.add(new Item(p[0], p[1], p[2]));
        }
      }
    }
    return out.isEmpty() ? defaults(key) : out;
  }

  public static void save(Context c, String key, List<Item> items) {
    StringBuilder b = new StringBuilder();
    for (Item i : items) {
      b.append(i.label.replace('\t', ' ').replace('\n', ' '))
          .append('\t')
          .append(i.kind)
          .append('\t')
          .append(i.name.replace('\t', ' ').replace('\n', ' '))
          .append('\n');
    }
    prefs(c).edit().putString(key, b.toString()).apply();
  }

  public static void resetToDefault(Context c, String key) {
    prefs(c).edit().remove(key).apply();
  }

  private static void cmd(List<Item> l, String name) {
    if (JxCommands.idOf(name) != null) {
      l.add(new Item(name, "cmd", name));
    }
  }

  static List<Item> defaults(String key) {
    List<Item> l = new ArrayList<>();
    if (K_FLOAT.equals(key)) {
      cmd(l, "Main menu");
    } else if (K_MAIN.equals(key)) {
      for (String n :
          new String[] {
            "Granular browsing mode", "List browsing", "Read the whole screen", "Voice assistant",
            "Screen reader settings", "Extensions", "Suspend speech feedback and browse by touch"
          }) {
        cmd(l, n);
      }
    } else {
      for (String n :
          new String[] {
            "Functions menu", "Recognition menu", "Current per-app gestures menu",
            "Screen reader settings", "Granular browsing mode", "List browsing",
            "Read the whole screen", "Reading mode", "Voice assistant", "Voice dictation",
            "Describe currently focused item", "Describe entire screen",
            "Recognize text on the current screen",
            "Recognize text of the current focused element", "Translation", "Clipboard history",
            "Favorites", "Notification box", "Recent applications", "Home screen", "Go back",
            "Quick settings", "Take a screenshot", "Power menu",
            "Suspend speech feedback and browse by touch", "Suspend browse by touch",
            "Enable curtain", "Screen lock"
          }) {
        cmd(l, n);
      }
    }
    return l;
  }

  // ---------------------------------------------------------------- showing the menus

  private static void launch(
      Context c, String title, List<Item> items, int cols) {
    String[] labels = new String[items.size()];
    String[] kinds = new String[items.size()];
    String[] names = new String[items.size()];
    for (int i = 0; i < labels.length; i++) {
      labels[i] = items.get(i).label;
      kinds[i] = items.get(i).kind;
      names[i] = items.get(i).name;
    }
    Intent in = new Intent(c, MsMenuActivity.class);
    in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    in.putExtra("title", title);
    in.putExtra("labels", labels);
    in.putExtra("kinds", kinds);
    in.putExtra("names", names);
    in.putExtra("cols", cols);
    c.startActivity(in);
  }

  /** Main menu: your list, then "More" (multi-column menu). With "use multi-column menu": that menu. */
  public static boolean showMain(TalkBackService s) {
    if (s == null) {
      return false;
    }
    if (prefs(s).getBoolean("ms_jx_use_multi_menu", false)) {
      return showMulti(s);
    }
    List<Item> items = new ArrayList<>(load(s, K_MAIN));
    items.add(new Item("More", "more", "multi"));
    launch(s, "Main menu", items, 0);
    return true;
  }

  public static boolean showMulti(TalkBackService s) {
    if (s == null) {
      return false;
    }
    List<Item> items = new ArrayList<>(load(s, K_MULTI));
    items.add(new Item("More", "more", "all"));
    launch(s, "Main menu", items, COLUMNS);
    return true;
  }

  /** Third page: every function. */
  public static boolean showAll(TalkBackService s) {
    if (s == null) {
      return false;
    }
    List<Item> items = new ArrayList<>();
    for (String[] row : JxCommands.TABLE) {
      if (row.length > 1 && row[1] != null && !row[1].isEmpty()) {
        items.add(new Item(row[1], "cmd", row[1]));
      }
    }
    launch(s, "Main menu", items, COLUMNS);
    return true;
  }

  public static boolean showRecognition(TalkBackService s) {
    if (s == null) {
      return false;
    }
    List<Item> items = new ArrayList<>();
    for (String n :
        new String[] {
          "Translation", "Recognize text on the current screen",
          "Recognize text of the current focused element", "Describe currently focused item",
          "Describe entire screen", "Detailed description of entire screen",
          "Recognize the focused icon", "Identify form", "Get CAPTCHA", "Recognize subtitles",
          "Virtual screen", "Inquire by voice about current focus",
          "Inquire by Voice About the Entire Screen", "Automatic translation"
        }) {
      cmd(items, n);
    }
    launch(s, "Recognition menu", items, 0);
    return true;
  }

  /** Functions menu: the 13 entries of Jieshuo, then the actions of the focused item (if switched on). */
  public static boolean showFunctions(TalkBackService s, AccessibilityNodeInfo focus) {
    if (s == null) {
      return false;
    }
    List<Item> items = new ArrayList<>();
    cmd(items, "Clear");
    items.add(new Item("Select", "cmd", "Full text selection"));
    cmd(items, "Copy");
    cmd(items, "Append");
    cmd(items, "Paste");
    cmd(items, "Clipboard history");
    cmd(items, "Favorites");
    cmd(items, "Granular editing mode");
    cmd(items, "List copying");
    items.add(new Item("Open keyboard", "kbd", "kbd"));
    cmd(items, "Recognition menu");
    addFocusActions(s, focus != null ? focus : findFocus(s), items);
    launch(s, "Functions menu", items, 0);
    return true;
  }

  /**
   * "Show actions in the functions menu" lists the focused item's own named actions (for example
   * Uninstall, Open chat). "Show additional dynamic focus related options" adds the plain ones
   * (Click, Long click, Expand, Collapse, Dismiss).
   */
  private static void addFocusActions(Context c, AccessibilityNodeInfo n, List<Item> out) {
    if (n == null) {
      return;
    }
    SharedPreferences p = prefs(c);
    boolean named = p.getBoolean("ms_jx_use_action_menu_show_cursor", false);
    boolean plain = p.getBoolean("ms_jx_use_action_menu_show_ext", false);
    if (!named && !plain) {
      return;
    }
    List<AccessibilityNodeInfo.AccessibilityAction> actions = n.getActionList();
    if (actions == null) {
      return;
    }
    for (AccessibilityNodeInfo.AccessibilityAction a : actions) {
      CharSequence label = a.getLabel();
      int id = a.getId();
      if (label != null && label.length() > 0) {
        if (named) {
          out.add(new Item(label.toString(), "act", String.valueOf(id)));
        }
      } else if (plain) {
        String t = plainName(id);
        if (t != null) {
          out.add(new Item(t, "act", String.valueOf(id)));
        }
      }
    }
  }

  private static String plainName(int id) {
    if (id == AccessibilityNodeInfo.ACTION_CLICK) {
      return "Click";
    } else if (id == AccessibilityNodeInfo.ACTION_LONG_CLICK) {
      return "Long click";
    } else if (id == AccessibilityNodeInfo.ACTION_EXPAND) {
      return "Expand";
    } else if (id == AccessibilityNodeInfo.ACTION_COLLAPSE) {
      return "Collapse";
    } else if (id == AccessibilityNodeInfo.ACTION_DISMISS) {
      return "Dismiss";
    }
    return null;
  }

  private static AccessibilityNodeInfo findFocus(AccessibilityService s) {
    AccessibilityNodeInfo root = s.getRootInActiveWindow();
    if (root == null) {
      return null;
    }
    AccessibilityNodeInfo f = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
    return f != null ? f : root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
  }

  // ---------------------------------------------------------------- running one entry

  /** Used by the menu window: waits until the window has closed so the focus is back. */
  static void runAfterClose(final Context c, final String kind, final String name, final String label) {
    MAIN.postDelayed(() -> runNow(c, kind, name, label), AFTER_CLOSE_MS);
  }

  public static void runNow(Context c, String kind, String name, String label) {
    TalkBackService s = TalkBackService.getInstance();
    try {
      if ("more".equals(kind)) {
        if ("multi".equals(name)) {
          showMulti(s);
        } else {
          showAll(s);
        }
      } else if ("kbd".equals(kind)) {
        openKeyboard(s);
      } else if ("act".equals(kind)) {
        performAction(s, Integer.parseInt(name));
      } else if ("app".equals(kind)) {
        Intent in = c.getPackageManager().getLaunchIntentForPackage(name);
        if (in == null) {
          LuaServiceApi.speakStatic("Application not found");
        } else {
          in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          c.startActivity(in);
        }
      } else {
        CustomVoiceCommands.run(c, new CustomVoiceCommands.Entry(label, kind, name));
      }
    } catch (Throwable t) {
      LuaServiceApi.speakStatic("Could not run " + label);
    }
  }

  private static void performAction(TalkBackService s, int id) {
    AccessibilityNodeInfo f = s == null ? null : findFocus(s);
    if (f == null) {
      LuaServiceApi.speakStatic("Nothing is focused");
      return;
    }
    f.performAction(id);
  }

  /** Open keyboard: focuses and taps the text box so the keyboard shows. */
  static boolean openKeyboard(TalkBackService s) {
    if (s == null) {
      return false;
    }
    AccessibilityNodeInfo root = s.getRootInActiveWindow();
    AccessibilityNodeInfo f =
        root == null ? null : root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
    if (f == null && root != null) {
      f = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
    }
    if (f == null || !f.isEditable()) {
      LuaServiceApi.speakStatic("No text box");
      return false;
    }
    f.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
    f.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    try {
      s.getSoftKeyboardController()
          .setShowMode(AccessibilityService.SHOW_MODE_AUTO);
    } catch (Throwable t) {
      // older Android: the tap above is enough
    }
    return true;
  }

  /** Clear: empties the text box that has the focus (Jieshuo's Clear). */
  static boolean clearText(LuaServiceApi api, TalkBackService s, AccessibilityNodeInfo n) {
    AccessibilityNodeInfo t = n;
    if ((t == null || !t.isEditable()) && s != null) {
      AccessibilityNodeInfo root = s.getRootInActiveWindow();
      t = root == null ? null : root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
    }
    if (t == null || !t.isEditable()) {
      LuaServiceApi.speakStatic("No text box");
      return false;
    }
    Bundle b = new Bundle();
    b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "");
    boolean ok = t.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b);
    LuaServiceApi.speakStatic(ok ? "Cleared" : "Could not clear");
    return ok;
  }
}
