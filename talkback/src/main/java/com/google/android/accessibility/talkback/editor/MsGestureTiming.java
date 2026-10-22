/*
 * Copyright 2026 Advanced Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.ViewConfiguration;
import com.google.android.accessibility.utils.SharedPreferencesUtils;

/** Step 40: timing values for service-made long presses and for the wait after a scroll. */
public final class MsGestureTiming {
  private MsGestureTiming() {}

  static final String K_LONG_CLICK_EXTRA = "ms_gesture_long_click_extra";
  static final String K_SCROLL_READ_DELAY = "ms_scroll_read_delay";

  /** How long a service-made long press holds: system long-press timeout plus a safety margin. */
  public static long longClickMs(Context c) {
    return ViewConfiguration.getLongPressTimeout() + read(c, K_LONG_CLICK_EXTRA, 80, 30, 400);
  }

  /** Wait after a scroll before the new content is read. */
  public static long scrollReadDelayMs(Context c) {
    return read(c, K_SCROLL_READ_DELAY, 400, 150, 1000);
  }

  private static int read(Context c, String key, int def, int min, int max) {
    try {
      SharedPreferences p = SharedPreferencesUtils.getSharedPreferences(c);
      return Math.max(min, Math.min(max, Integer.parseInt(p.getString(key, String.valueOf(def)))));
    } catch (RuntimeException e) {
      return def;
    }
  }
}
