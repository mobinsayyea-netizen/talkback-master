/*
 * Copyright 2026 Advanced Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityNodeInfo;
import com.google.android.accessibility.talkback.TalkBackService;

/** Voice dictation into the focused text field: once, or again and again until switched off. */
final class MsDictation {
  private MsDictation() {}

  private static final Handler MAIN = new Handler(Looper.getMainLooper());
  private static final long CONTINUOUS_MAX_MS = 15 * 60 * 1000L;
  private static final int MAX_QUIET_ROUNDS = 3;

  private static boolean continuous;
  private static long stopAt;
  private static int quietRounds;

  /** The text field that has input focus now, or null. */
  private static AccessibilityNodeInfo inputField(TalkBackService s, AccessibilityNodeInfo hint) {
    if (hint != null && hint.refresh() && hint.isEditable()) {
      return hint;
    }
    AccessibilityNodeInfo root = s.getRootInActiveWindow();
    if (root == null) {
      return null;
    }
    AccessibilityNodeInfo f = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
    return f != null && f.isEditable() ? f : null;
  }

  /** Puts text at the cursor (replacing a selection) and leaves the cursor after it. */
  static boolean insert(AccessibilityNodeInfo node, String add) {
    CharSequence cur = node.getText();
    String t = cur == null ? "" : cur.toString();
    int a = node.getTextSelectionStart();
    int b = node.getTextSelectionEnd();
    if (a < 0 || b < 0 || a > t.length() || b > t.length()) {
      a = t.length();
      b = a;
    }
    int lo = Math.min(a, b);
    int hi = Math.max(a, b);
    String sep = lo > 0 && !Character.isWhitespace(t.charAt(lo - 1)) ? " " : "";
    String out = t.substring(0, lo) + sep + add + t.substring(hi);
    Bundle args = new Bundle();
    args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, out);
    boolean ok = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
    int pos = lo + sep.length() + add.length();
    Bundle sel = new Bundle();
    sel.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, pos);
    sel.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, pos);
    node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, sel);
    return ok;
  }

  /** Voice dictation: listens once and types what it heard. */
  static boolean once(
      final LuaServiceApi api, final Context ctx, final TalkBackService s, AccessibilityNodeInfo n) {
    if (inputField(s, n) == null) {
      api.speak("Focus a text field first");
      return false;
    }
    api.speak("Speak now");
    MAIN.postDelayed(
        () ->
            MsSpeechInput.listen(
                ctx,
                false,
                (text, message) -> {
                  AccessibilityNodeInfo f = inputField(s, null);
                  if (text == null) {
                    api.speak(String.valueOf(message));
                  } else if (f == null || !insert(f, text)) {
                    api.speak("Could not type: " + text);
                  }
                }),
        1000);
    return true;
  }

  /** Continuous voice dictation: on or off. It ends by itself after quiet rounds or 15 minutes. */
  static boolean toggleContinuous(
      final LuaServiceApi api, final Context ctx, final TalkBackService s, AccessibilityNodeInfo n) {
    if (continuous) {
      continuous = false;
      MsSpeechInput.stop();
      api.speak("Continuous dictation off");
      return true;
    }
    if (inputField(s, n) == null) {
      api.speak("Focus a text field first");
      return false;
    }
    continuous = true;
    quietRounds = 0;
    stopAt = System.currentTimeMillis() + CONTINUOUS_MAX_MS;
    api.speak("Continuous dictation on");
    MAIN.postDelayed(() -> round(api, ctx, s), 1500);
    return true;
  }

  private static void round(final LuaServiceApi api, final Context ctx, final TalkBackService s) {
    if (!continuous) {
      return;
    }
    if (System.currentTimeMillis() > stopAt) {
      continuous = false;
      api.speak("Continuous dictation stopped after 15 minutes");
      return;
    }
    MsSpeechInput.listen(
        ctx,
        false,
        (text, message) -> {
          if (!continuous) {
            return;
          }
          if (text == null) {
            if (++quietRounds >= MAX_QUIET_ROUNDS) {
              continuous = false;
              api.speak("Continuous dictation off, nothing heard");
              return;
            }
          } else {
            quietRounds = 0;
            AccessibilityNodeInfo f = inputField(s, null);
            if (f == null) {
              continuous = false;
              api.speak("Continuous dictation off, no text field");
              return;
            }
            insert(f, text);
          }
          MAIN.postDelayed(() -> round(api, ctx, s), 400);
        });
  }
}
