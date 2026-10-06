/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import static com.google.android.accessibility.talkback.focusmanagement.NavigationTarget.TARGET_CONTROL;
import static com.google.android.accessibility.talkback.focusmanagement.NavigationTarget.TARGET_HEADING;
import static com.google.android.accessibility.talkback.focusmanagement.NavigationTarget.TARGET_LINK;
import static com.google.android.accessibility.utils.monitor.InputModeTracker.INPUT_MODE_TOUCH;
import static com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_BACKWARD;
import static com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_FORWARD;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.TalkBackService;

/**
 * Group 1 of the remaining Jieshuo commands: navigation by heading/link/control, text movement by
 * character/word/sentence/paragraph, window switching, clipboard clear, network info. Called from
 * JxCommands.dispatch default branch. Not compiled yet.
 */
final class JxCommandsNav {
  private JxCommandsNav() {}

  /** Returns null when this class does not know the id (caller then returns false). */
  static Boolean run(
      LuaServiceApi api, Context ctx, TalkBackService s, String id, AccessibilityNodeInfo n) {
    switch (id) {
      case "command_to_next_section":
        return target(api, s, SEARCH_FOCUS_FORWARD, TARGET_HEADING);
      case "command_to_previous_section":
        return target(api, s, SEARCH_FOCUS_BACKWARD, TARGET_HEADING);
      case "command_to_next_link":
        return target(api, s, SEARCH_FOCUS_FORWARD, TARGET_LINK);
      case "command_to_previous_link":
        return target(api, s, SEARCH_FOCUS_BACKWARD, TARGET_LINK);
      case "command_to_next_control":
        return target(api, s, SEARCH_FOCUS_FORWARD, TARGET_CONTROL);
      case "command_to_previous_control":
        return target(api, s, SEARCH_FOCUS_BACKWARD, TARGET_CONTROL);

      case "command_next_window":
        return feedback(api, s, Feedback.nextWindow(INPUT_MODE_TOUCH).setDefaultToInputFocus(true));
      case "command_previous_window":
        return feedback(
            api, s, Feedback.previousWindow(INPUT_MODE_TOUCH).setDefaultToInputFocus(true));

      case "command_to_next_char":
        return move(n, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER, false);
      case "command_to_previous_char":
        return move(n, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER, true);
      case "command_to_next_word":
        return move(n, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD, false);
      case "command_to_previous_word":
        return move(n, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD, true);
      case "command_to_next_row":
        return move(n, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE, false);
      case "command_to_previous_row":
        return move(n, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE, true);
      case "command_to_next_paragraph":
        return move(n, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH, false);
      case "command_to_previous_paragraph":
        return move(n, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH, true);

      case "command_clean":
        return clearClipboard(ctx);
      case "command_speak_net":
        return speakNetwork(api, ctx);
      default:
        return null;
    }
  }

  static boolean target(LuaServiceApi api, TalkBackService s, int dir, int type) {
    return feedback(
        api,
        s,
        Feedback.focusDirection(dir)
            .setHtmlTargetType(type)
            .setInputMode(INPUT_MODE_TOUCH)
            .setWrap(false));
  }

  private static boolean feedback(
      LuaServiceApi api, final TalkBackService s, final Feedback.FocusDirection.Builder b) {
    api.postToUi(() -> s.msFocusFeedback(b));
    return true;
  }

  static boolean move(AccessibilityNodeInfo n, int unit, boolean back) {
    if (n == null) {
      return false;
    }
    Bundle b = new Bundle();
    b.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT, unit);
    b.putBoolean(AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN, false);
    return n.performAction(
        back
            ? AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
            : AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY,
        b);
  }

  private static boolean clearClipboard(Context ctx) {
    ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
    if (cm == null) {
      return false;
    }
    if (Build.VERSION.SDK_INT >= 28) {
      cm.clearPrimaryClip();
    } else {
      cm.setPrimaryClip(ClipData.newPlainText("", ""));
    }
    return true;
  }

  @SuppressWarnings("deprecation")
  private static boolean speakNetwork(LuaServiceApi api, Context ctx) {
    ConnectivityManager cm =
        (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo ni = cm == null ? null : cm.getActiveNetworkInfo();
    api.speak(ni == null || !ni.isConnected() ? "No network connection" : "Connected: " + ni.getTypeName());
    return true;
  }
}
