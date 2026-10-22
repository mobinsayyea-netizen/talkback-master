/*
 * Copyright 2026 Advanced Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.accessibilityservice.FingerprintGestureController;
import android.content.Context;
import android.content.SharedPreferences;
import com.google.android.accessibility.utils.SharedPreferencesUtils;

/**
 * Fingerprint reader swipes (Step 27). TalkBack already reports them (FingerprintGestureCallback in
 * TalkBackService). When "Use fingerprint reader gestures" is on and a swipe has a command, that
 * command runs; otherwise TalkBack's own fingerprint action is used as before. Not compiled yet.
 */
public final class MsFingerprintGestures {
  private MsFingerprintGestures() {}

  static final String K_USE = "ms_jx_use_fingerprint_gesture";
  static final String NOTHING = "command_nothing";

  /** The preference key that holds the command for a swipe, or null for an unknown gesture. */
  static String keyFor(int gesture) {
    switch (gesture) {
      case FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_LEFT:
        return "ms_jx_fingerprint_left_gesture";
      case FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_RIGHT:
        return "ms_jx_fingerprint_right_gesture";
      case FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_UP:
        return "ms_jx_fingerprint_up_gesture";
      case FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_DOWN:
        return "ms_jx_fingerprint_down_gesture";
      default:
        return null;
    }
  }

  /** True when a command took the swipe (TalkBack's own action must then be skipped). */
  public static boolean handle(final Context c, int gesture) {
    SharedPreferences p = SharedPreferencesUtils.getSharedPreferences(c);
    if (!p.getBoolean(K_USE, false)) {
      return false;
    }
    String key = keyFor(gesture);
    if (key == null) {
      return false;
    }
    final String cmd = p.getString(key, NOTHING);
    if (cmd == null || cmd.isEmpty() || NOTHING.equals(cmd)) {
      return false;
    }
    new Thread(() -> LuaServiceApi.executeStatic(c, cmd), "ms-fingerprint").start();
    return true;
  }
}
