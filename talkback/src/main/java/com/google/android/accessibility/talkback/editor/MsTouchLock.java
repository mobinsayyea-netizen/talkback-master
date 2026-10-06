/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import com.google.android.accessibility.talkback.TalkBackService;

/**
 * "Lock touch" (false touch prevention): a see-through full-screen layer takes every touch, so a
 * finger in a pocket or a child cannot press anything. Screen reader gestures and actions that come
 * through the accessibility service are not touches on this layer. The same command removes it.
 * Not compiled yet.
 */
final class MsTouchLock {
  private MsTouchLock() {}

  private static View overlay;

  static boolean toggle(final LuaServiceApi api, final TalkBackService s) {
    new Handler(Looper.getMainLooper())
        .post(
            () -> {
              WindowManager wm = (WindowManager) s.getSystemService(android.content.Context.WINDOW_SERVICE);
              if (wm == null) {
                api.speak("Could not lock the touch");
                return;
              }
              if (overlay != null) {
                try {
                  wm.removeView(overlay);
                } catch (RuntimeException ignored) {
                }
                overlay = null;
                api.speak("Touch unlocked");
                return;
              }
              View v = new View(s);
              v.setOnTouchListener((view, event) -> true); // take the touch, do nothing
              v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
              WindowManager.LayoutParams lp =
                  new WindowManager.LayoutParams(
                      WindowManager.LayoutParams.MATCH_PARENT,
                      WindowManager.LayoutParams.MATCH_PARENT,
                      WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                          | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                          | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                      PixelFormat.TRANSLUCENT);
              try {
                wm.addView(v, lp);
                overlay = v;
                api.speak("Touch locked");
              } catch (RuntimeException e) {
                api.speak("Could not lock the touch");
              }
            });
    return true;
  }
}
