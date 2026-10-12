/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.utils.Performance;
import com.google.android.accessibility.utils.SharedPreferencesUtils;

/**
 * Multi-part gestures, like Jieshuo (CSR class 52). Part 1 is one of 24 gestures (the 16 one-finger
 * swipes and the 3- and 4-finger swipes) whose own multi-part switch is on. The finger is lifted,
 * and part 2 (any gesture in {@link #SECOND_IDS}) must follow within the recognition duration
 * (setting ms_jx_double_fling_time, 500 ms by default). Then the action put on that pair runs. If
 * part 2 does not come in time, part 1's own action runs after the wait.
 *
 * <p>Saved in the gesture schemes (global scheme = ExtensionGestures, per-app = AppGestureSchemes)
 * with the old text format, using a combined number key(first, second) = 100000 + first * 1000 +
 * second. second = 0 holds the switch of part 1 (kind "mp", name "on"). Targets: extension, tool,
 * cmd (a screen reader function) or tb (a TalkBack action key). Not compiled yet.
 */
public final class MsMultiPartGestures {
  private MsMultiPartGestures() {}

  static final int BASE = 100000;
  static final String KIND_SWITCH = "mp";
  static final String KIND_TALKBACK = "tb";
  static final String PREF_USE = "ms_jx_use_double_fling";
  static final String PREF_TIME = "ms_jx_double_fling_time";

  /** Part 1 candidates: 16 one-finger swipes, 4 three-finger and 4 four-finger swipes. */
  public static final int[] FIRST_IDS = {
    1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
    AccessibilityService.GESTURE_3_FINGER_SWIPE_UP,
    AccessibilityService.GESTURE_3_FINGER_SWIPE_DOWN,
    AccessibilityService.GESTURE_3_FINGER_SWIPE_LEFT,
    AccessibilityService.GESTURE_3_FINGER_SWIPE_RIGHT,
    AccessibilityService.GESTURE_4_FINGER_SWIPE_UP,
    AccessibilityService.GESTURE_4_FINGER_SWIPE_DOWN,
    AccessibilityService.GESTURE_4_FINGER_SWIPE_LEFT,
    AccessibilityService.GESTURE_4_FINGER_SWIPE_RIGHT
  };

  public static final String[] FIRST_NAMES = {
    "Swipe up",
    "Swipe down",
    "Swipe left",
    "Swipe right",
    "Swipe left then right",
    "Swipe right then left",
    "Swipe up then down",
    "Swipe down then up",
    "Swipe left then up",
    "Swipe left then down",
    "Swipe right then up",
    "Swipe right then down",
    "Swipe up then left",
    "Swipe up then right",
    "Swipe down then left",
    "Swipe down then right",
    "Swipe up with three fingers",
    "Swipe down with three fingers",
    "Swipe left with three fingers",
    "Swipe right with three fingers",
    "Swipe up with four fingers",
    "Swipe down with four fingers",
    "Swipe left with four fingers",
    "Swipe right with four fingers"
  };

  /** Part 2 candidates: the part 1 list plus two-finger swipes and 2/3/4-finger taps. */
  public static final int[] SECOND_IDS = {
    1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
    AccessibilityService.GESTURE_2_FINGER_SWIPE_UP,
    AccessibilityService.GESTURE_2_FINGER_SWIPE_DOWN,
    AccessibilityService.GESTURE_2_FINGER_SWIPE_LEFT,
    AccessibilityService.GESTURE_2_FINGER_SWIPE_RIGHT,
    AccessibilityService.GESTURE_3_FINGER_SWIPE_UP,
    AccessibilityService.GESTURE_3_FINGER_SWIPE_DOWN,
    AccessibilityService.GESTURE_3_FINGER_SWIPE_LEFT,
    AccessibilityService.GESTURE_3_FINGER_SWIPE_RIGHT,
    AccessibilityService.GESTURE_4_FINGER_SWIPE_UP,
    AccessibilityService.GESTURE_4_FINGER_SWIPE_DOWN,
    AccessibilityService.GESTURE_4_FINGER_SWIPE_LEFT,
    AccessibilityService.GESTURE_4_FINGER_SWIPE_RIGHT,
    AccessibilityService.GESTURE_2_FINGER_SINGLE_TAP,
    AccessibilityService.GESTURE_2_FINGER_DOUBLE_TAP,
    AccessibilityService.GESTURE_2_FINGER_TRIPLE_TAP,
    AccessibilityService.GESTURE_3_FINGER_SINGLE_TAP,
    AccessibilityService.GESTURE_3_FINGER_DOUBLE_TAP,
    AccessibilityService.GESTURE_3_FINGER_TRIPLE_TAP,
    AccessibilityService.GESTURE_4_FINGER_SINGLE_TAP,
    AccessibilityService.GESTURE_4_FINGER_DOUBLE_TAP,
    AccessibilityService.GESTURE_4_FINGER_TRIPLE_TAP
  };

  public static final String[] SECOND_NAMES = {
    "Swipe up",
    "Swipe down",
    "Swipe left",
    "Swipe right",
    "Swipe left then right",
    "Swipe right then left",
    "Swipe up then down",
    "Swipe down then up",
    "Swipe left then up",
    "Swipe left then down",
    "Swipe right then up",
    "Swipe right then down",
    "Swipe up then left",
    "Swipe up then right",
    "Swipe down then left",
    "Swipe down then right",
    "Swipe up with two fingers",
    "Swipe down with two fingers",
    "Swipe left with two fingers",
    "Swipe right with two fingers",
    "Swipe up with three fingers",
    "Swipe down with three fingers",
    "Swipe left with three fingers",
    "Swipe right with three fingers",
    "Swipe up with four fingers",
    "Swipe down with four fingers",
    "Swipe left with four fingers",
    "Swipe right with four fingers",
    "Two-finger tap",
    "Two-finger double tap",
    "Two-finger triple tap",
    "Three-finger tap",
    "Three-finger double tap",
    "Three-finger triple tap",
    "Four-finger tap",
    "Four-finger double tap",
    "Four-finger triple tap"
  };

  /** The number under which a (part 1, part 2) pair is saved. second = 0 is the switch. */
  public static int key(int first, int second) {
    return BASE + first * 1000 + second;
  }

  public static boolean isFirstId(int gesture) {
    for (int id : FIRST_IDS) {
      if (id == gesture) {
        return true;
      }
    }
    return false;
  }

  public static String firstName(int gesture) {
    for (int i = 0; i < FIRST_IDS.length; i++) {
      if (FIRST_IDS[i] == gesture) {
        return FIRST_NAMES[i];
      }
    }
    return "Gesture " + gesture;
  }

  /** The foreground app's scheme first, then the global scheme. */
  static ExtensionGestures.Target lookup(Context c, int key) {
    ExtensionGestures.Target t = AppGestureSchemes.forForegroundApp(c, key);
    return t != null ? t : ExtensionGestures.get(c, key);
  }

  private static SharedPreferences prefs(Context c) {
    return SharedPreferencesUtils.getSharedPreferences(c);
  }

  public static boolean globalEnabled(Context c) {
    return prefs(c).getBoolean(PREF_USE, false);
  }

  public static long durationMs(Context c) {
    try {
      return Math.max(100, Long.parseLong(prefs(c).getString(PREF_TIME, "500")));
    } catch (RuntimeException e) {
      return 500;
    }
  }

  // ------------------------------------------------------------------ the engine

  private static final MultiPartCore CORE = new MultiPartCore();
  private static final Handler HANDLER = new Handler(Looper.getMainLooper());
  private static Runnable timeoutRunnable;

  private static final class ServiceHost implements MultiPartCore.Host {
    final TalkBackService s;
    final int displayId;

    ServiceHost(TalkBackService s, int displayId) {
      this.s = s;
      this.displayId = displayId;
    }

    @Override
    public boolean enabled() {
      return globalEnabled(s);
    }

    @Override
    public boolean isFirstPart(int gesture) {
      if (!isFirstId(gesture)) {
        return false;
      }
      ExtensionGestures.Target t = lookup(s, key(gesture, 0));
      return t != null && KIND_SWITCH.equals(t.kind);
    }

    @Override
    public boolean hasSecond(int first, int second) {
      ExtensionGestures.Target t = lookup(s, key(first, second));
      return t != null && !KIND_SWITCH.equals(t.kind);
    }

    @Override
    public void runSecond(int first, int second) {
      ExtensionGestures.Target t = lookup(s, key(first, second));
      if (t == null) {
        return;
      }
      if (KIND_TALKBACK.equals(t.kind)) {
        s.msRunTalkbackAction(t.name);
      } else {
        CustomVoiceCommands.run(s, new CustomVoiceCommands.Entry("", t.kind, t.name));
      }
    }

    @Override
    public void runNormal(int gesture) {
      s.msRunGestureNormally(displayId, gesture);
    }

    @Override
    public void soundFirstPart() {
      s.getFeedbackController().playAuditory(R.raw.gesture_end, Performance.EVENT_ID_UNTRACKED);
    }

    @Override
    public void soundSecondPart() {
      s.getFeedbackController().playAuditory(R.raw.gesture_begin, Performance.EVENT_ID_UNTRACKED);
    }

    @Override
    public void scheduleTimeout(long delayMs) {
      cancelTimeout();
      final ServiceHost self = this;
      timeoutRunnable = () -> CORE.onTimeout(self);
      HANDLER.postDelayed(timeoutRunnable, delayMs);
    }

    @Override
    public void cancelTimeout() {
      if (timeoutRunnable != null) {
        HANDLER.removeCallbacks(timeoutRunnable);
        timeoutRunnable = null;
      }
    }

    @Override
    public long durationMs() {
      return MsMultiPartGestures.durationMs(s);
    }
  }

  /**
   * Called for every gesture before anything else. True = multi-part took it (waiting for part 2,
   * or part 2 ran). False = handle the gesture as usual. Costs one preference read when the global
   * switch is off.
   */
  public static boolean intercept(TalkBackService s, int displayId, int gestureId) {
    if (!CORE.isWaiting() && !globalEnabled(s)) {
      return false;
    }
    try {
      return CORE.onGesture(new ServiceHost(s, displayId), gestureId);
    } catch (RuntimeException e) {
      CORE.reset(new ServiceHost(s, displayId));
      return false;
    }
  }

  /** Called when the service stops or is interrupted. */
  public static void reset(TalkBackService s) {
    CORE.reset(new ServiceHost(s, 0));
  }
}
