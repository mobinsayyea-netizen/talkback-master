/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

/**
 * The two-step state machine of multi-part gestures (pure Java, tested on a computer). Part 1 is a
 * gesture whose multi-part switch is on. After it the machine waits for part 2 for a short time.
 * Part 2 in time and assigned: its action runs and part 1's own action does not. No part 2 in time
 * (or part 2 not assigned): part 1's own action runs. Called on the main thread only.
 */
final class MultiPartCore {

  /** What the machine needs from the app. */
  interface Host {
    /** The global "Use multi-part gestures" switch. */
    boolean enabled();

    /** The gesture can be a part 1 and its own multi-part switch is on (current scheme). */
    boolean isFirstPart(int gesture);

    /** Part 2 has an action under part 1 (current scheme). */
    boolean hasSecond(int first, int second);

    void runSecond(int first, int second);

    /** Runs the gesture as if multi-part did not exist (its normal or extension action). */
    void runNormal(int gesture);

    void soundFirstPart();

    void soundSecondPart();

    /** Calls {@link MultiPartCore#onTimeout} after delayMs (replaces an older request). */
    void scheduleTimeout(long delayMs);

    void cancelTimeout();

    long durationMs();
  }

  private int pending = 0;

  boolean isWaiting() {
    return pending != 0;
  }

  /** Returns true when the gesture was taken by multi-part. False = handle it as usual. */
  boolean onGesture(Host h, int gesture) {
    if (pending != 0) {
      int first = pending;
      pending = 0;
      h.cancelTimeout();
      if (h.hasSecond(first, gesture)) {
        h.soundSecondPart();
        h.runSecond(first, gesture);
        return true;
      }
      // Part 2 has no action: part 1 does its own work now; this gesture starts fresh.
      h.runNormal(first);
    }
    if (!h.enabled() || !h.isFirstPart(gesture)) {
      return false;
    }
    pending = gesture;
    h.soundFirstPart();
    h.scheduleTimeout(h.durationMs());
    return true;
  }

  /** Time ran out without part 2. */
  void onTimeout(Host h) {
    if (pending != 0) {
      int first = pending;
      pending = 0;
      h.runNormal(first);
    }
  }

  /** Drops a waiting part 1 without running anything (service stops). */
  void reset(Host h) {
    pending = 0;
    h.cancelTimeout();
  }
}
