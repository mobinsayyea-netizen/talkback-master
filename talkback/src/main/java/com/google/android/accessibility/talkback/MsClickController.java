/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.google.android.accessibility.utils.SharedPreferencesUtils;

/**
 * MS Screen Reader: "Clicking settings". Single tap to activate, lift to activate and hold to long
 * press, built on touch-interaction events.
 */
public final class MsClickController {
  private static final String P = "ms_op_";

  private final TalkBackService service;
  private final SharedPreferences prefs;
  private final Handler handler = new Handler(Looper.getMainLooper());

  private long start;
  private long lastFocus;
  private boolean gesture;

  public MsClickController(TalkBackService service) {
    this.service = service;
    this.prefs = SharedPreferencesUtils.getSharedPreferences(service);
  }

  public void stop() {
    handler.removeCallbacksAndMessages(null);
  }

  public void onEvent(AccessibilityEvent ev) {
    switch (ev.getEventType()) {
      case AccessibilityEvent.TYPE_TOUCH_INTERACTION_START:
        start = SystemClock.elapsedRealtime();
        gesture = false;
        break;
      case AccessibilityEvent.TYPE_GESTURE_DETECTION_START:
        gesture = true;
        break;
      case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED:
        lastFocus = SystemClock.elapsedRealtime();
        break;
      case AccessibilityEvent.TYPE_TOUCH_INTERACTION_END:
        onEnd();
        break;
      default:
        break;
    }
  }

  private void onEnd() {
    if (gesture) {
      return;
    }
    boolean singleTap = prefs.getBoolean(P + "single_tap", false);
    boolean liftTap = prefs.getBoolean(P + "up_tap", false);
    if (!singleTap && !liftTap) {
      return;
    }
    long now = SystemClock.elapsedRealtime();
    long total = now - start;
    long dwell = now - lastFocus;
    long hold = holdMs();
    if (singleTap && total < 350) {
      activate(false);
    } else if (liftTap) {
      if (prefs.getBoolean(P + "up_long_click", true) && dwell >= hold) {
        activate(true);
      } else if (dwell < hold) {
        activate(false);
      }
    }
  }

  private long holdMs() {
    try {
      return Long.parseLong(prefs.getString(P + "long_click", "750"));
    } catch (NumberFormatException e) {
      return 750;
    }
  }

  private void activate(boolean longClick) {
    handler.postDelayed(
        () -> {
          AccessibilityNodeInfo root = service.getRootInActiveWindow();
          if (root == null) {
            return;
          }
          AccessibilityNodeInfo node = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
          while (node != null) {
            boolean ok =
                longClick ? node.isLongClickable() : node.isClickable();
            if (ok) {
              node.performAction(
                  longClick
                      ? AccessibilityNodeInfo.ACTION_LONG_CLICK
                      : AccessibilityNodeInfo.ACTION_CLICK);
              return;
            }
            node = node.getParent();
          }
        },
        180);
  }
}
