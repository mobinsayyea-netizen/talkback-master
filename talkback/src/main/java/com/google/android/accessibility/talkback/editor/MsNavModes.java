/*
 * Copyright 2026 Advanced Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import static com.google.android.accessibility.talkback.focusmanagement.NavigationTarget.TARGET_CONTROL;
import static com.google.android.accessibility.talkback.focusmanagement.NavigationTarget.TARGET_HEADING;
import static com.google.android.accessibility.talkback.focusmanagement.NavigationTarget.TARGET_HTML_ELEMENT_ARIA_LANDMARK;
import static com.google.android.accessibility.talkback.focusmanagement.NavigationTarget.TARGET_HTML_ELEMENT_BUTTON;
import static com.google.android.accessibility.talkback.focusmanagement.NavigationTarget.TARGET_HTML_ELEMENT_CHECKBOX;
import static com.google.android.accessibility.talkback.focusmanagement.NavigationTarget.TARGET_HTML_ELEMENT_COMBOBOX;
import static com.google.android.accessibility.talkback.focusmanagement.NavigationTarget.TARGET_HTML_ELEMENT_EDIT_FIELD;
import static com.google.android.accessibility.talkback.focusmanagement.NavigationTarget.TARGET_HTML_ELEMENT_GRAPHIC;
import static com.google.android.accessibility.talkback.focusmanagement.NavigationTarget.TARGET_HTML_ELEMENT_LIST;
import static com.google.android.accessibility.talkback.focusmanagement.NavigationTarget.TARGET_HTML_ELEMENT_RADIO;
import static com.google.android.accessibility.talkback.focusmanagement.NavigationTarget.TARGET_HTML_ELEMENT_TABLE;
import static com.google.android.accessibility.talkback.focusmanagement.NavigationTarget.TARGET_LINK;
import static com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_BACKWARD;
import static com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_FORWARD;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityNodeInfo;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.utils.SharedPreferencesUtils;

/**
 * "Select navigation type" with "Next/Previous element of the specified type", backwards automatic
 * browsing, and long press and hold. Not compiled yet.
 */
final class MsNavModes {
  private MsNavModes() {}

  static final String PREF_TYPE = "ms_nav_type";

  /** key, label. Types with "web" only find something inside web pages. */
  static final String[][] TYPES = {
    {"heading", "Headings"},
    {"link", "Links"},
    {"control", "Controls"},
    {"char", "Characters"},
    {"word", "Words"},
    {"line", "Lines"},
    {"paragraph", "Paragraphs"},
    {"web_button", "Buttons (web)"},
    {"web_checkbox", "Check boxes (web)"},
    {"web_edit", "Edit fields (web)"},
    {"web_graphic", "Graphics (web)"},
    {"web_list", "Lists (web)"},
    {"web_table", "Tables (web)"},
    {"web_radio", "Radio buttons (web)"},
    {"web_combobox", "Combo boxes (web)"},
    {"web_landmark", "Landmarks (web)"},
  };

  static String labelOf(String key) {
    for (String[] t : TYPES) {
      if (t[0].equals(key)) {
        return t[1];
      }
    }
    return TYPES[0][1];
  }

  static String current(Context ctx) {
    SharedPreferences sp = SharedPreferencesUtils.getSharedPreferences(ctx);
    return sp.getString(PREF_TYPE, "heading");
  }

  /** Called by the picker screen when a type was chosen. */
  static void choose(Context ctx, String key) {
    SharedPreferencesUtils.getSharedPreferences(ctx).edit().putString(PREF_TYPE, key).apply();
    LuaServiceApi.speakStatic(labelOf(key));
  }

  /** Moves to the next or previous element of the chosen type. */
  static boolean step(
      LuaServiceApi api, Context ctx, TalkBackService s, AccessibilityNodeInfo n, boolean back) {
    int dir = back ? SEARCH_FOCUS_BACKWARD : SEARCH_FOCUS_FORWARD;
    switch (current(ctx)) {
      case "heading":
        return JxCommandsNav.target(api, s, dir, TARGET_HEADING);
      case "link":
        return JxCommandsNav.target(api, s, dir, TARGET_LINK);
      case "control":
        return JxCommandsNav.target(api, s, dir, TARGET_CONTROL);
      case "char":
        return JxCommandsNav.move(n, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER, back);
      case "word":
        return JxCommandsNav.move(n, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD, back);
      case "line":
        return JxCommandsNav.move(n, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE, back);
      case "paragraph":
        return JxCommandsNav.move(n, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH, back);
      case "web_button":
        return JxCommandsNav.target(api, s, dir, TARGET_HTML_ELEMENT_BUTTON);
      case "web_checkbox":
        return JxCommandsNav.target(api, s, dir, TARGET_HTML_ELEMENT_CHECKBOX);
      case "web_edit":
        return JxCommandsNav.target(api, s, dir, TARGET_HTML_ELEMENT_EDIT_FIELD);
      case "web_graphic":
        return JxCommandsNav.target(api, s, dir, TARGET_HTML_ELEMENT_GRAPHIC);
      case "web_list":
        return JxCommandsNav.target(api, s, dir, TARGET_HTML_ELEMENT_LIST);
      case "web_table":
        return JxCommandsNav.target(api, s, dir, TARGET_HTML_ELEMENT_TABLE);
      case "web_radio":
        return JxCommandsNav.target(api, s, dir, TARGET_HTML_ELEMENT_RADIO);
      case "web_combobox":
        return JxCommandsNav.target(api, s, dir, TARGET_HTML_ELEMENT_COMBOBOX);
      case "web_landmark":
        return JxCommandsNav.target(api, s, dir, TARGET_HTML_ELEMENT_ARIA_LANDMARK);
      default:
        return false;
    }
  }

  // ---------------------------------------------------------------- Backwards automatic browsing

  private static final Handler MAIN = new Handler(Looper.getMainLooper());
  private static final long AUTO_PERIOD_MS = 1600;
  private static final long AUTO_MAX_MS = 20 * 60 * 1000L;
  private static boolean autoBack;
  private static long autoStopAt;
  private static String lastKey = "";
  private static int sameCount;

  /** Moves the focus backwards again and again, until the start, the same command, or 20 minutes. */
  static boolean toggleAutoBackward(final LuaServiceApi api) {
    if (autoBack) {
      autoBack = false;
      api.speak("Backwards automatic browsing off");
      return true;
    }
    autoBack = true;
    autoStopAt = System.currentTimeMillis() + AUTO_MAX_MS;
    lastKey = "";
    sameCount = 0;
    api.speak("Backwards automatic browsing on");
    MAIN.postDelayed(() -> autoRound(api), 1500);
    return true;
  }

  private static String focusKey(LuaServiceApi api) {
    AccessibilityNodeInfo f = api.focusedNode();
    if (f == null) {
      return "";
    }
    Rect r = new Rect();
    f.getBoundsInScreen(r);
    return r.flattenToString() + "|" + f.getText() + "|" + f.getContentDescription();
  }

  private static void autoRound(final LuaServiceApi api) {
    if (!autoBack) {
      return;
    }
    if (System.currentTimeMillis() > autoStopAt) {
      autoBack = false;
      api.speak("Backwards automatic browsing stopped after 20 minutes");
      return;
    }
    String key = focusKey(api);
    sameCount = key.equals(lastKey) ? sameCount + 1 : 0;
    if (sameCount >= 2) { // the focus did not move twice: we are at the start
      autoBack = false;
      api.speak("Reached the beginning");
      return;
    }
    lastKey = key;
    api.toPrevious();
    MAIN.postDelayed(() -> autoRound(api), AUTO_PERIOD_MS);
  }

  // ---------------------------------------------------------------- Long press and hold

  /** Presses the middle of the focused item and keeps the finger down for 1.5 seconds. */
  static boolean longHold(LuaServiceApi api, AccessibilityNodeInfo n) {
    if (n == null) {
      return false;
    }
    Rect r = new Rect();
    n.getBoundsInScreen(r);
    if (r.isEmpty()) {
      return false;
    }
    return api.press(r.centerX(), r.centerY(), 1500);
  }
}
