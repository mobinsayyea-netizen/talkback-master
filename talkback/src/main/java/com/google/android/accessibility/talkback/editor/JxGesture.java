/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** MS Screen Reader: touch gestures for the Lua API (tap, long press, swipe). */
final class JxGesture {
  private JxGesture() {}

  /** Taps (or holds for durationMs) at x,y. Waits for the result unless on the main thread. */
  static boolean tap(AccessibilityService service, float x, float y, long durationMs) {
    Path p = new Path();
    p.moveTo(x, y);
    p.lineTo(x, y);
    return dispatch(service, p, 0, Math.max(1L, durationMs));
  }

  /** Swipes from x1,y1 to x2,y2 in durationMs. */
  static boolean swipe(
      AccessibilityService service, float x1, float y1, float x2, float y2, long durationMs) {
    Path p = new Path();
    p.moveTo(x1, y1);
    p.lineTo(x2, y2);
    return dispatch(service, p, 0, Math.max(1L, durationMs));
  }

  private static boolean dispatch(
      AccessibilityService service, Path path, long startMs, long durationMs) {
    if (service == null) {
      return false;
    }
    final CountDownLatch done = new CountDownLatch(1);
    final boolean[] ok = {false};
    GestureDescription gesture;
    try {
      gesture =
          new GestureDescription.Builder()
              .addStroke(new GestureDescription.StrokeDescription(path, startMs, durationMs))
              .build();
    } catch (IllegalArgumentException e) {
      return false;
    }
    boolean accepted =
        service.dispatchGesture(
            gesture,
            new AccessibilityService.GestureResultCallback() {
              @Override
              public void onCompleted(GestureDescription g) {
                ok[0] = true;
                done.countDown();
              }

              @Override
              public void onCancelled(GestureDescription g) {
                done.countDown();
              }
            },
            new Handler(Looper.getMainLooper()));
    if (!accepted) {
      return false;
    }
    if (Looper.myLooper() == Looper.getMainLooper()) {
      return true;
    }
    try {
      done.await(durationMs + 3000L, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
    return ok[0];
  }
}
