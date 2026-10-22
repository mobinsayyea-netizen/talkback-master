/*
 * Copyright 2026 Advanced Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.translate.OcrTool;
import com.google.android.accessibility.talkback.translate.TranslateEngine;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Step 14: the last 30 execute() commands, decided by the assistant (see STEP14_NOTES.md for each
 * decision), plus the event hooks they need (notification and chat store, lift to activate,
 * detailed focus, automatic OCR / translation, logging, gesture blocking). Not compiled yet.
 */
public final class MsExtras {
  private MsExtras() {}

  private static final Handler MAIN = new Handler(Looper.getMainLooper());
  private static final int MAX_ENTRIES = 200;
  private static final int TOUCH_EXPLORATION_GESTURE_END = 0x00000400;

  // saved flags (all in the normal TalkBack preferences)
  static final String K_LOG = "ms_cmd_logging";
  static final String K_NO_EDGE = "ms_cmd_no_edge";
  static final String K_NO_MULTI = "ms_cmd_no_multi";
  static final String K_AUTO_TRANS = "ms_cmd_auto_trans";
  static final String K_AUTO_OCR = "ms_cmd_auto_ocr";
  static final String K_AUTO_CHAT = "ms_cmd_auto_chat";
  static final String K_CHAT_HISTORY_OFF = "ms_cmd_chat_history_off";
  static final String K_DETAIL = "ms_cmd_detail_focus";
  static final String K_LIFT = "ms_cmd_lift_activate";
  static final String K_QUICK = "ms_cmd_quick_mode";
  static final String K_EARPIECE = "ms_cmd_earpiece";
  static final String K_LEFT = "ms_left_shortcut"; // Jieshuo function name or command id
  static final String K_RIGHT = "ms_right_shortcut";

  private static final Set<String> CHAT_PKGS =
      new HashSet<>(
          Arrays.asList(
              "com.whatsapp",
              "com.whatsapp.w4b",
              "org.telegram.messenger",
              "org.telegram.messenger.web",
              "org.thunderdog.challegram",
              "com.facebook.orca",
              "com.instagram.android",
              "org.thoughtcrime.securesms",
              "com.google.android.apps.messaging",
              "com.samsung.android.messaging",
              "com.android.mms",
              "com.discord",
              "com.viber.voip",
              "jp.naver.line.android",
              "com.skype.raider",
              "com.google.android.apps.dynamite"));

  // ---------------------------------------------------------------- registration

  static void register(Map<String, JxCommandTable.Handler> m) {
    m.put("command_notification_box", (api, ctx, s, n) -> showHistory(ctx, false));
    m.put("chat_history", (api, ctx, s, n) -> showHistory(ctx, true));
    m.put("command_speak_all_notification", (api, ctx, s, n) -> readAllNotifications(api, ctx));
    m.put("command_auto_list_speak", (api, ctx, s, n) -> flag(api, ctx, K_AUTO_CHAT, false, "Automatic chat reading on", "Automatic chat reading off"));
    m.put("command_auto_list_speak_history", (api, ctx, s, n) -> flag(api, ctx, K_CHAT_HISTORY_OFF, false, "Chat history on", "Chat history off"));

    m.put("command_all_node", (api, ctx, s, n) -> nodeList(api, ctx, s, MODE_ALL, n));
    m.put("command_virtual_screen", (api, ctx, s, n) -> nodeList(api, ctx, s, MODE_CLICK, n));
    m.put("command_virtual_navigation", (api, ctx, s, n) -> nodeList(api, ctx, s, MODE_FOCUS, n));
    m.put("command_grid_mode", (api, ctx, s, n) -> nodeList(api, ctx, s, MODE_GRID, n));
    m.put("list_mode_title", (api, ctx, s, n) -> listBrowse(api, ctx, s, n));
    m.put("split_menu_title", (api, ctx, s, n) -> granularBrowse(api, ctx, n));
    m.put("split_edit_title", (api, ctx, s, n) -> TextViewerActivity.show(ctx, TextViewerActivity.MODE_EDIT, n));

    m.put("command_start_log", (api, ctx, s, n) -> flag(api, ctx, K_LOG, false, "Logging on", "Logging off"));
    m.put("command_disable_edge_gesture_title", (api, ctx, s, n) -> flag(api, ctx, K_NO_EDGE, false, "Corner gestures suspended", "Corner gestures on"));
    m.put("switch_multi_finger_gestures", (api, ctx, s, n) -> flag(api, ctx, K_NO_MULTI, false, "Multi-finger gestures suspended", "Multi-finger gestures on"));
    m.put("command_pause_a11y_volume", (api, ctx, s, n) -> flag(api, ctx, "ms_async_tts_accessibility_volume", false, "Accessibility volume on", "Accessibility volume suspended"));
    m.put("command_incall_mode", (api, ctx, s, n) -> earpiece(api, ctx));
    m.put("command_auto_trans", (api, ctx, s, n) -> flag(api, ctx, K_AUTO_TRANS, false, "Automatic translation on", "Automatic translation off"));
    m.put("use_ocr_title", (api, ctx, s, n) -> flag(api, ctx, K_AUTO_OCR, false, "Automatic OCR on", "Automatic OCR off"));

    m.put("command_supper_mode", (api, ctx, s, n) -> flag(api, ctx, K_DETAIL, false, "Detailed focusing on", "Detailed focusing off"));
    m.put("command_up_tap", (api, ctx, s, n) -> flag(api, ctx, K_LIFT, false, "Lift to activate on", "Lift to activate off"));
    m.put("command_tap_mode", (api, ctx, s, n) -> tapMode(api, ctx));
    m.put("command_quick_mode", (api, ctx, s, n) -> quick(api, ctx, s));
    m.put("command_left_shortcut", (api, ctx, s, n) -> shortcut(api, ctx, s, n, false));
    m.put("command_right_shortcut", (api, ctx, s, n) -> shortcut(api, ctx, s, n, true));

    m.put("profiles_title", (api, ctx, s, n) -> profiles(api, ctx));
    m.put("camera_title", (api, ctx, s, n) -> camera(ctx));
    m.put("command_current_location", (api, ctx, s, n) -> location(api, ctx));
    m.put("command_copy_to_cloud", (api, ctx, s, n) -> cloudCopy(api, ctx, n));
    m.put("command_paste_from_cloud", (api, ctx, s, n) -> cloudPaste(api, ctx, n));
  }

  // ---------------------------------------------------------------- small helpers

  private static SharedPreferences prefs(Context c) {
    return SharedPreferencesUtils.getSharedPreferences(c);
  }

  static boolean on(Context c, String key) {
    return prefs(c).getBoolean(key, false);
  }

  /** Flips a flag. If {@code defaultOn}, the stored value means "off" is false (see callers). */
  private static boolean flag(LuaServiceApi api, Context c, String key, boolean defaultOn, String onText, String offText) {
    SharedPreferences p = prefs(c);
    boolean now = !p.getBoolean(key, defaultOn);
    p.edit().putBoolean(key, now).apply();
    // K_CHAT_HISTORY_OFF stores "history off"; its texts are given as (on-text first), so invert
    boolean saysOn = key.equals(K_CHAT_HISTORY_OFF) ? !now : now;
    if (key.equals("ms_async_tts_accessibility_volume")) {
      saysOn = now;
    }
    api.speak(saysOn ? onText : offText);
    return true;
  }

  private static String textOf(AccessibilityNodeInfo n) {
    if (n == null) {
      return "";
    }
    CharSequence t = n.getText();
    if (t == null || t.length() == 0) {
      t = n.getContentDescription();
    }
    return t == null ? "" : t.toString().trim();
  }

  /** Own text, or the texts of the children joined (a list row is usually many small views). */
  private static String deepText(AccessibilityNodeInfo n, int depth) {
    if (n == null || depth > 4) {
      return "";
    }
    String own = textOf(n);
    if (!own.isEmpty()) {
      return own;
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < n.getChildCount() && i < 20; i++) {
      AccessibilityNodeInfo c = n.getChild(i);
      String t = deepText(c, depth + 1);
      if (!t.isEmpty()) {
        if (sb.length() > 0) {
          sb.append(", ");
        }
        sb.append(t);
      }
    }
    return sb.toString();
  }

  private static String role(AccessibilityNodeInfo n) {
    CharSequence cn = n.getClassName();
    if (cn == null) {
      return "view";
    }
    String s = cn.toString();
    int dot = s.lastIndexOf('.');
    return dot >= 0 ? s.substring(dot + 1) : s;
  }

  private static void clickNode(AccessibilityNodeInfo n) {
    AccessibilityNodeInfo p = n;
    for (int i = 0; p != null && i < 8; i++) {
      p.refresh();
      if (p.isClickable() && p.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
        return;
      }
      p = p.getParent();
    }
    n.refresh();
    n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
  }

  private static void focusNode(AccessibilityNodeInfo n) {
    n.refresh();
    n.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
  }

  private static void copyText(Context c, String text) {
    ClipboardManager cm = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
    if (cm != null) {
      cm.setPrimaryClip(ClipData.newPlainText("Advanced Screen Reader", text));
    }
  }

  // ---------------------------------------------------------------- notification and chat store

  static final class Entry {
    final long time;
    final String pkg;
    final String text;

    Entry(long time, String pkg, String text) {
      this.time = time;
      this.pkg = pkg;
      this.text = text;
    }
  }

  private static final List<Entry> NOTES = new ArrayList<>();
  private static final List<Entry> CHAT = new ArrayList<>();
  private static boolean loaded;

  private static File storeFile(Context c, boolean chat) {
    return new File(LuaPaths.base(c), chat ? "Chat history.txt" : "Notifications.txt");
  }

  private static synchronized void ensureLoaded(Context c) {
    if (loaded) {
      return;
    }
    loaded = true;
    read(storeFile(c, false), NOTES);
    read(storeFile(c, true), CHAT);
  }

  private static void read(File f, List<Entry> out) {
    if (!f.isFile()) {
      return;
    }
    try (BufferedReader r = new BufferedReader(new FileReader(f))) {
      String line;
      while ((line = r.readLine()) != null) {
        String[] p = line.split("\t", 3);
        if (p.length == 3) {
          try {
            out.add(new Entry(Long.parseLong(p[0]), p[1], p[2].replace('\u23CE', '\n')));
          } catch (NumberFormatException ignored) {
            // skip a damaged line
          }
        }
      }
    } catch (IOException e) {
      // start empty
    }
  }

  private static void write(File f, List<Entry> list) {
    try (FileWriter w = new FileWriter(f)) {
      for (Entry e : list) {
        w.write(e.time + "\t" + e.pkg + "\t" + e.text.replace('\n', '\u23CE').replace('\t', ' ') + "\n");
      }
    } catch (IOException e) {
      // best effort
    }
  }

  private static synchronized void add(Context c, boolean chat, String pkg, String text) {
    ensureLoaded(c);
    List<Entry> list = chat ? CHAT : NOTES;
    if (!list.isEmpty()) {
      Entry last = list.get(list.size() - 1);
      if (last.pkg.equals(pkg) && last.text.equals(text)) {
        return; // same thing twice in a row
      }
    }
    list.add(new Entry(System.currentTimeMillis(), pkg, text));
    while (list.size() > MAX_ENTRIES) {
      list.remove(0);
    }
    write(storeFile(c, chat), list);
  }

  private static synchronized void clear(Context c, boolean chat) {
    ensureLoaded(c);
    (chat ? CHAT : NOTES).clear();
    write(storeFile(c, chat), chat ? CHAT : NOTES);
  }

  private static String appName(Context c, String pkg) {
    try {
      android.content.pm.PackageManager pm = c.getPackageManager();
      return pm.getApplicationInfo(pkg, 0).loadLabel(pm).toString();
    } catch (Exception e) {
      return pkg;
    }
  }

  private static String when(long t) {
    return java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT)
        .format(new java.util.Date(t));
  }

  private static synchronized boolean showHistory(final Context c, final boolean chat) {
    ensureLoaded(c);
    List<Entry> list = chat ? CHAT : NOTES;
    if (list.isEmpty()) {
      LuaServiceApi.speakStatic(chat ? "No chat messages yet" : "No notifications yet");
      return false;
    }
    List<MsListActivity.Item> items = new ArrayList<>();
    StringBuilder all = new StringBuilder();
    for (int i = list.size() - 1; i >= 0; i--) { // newest first
      final Entry e = list.get(i);
      String label = appName(c, e.pkg) + ": " + e.text + ", " + when(e.time);
      all.append(appName(c, e.pkg)).append(": ").append(e.text).append('\n');
      items.add(new MsListActivity.Item(label, () -> {
        copyText(c, e.text);
        LuaServiceApi.speakStatic("Copied");
      }, false));
    }
    final String allText = all.toString();
    items.add(0, new MsListActivity.Item("Copy all", () -> {
      copyText(c, allText);
      LuaServiceApi.speakStatic("Copied all");
    }, false));
    items.add(1, new MsListActivity.Item("Delete all", () -> {
      clear(c, chat);
      LuaServiceApi.speakStatic("Deleted");
    }, true));
    return MsListActivity.show(c, chat ? "Chat history" : "Notification box", items);
  }

  private static synchronized boolean readAllNotifications(LuaServiceApi api, Context c) {
    ensureLoaded(c);
    if (NOTES.isEmpty()) {
      api.speak("No notifications");
      return false;
    }
    StringBuilder sb = new StringBuilder();
    int from = Math.max(0, NOTES.size() - 20);
    for (int i = NOTES.size() - 1; i >= from; i--) {
      Entry e = NOTES.get(i);
      sb.append(appName(c, e.pkg)).append(": ").append(e.text).append(". ");
    }
    api.speak(sb.toString());
    return true;
  }

  // ---------------------------------------------------------------- event hook

  private static String lastTransText = "";
  private static long lastOcrAt;
  private static final Map<String, String> LAST_CHAT = new HashMap<>();
  private static long lastLogTrim;

  /** Called from TalkBackService.onAccessibilityEvent for every event. Must stay cheap. */
  public static void onEvent(final TalkBackService s, AccessibilityEvent e) {
    try {
      int type = e.getEventType();
      String pkg = e.getPackageName() == null ? "" : e.getPackageName().toString();
      SharedPreferences p = prefs(s);

      if (p.getBoolean(K_LOG, false)) {
        log(s, e, pkg);
      }

      if (type == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
        String text = notificationText(e);
        if (!text.isEmpty()) {
          add(s, false, pkg, text);
          if (CHAT_PKGS.contains(pkg) && !p.getBoolean(K_CHAT_HISTORY_OFF, false)) {
            add(s, true, pkg, text);
          }
        }
        return;
      }

      if (type == TOUCH_EXPLORATION_GESTURE_END && p.getBoolean(K_LIFT, false)) {
        MAIN.postDelayed(() -> liftActivate(s), 140);
        return;
      }

      if (type == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
        boolean detail = p.getBoolean(K_DETAIL, false);
        boolean ocr = p.getBoolean(K_AUTO_OCR, false);
        boolean trans = p.getBoolean(K_AUTO_TRANS, false);
        if (!detail && !ocr && !trans) {
          return;
        }
        AccessibilityNodeInfo src = e.getSource();
        if (src == null) {
          return;
        }
        if (detail) {
          s.msSpeakMain(details(src), true);
        }
        String own = textOf(src);
        if (own.isEmpty() && ocr) {
          autoOcr(s, src);
        } else if (!own.isEmpty() && trans) {
          autoTranslate(s, own);
        }
        return;
      }

      if (!p.getBoolean(K_AUTO_CHAT, false) || !CHAT_PKGS.contains(pkg)) {
        return;
      }
      if (type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
        autoChat(s, e, pkg, !p.getBoolean(K_CHAT_HISTORY_OFF, false));
      }
    } catch (RuntimeException ignored) {
      // never disturb the screen reader because of an extra
    }
  }

  private static String notificationText(AccessibilityEvent e) {
    StringBuilder sb = new StringBuilder();
    Object data = e.getParcelableData();
    if (data instanceof Notification) {
      android.os.Bundle x = ((Notification) data).extras;
      if (x != null) {
        CharSequence title = x.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = x.getCharSequence(Notification.EXTRA_TEXT);
        if (title != null && title.length() > 0) {
          sb.append(title);
        }
        if (text != null && text.length() > 0) {
          sb.append(sb.length() > 0 ? ": " : "").append(text);
        }
      }
    }
    if (sb.length() == 0 && e.getText() != null) {
      for (CharSequence t : e.getText()) {
        if (t != null && t.length() > 0) {
          sb.append(sb.length() > 0 ? ", " : "").append(t);
        }
      }
    }
    return sb.toString().trim();
  }

  private static void liftActivate(TalkBackService s) {
    AccessibilityNodeInfo root = s.getRootInActiveWindow();
    if (root == null) {
      return;
    }
    AccessibilityNodeInfo f = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
    if (f != null) {
      clickNode(f);
    }
  }

  private static String details(AccessibilityNodeInfo n) {
    StringBuilder sb = new StringBuilder(role(n));
    if (n.isClickable()) {
      sb.append(", clickable");
    }
    if (n.isLongClickable()) {
      sb.append(", long clickable");
    }
    if (n.isCheckable()) {
      sb.append(n.isChecked() ? ", checked" : ", not checked");
    }
    if (!n.isEnabled()) {
      sb.append(", disabled");
    }
    AccessibilityNodeInfo parent = n.getParent();
    if (parent != null) {
      int idx = -1;
      for (int i = 0; i < parent.getChildCount(); i++) {
        AccessibilityNodeInfo c = parent.getChild(i);
        if (c != null && c.equals(n)) {
          idx = i;
          break;
        }
      }
      if (idx >= 0) {
        sb.append(", item ").append(idx + 1).append(" of ").append(parent.getChildCount());
      }
    }
    Rect r = new Rect();
    n.getBoundsInScreen(r);
    sb.append(", at ").append(r.centerX()).append(" ").append(r.centerY());
    return sb.toString();
  }

  private static void autoOcr(final TalkBackService s, AccessibilityNodeInfo n) {
    long now = android.os.SystemClock.elapsedRealtime();
    if (now - lastOcrAt < 2500 || n.isEditable()) {
      return;
    }
    Rect r = new Rect();
    n.getBoundsInScreen(r);
    if (r.width() < 24 || r.height() < 24) {
      return;
    }
    lastOcrAt = now;
    OcrTool.recognize(
        s,
        r,
        (text, message) -> {
          if (text != null && !text.trim().isEmpty()) {
            s.msSpeakMain(text.trim(), true);
          }
        });
  }

  private static void autoTranslate(final TalkBackService s, String text) {
    if (text.length() < 2 || text.length() > 500 || text.equals(lastTransText)) {
      return;
    }
    lastTransText = text;
    TranslateEngine.translate(s, text, t -> s.msSpeakMain(t, true));
  }

  private static void autoChat(TalkBackService s, AccessibilityEvent e, String pkg, boolean save) {
    AccessibilityNodeInfo src = e.getSource();
    if (src == null) {
      return;
    }
    AccessibilityNodeInfo list = src;
    for (int i = 0; list != null && i < 6; i++) {
      if (list.getCollectionInfo() != null || (list.isScrollable() && list.getChildCount() > 1)) {
        break;
      }
      list = list.getParent();
    }
    if (list == null || list.getChildCount() == 0) {
      return;
    }
    String last = "";
    for (int i = list.getChildCount() - 1; i >= 0 && last.isEmpty(); i--) {
      AccessibilityNodeInfo c = list.getChild(i);
      if (c != null && c.isVisibleToUser()) {
        last = deepText(c, 0);
      }
    }
    if (last.isEmpty()) {
      return;
    }
    String before = LAST_CHAT.put(pkg, last);
    if (before != null && !before.equals(last)) { // first look only remembers
      if (save) {
        add(s, true, pkg, last);
      }
      s.msSpeakMain(last, true);
    }
  }

  private static void log(Context c, AccessibilityEvent e, String pkg) {
    try {
      File dir = new File(LuaPaths.base(c), "Logs");
      dir.mkdirs();
      File f = new File(dir, "log.txt");
      long now = System.currentTimeMillis();
      if (f.length() > 1_000_000 && now - lastLogTrim > 1000) {
        lastLogTrim = now;
        f.renameTo(new File(dir, "log-old.txt"));
        f = new File(dir, "log.txt");
      }
      try (FileWriter w = new FileWriter(f, true)) {
        w.write(now + " " + AccessibilityEvent.eventTypeToString(e.getEventType()) + " " + pkg + " "
            + e.getClassName() + " " + e.getText() + "\n");
      }
    } catch (IOException | RuntimeException ignored) {
      // best effort
    }
  }

  // ---------------------------------------------------------------- gesture blocking

  private static Set<Integer> multiIds;
  private static Set<Integer> cornerIds;

  private static void buildGestureSets() {
    multiIds = new HashSet<>();
    cornerIds = new HashSet<>();
    for (Field f : AccessibilityService.class.getFields()) {
      String n = f.getName();
      if (!n.startsWith("GESTURE_") || f.getType() != int.class) {
        continue;
      }
      try {
        int v = f.getInt(null);
        if (n.startsWith("GESTURE_2_FINGER") || n.startsWith("GESTURE_3_FINGER") || n.startsWith("GESTURE_4_FINGER")) {
          multiIds.add(v);
        } else if (n.matches("GESTURE_SWIPE_(UP|DOWN|LEFT|RIGHT)_AND_(UP|DOWN|LEFT|RIGHT)")) {
          cornerIds.add(v);
        }
      } catch (IllegalAccessException ignored) {
        // skip
      }
    }
  }

  /** True when this gesture is switched off by "multi-finger" or "corner gestures" suspend. */
  public static synchronized boolean gestureBlocked(Context c, int gestureId) {
    SharedPreferences p = prefs(c);
    boolean noMulti = p.getBoolean(K_NO_MULTI, false);
    boolean noEdge = p.getBoolean(K_NO_EDGE, false);
    if (!noMulti && !noEdge) {
      return false;
    }
    if (multiIds == null) {
      buildGestureSets();
    }
    return (noMulti && multiIds.contains(gestureId)) || (noEdge && cornerIds.contains(gestureId));
  }

  // ---------------------------------------------------------------- node lists

  static final int MODE_ALL = 0;
  static final int MODE_CLICK = 1;
  static final int MODE_FOCUS = 2;
  static final int MODE_GRID = 3;

  private static void collectNodes(AccessibilityNodeInfo n, List<AccessibilityNodeInfo> out, boolean all, int[] budget) {
    if (n == null || budget[0]-- <= 0) {
      return;
    }
    if (all || n.isVisibleToUser()) {
      if (all || !textOf(n).isEmpty() || n.isClickable()) {
        out.add(n);
      }
    }
    for (int i = 0; i < n.getChildCount(); i++) {
      collectNodes(n.getChild(i), out, all, budget);
    }
  }

  private static boolean nodeList(LuaServiceApi api, final Context c, TalkBackService s, final int mode, AccessibilityNodeInfo focus) {
    AccessibilityNodeInfo root = s.getRootInActiveWindow();
    if (root == null) {
      api.speak("Screen is not readable");
      return false;
    }
    List<AccessibilityNodeInfo> nodes = new ArrayList<>();
    collectNodes(root, nodes, mode == MODE_ALL, new int[] {1500});
    if (mode != MODE_ALL) {
      Collections.sort(
          nodes,
          new Comparator<AccessibilityNodeInfo>() {
            @Override
            public int compare(AccessibilityNodeInfo a, AccessibilityNodeInfo b) {
              Rect ra = new Rect();
              Rect rb = new Rect();
              a.getBoundsInScreen(ra);
              b.getBoundsInScreen(rb);
              return ra.top != rb.top ? Integer.compare(ra.top, rb.top) : Integer.compare(ra.left, rb.left);
            }
          });
    }
    android.util.DisplayMetrics dm = c.getResources().getDisplayMetrics();
    List<MsListActivity.Item> items = new ArrayList<>();
    for (final AccessibilityNodeInfo n : nodes) {
      Rect r = new Rect();
      n.getBoundsInScreen(r);
      String text = textOf(n);
      String label;
      if (mode == MODE_ALL) {
        String id = n.getViewIdResourceName();
        label = role(n) + ": " + (text.isEmpty() ? "no text" : text)
            + (id == null ? "" : ", id " + id)
            + (n.isClickable() ? ", clickable" : "")
            + (n.isVisibleToUser() ? "" : ", hidden")
            + ", " + r.flattenToString();
      } else if (mode == MODE_GRID) {
        int col = Math.min(2, Math.max(0, r.centerX() * 3 / Math.max(1, dm.widthPixels)));
        int row = Math.min(3, Math.max(0, r.centerY() * 4 / Math.max(1, dm.heightPixels)));
        label = "Row " + (row + 1) + ", column " + (col + 1) + ": " + (text.isEmpty() ? role(n) : text);
      } else {
        label = (text.isEmpty() ? "" : text + ", ") + role(n);
      }
      Runnable action;
      if (mode == MODE_ALL) {
        final String copy = label;
        action = () -> {
          copyText(c, copy);
          LuaServiceApi.speakStatic("Copied");
        };
      } else if (mode == MODE_CLICK) {
        action = () -> clickNode(n);
      } else {
        action = () -> focusNode(n);
      }
      items.add(new MsListActivity.Item(label, action, mode != MODE_ALL));
    }
    if (mode == MODE_GRID) {
      // keep reading order by grid cell
      Collections.sort(items, (a, b) -> a.label.compareTo(b.label));
    }
    String title = mode == MODE_ALL ? "All nodes" : mode == MODE_CLICK ? "Virtual screen"
        : mode == MODE_FOCUS ? "Virtual navigation" : "Screen grid";
    if (!MsListActivity.show(c, title, items)) {
      api.speak("Nothing found on the screen");
      return false;
    }
    return true;
  }

  private static boolean listBrowse(LuaServiceApi api, final Context c, TalkBackService s, AccessibilityNodeInfo focus) {
    AccessibilityNodeInfo list = focus;
    for (int i = 0; list != null && i < 8; i++) {
      if (list.getCollectionInfo() != null || (list.isScrollable() && list.getChildCount() > 1)) {
        break;
      }
      list = list.getParent();
    }
    if (list == null || list.getChildCount() == 0) {
      api.speak("No list here");
      return false;
    }
    List<MsListActivity.Item> items = new ArrayList<>();
    for (int i = 0; i < list.getChildCount() && i < 300; i++) {
      final AccessibilityNodeInfo child = list.getChild(i);
      if (child == null) {
        continue;
      }
      String t = deepText(child, 0);
      if (t.isEmpty()) {
        continue;
      }
      items.add(new MsListActivity.Item(t, () -> focusNode(child), true));
    }
    if (!MsListActivity.show(c, "List", items)) {
      api.speak("The list has no text");
      return false;
    }
    return true;
  }

  private static boolean granularBrowse(LuaServiceApi api, final Context c, AccessibilityNodeInfo n) {
    String text = textOf(n);
    if (text.isEmpty()) {
      api.speak("No text here");
      return false;
    }
    String[] parts = text.split("(?<=[.!?\u0964\n])\\s*");
    List<MsListActivity.Item> items = new ArrayList<>();
    for (final String part : parts) {
      final String piece = part.trim();
      if (!piece.isEmpty()) {
        items.add(new MsListActivity.Item(piece, () -> {
          copyText(c, piece);
          LuaServiceApi.speakStatic("Copied");
        }, false));
      }
    }
    return MsListActivity.show(c, "Granular browsing", items);
  }

  // ---------------------------------------------------------------- modes and switches

  private static boolean tapMode(LuaServiceApi api, Context c) {
    // Tap to move focus is TalkBack's normal behaviour: lift-to-activate off, touch browsing on.
    prefs(c).edit().putBoolean(K_LIFT, false).apply();
    api.speak("Tap to move focus mode. Double tap to activate.");
    return true;
  }

  private static boolean quick(LuaServiceApi api, Context c, TalkBackService s) {
    SharedPreferences p = prefs(c);
    boolean now = !p.getBoolean(K_QUICK, false);
    p.edit().putBoolean(K_QUICK, now).apply();
    s.msChangeSpeechRate(now);
    s.msChangeSpeechRate(now);
    api.speak(now ? "Quick browsing on" : "Quick browsing off");
    return true;
  }

  private static boolean shortcut(LuaServiceApi api, Context c, TalkBackService s, AccessibilityNodeInfo n, boolean right) {
    String cmd = prefs(c).getString(right ? K_RIGHT : K_LEFT, "");
    if (cmd != null && !cmd.isEmpty() && !cmd.startsWith("command_left") && !cmd.startsWith("command_right")) {
      return JxCommands.run(api, c, cmd, n);
    }
    return MsNavModes.step(api, c, s, n, !right); // default: previous / next of the chosen type
  }

  /** Applies the actual earpiece mode, shared by the command and Lua service setters. */
  static boolean setEarpieceMode(Context c, boolean enabled) {
    android.media.AudioManager am =
        (android.media.AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
    if (am == null) {
      return false;
    }
    try {
      if (enabled) {
        am.setMode(android.media.AudioManager.MODE_IN_COMMUNICATION);
        am.setSpeakerphoneOn(false);
      } else {
        am.setMode(android.media.AudioManager.MODE_NORMAL);
      }
      prefs(c).edit().putBoolean(K_EARPIECE, enabled).apply();
      return true;
    } catch (RuntimeException e) {
      return false;
    }
  }

  private static boolean earpiece(LuaServiceApi api, Context c) {
    boolean now = !prefs(c).getBoolean(K_EARPIECE, false);
    if (!setEarpieceMode(c, now)) {
      api.speak("This phone does not allow it");
      return false;
    }
    api.speak(now ? "Earpiece mode on. It may not work with every voice." : "Earpiece mode off");
    return true;
  }

  // ---------------------------------------------------------------- profiles, camera, location, cloud

  private static boolean profiles(LuaServiceApi api, final Context c) {
    File dir = new File(LuaPaths.base(c), "Profiles");
    dir.mkdirs();
    File[] dirs = dir.listFiles();
    List<MsListActivity.Item> items = new ArrayList<>();
    if (dirs != null) {
      Arrays.sort(dirs);
      for (final File d : dirs) {
        final File main = new File(d, "main.lua");
        if (d.isDirectory() && main.isFile()) {
          items.add(new MsListActivity.Item(d.getName(), () -> {
            final Context app = c.getApplicationContext();
            new Thread(() -> {
              String err = LuaServiceApi.run(app, main, null);
              if (err != null) {
                LuaServiceApi.speakStatic(err);
              }
            }, "ms-profile").start();
          }, true));
        }
      }
    }
    if (items.isEmpty()) {
      api.speak("No profiles yet. Put a folder with main.lua in the Profiles folder.");
      return false;
    }
    return MsListActivity.show(c, "Profiles", items);
  }

  private static boolean camera(Context c) {
    try {
      Intent i = new Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
      i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      c.startActivity(i);
      return true;
    } catch (RuntimeException e) {
      return false;
    }
  }

  private static boolean location(final LuaServiceApi api, final Context c) {
    boolean fine = c.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
        == android.content.pm.PackageManager.PERMISSION_GRANTED;
    boolean coarse = c.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        == android.content.pm.PackageManager.PERMISSION_GRANTED;
    if (!fine && !coarse) {
      api.speak("Location permission is missing. Opening app settings.");
      try {
        Intent i = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.parse("package:" + c.getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(i);
      } catch (RuntimeException ignored) {
        // nothing more to do
      }
      return false;
    }
    final android.location.LocationManager lm =
        (android.location.LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
    android.location.Location best = null;
    if (lm != null) {
      for (String prov : new String[] {"gps", "network", "passive"}) {
        try {
          android.location.Location l = lm.getLastKnownLocation(prov);
          if (l != null && (best == null || l.getTime() > best.getTime())) {
            best = l;
          }
        } catch (RuntimeException ignored) {
          // provider not allowed
        }
      }
    }
    if (best == null) {
      api.speak("Location is not available. Turn on location and try again.");
      return false;
    }
    final android.location.Location loc = best;
    new Thread(() -> {
      String text = null;
      try {
        if (android.location.Geocoder.isPresent()) {
          List<android.location.Address> a =
              new android.location.Geocoder(c, java.util.Locale.getDefault())
                  .getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
          if (a != null && !a.isEmpty() && a.get(0).getMaxAddressLineIndex() >= 0) {
            text = a.get(0).getAddressLine(0);
          }
        }
      } catch (IOException | RuntimeException ignored) {
        // no address service
      }
      if (text == null) {
        text = "Latitude " + String.format(java.util.Locale.US, "%.5f", loc.getLatitude())
            + ", longitude " + String.format(java.util.Locale.US, "%.5f", loc.getLongitude());
      }
      final String say = text;
      MAIN.post(() -> api.speak(say));
    }, "ms-location").start();
    return true;
  }

  private static File cloudFile(Context c) {
    return new File(LuaPaths.base(c), "Cloud clipboard.txt");
  }

  private static boolean cloudCopy(LuaServiceApi api, Context c, AccessibilityNodeInfo n) {
    String text = null;
    ClipboardManager cm = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
    try {
      if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
        CharSequence t = cm.getPrimaryClip().getItemAt(0).coerceToText(c);
        text = t == null ? null : t.toString();
      }
    } catch (RuntimeException ignored) {
      // clipboard blocked in background
    }
    if (text == null || text.isEmpty()) {
      text = textOf(n);
    }
    if (text.isEmpty()) {
      api.speak("Nothing to copy");
      return false;
    }
    try (FileWriter w = new FileWriter(cloudFile(c))) {
      w.write(text);
    } catch (IOException e) {
      api.speak("Could not save");
      return false;
    }
    api.speak("Copied to the shared clipboard file");
    return true;
  }

  private static boolean cloudPaste(LuaServiceApi api, Context c, AccessibilityNodeInfo n) {
    File f = cloudFile(c);
    if (!f.isFile()) {
      api.speak("Shared clipboard is empty");
      return false;
    }
    StringBuilder sb = new StringBuilder();
    try (BufferedReader r = new BufferedReader(new FileReader(f))) {
      String line;
      while ((line = r.readLine()) != null) {
        sb.append(sb.length() > 0 ? "\n" : "").append(line);
      }
    } catch (IOException e) {
      return false;
    }
    String text = sb.toString();
    if (text.isEmpty()) {
      api.speak("Shared clipboard is empty");
      return false;
    }
    copyText(c, text);
    if (n != null && n.isEditable() && n.performAction(AccessibilityNodeInfo.ACTION_PASTE)) {
      api.speak("Pasted");
    } else {
      api.speak("Copied. Paste it where you want.");
    }
    return true;
  }
}
