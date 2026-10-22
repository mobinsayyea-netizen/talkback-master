/*
 * Copyright 2026 Advanced Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.google.android.accessibility.utils.SharedPreferencesUtils;

/**
 * Step 24: working versions of four Reading settings that were only saved before.
 * "Allow reading of element indexes and display range in lists" (switches the list positions off),
 * "Read the state of the focused item first" (checked/unchecked before the name),
 * "Automatically read changes in the current window" (text that changes anywhere in the window),
 * "Read all visible content of the list when scrolling using the commands". Not compiled yet.
 */
public final class MsReadingExtras {
  private MsReadingExtras() {}

  public static final String K_LIST_ALLOW = "ms_rd_list_allow";
  public static final String K_STATE_FIRST = "ms_rd_state_first";
  public static final String K_WINDOW_CHANGES = "ms_rd_window_changes";
  public static final String K_SCROLL_CONTENT = "ms_rd_list_scroll_content";

  private static final Handler MAIN = new Handler(Looper.getMainLooper());
  private static String lastChangeText = "";
  private static long lastChangeTime;

  private static boolean on(Context c, String key, boolean def) {
    return SharedPreferencesUtils.getSharedPreferences(c).getBoolean(key, def);
  }

  public static boolean listAllowed(Context c) {
    return on(c, K_LIST_ALLOW, true);
  }

  public static boolean stateFirst(Context c) {
    return on(c, K_STATE_FIRST, false);
  }

  /** Speaks text that changed in a part of the window that does not have the focus. */
  public static void onWindowChanged(TalkBackService service, AccessibilityEvent event) {
    if (service == null
        || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        || (event.getContentChangeTypes() & AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT) == 0
        || !on(service, K_WINDOW_CHANGES, false)) {
      return;
    }
    AccessibilityNodeInfo src = event.getSource();
    if (src == null
        || src.isAccessibilityFocused()
        || src.isPassword()
        || src.isEditable()
        || !src.isVisibleToUser()
        || src.getLiveRegion() != android.view.View.ACCESSIBILITY_LIVE_REGION_NONE) {
      return; // the focused item and live regions are already read by TalkBack
    }
    CharSequence t = src.getText();
    if (TextUtils.isEmpty(t)) {
      return;
    }
    String s = t.toString().trim();
    long now = System.currentTimeMillis();
    if (s.isEmpty() || (s.equals(lastChangeText) && now - lastChangeTime < 1500)) {
      return;
    }
    lastChangeText = s;
    lastChangeTime = now;
    service.msSpeakMain(s, true);
  }

  /** After a scroll command worked: reads what is visible in the list now. */
  public static void afterScroll(final AccessibilityNodeInfo container) {
    final TalkBackService s = TalkBackService.getInstance();
    if (s == null || container == null || !on(s, K_SCROLL_CONTENT, true)) {
      return;
    }
    MAIN.postDelayed(
        () -> {
          container.refresh();
          StringBuilder sb = new StringBuilder();
          int[] count = new int[1];
          collect(container, sb, count, 0);
          if (sb.length() > 0) {
            s.msSpeakMain(sb.toString(), true);
          }
        },
        com.google.android.accessibility.talkback.editor.MsGestureTiming.scrollReadDelayMs(s));
  }

  private static void collect(AccessibilityNodeInfo n, StringBuilder sb, int[] count, int depth) {
    if (n == null || count[0] >= 40 || sb.length() > 1500 || depth > 12) {
      return;
    }
    if (!n.isVisibleToUser() || n.isPassword()) {
      return;
    }
    CharSequence t = n.getText();
    if (TextUtils.isEmpty(t)) {
      t = n.getContentDescription();
    }
    if (!TextUtils.isEmpty(t) && n.getChildCount() == 0) {
      sb.append(t.toString().trim()).append(". ");
      count[0]++;
    }
    for (int i = 0; i < n.getChildCount(); i++) {
      collect(n.getChild(i), sb, count, depth + 1);
    }
  }
}
