/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import com.google.android.accessibility.talkback.TalkBackService;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Step 39: the Jieshuo navigation commands that had no handler yet: previous/next window, heading,
 * link, control, character, word, sentence, paragraph; speak network information; clear. Written
 * only on top of the public Android accessibility API (node tree walk and text break iterators).
 * Every handler returns false instead of throwing.
 */
final class MsNavCommands {
  private MsNavCommands() {}

  private static final int HEADING = 0;
  private static final int LINK = 1;
  private static final int CONTROL = 2;
  private static final int NODE_BUDGET = 3000;

  static void register(Map<String, JxCommandTable.Handler> m) {
    m.put("command_clean", (api, ctx, s, n) -> MsMenus.clearText(api, s, n));
    m.put("command_speak_net", (api, ctx, s, n) -> speakNet(api, ctx));
    m.put("command_next_window", (api, ctx, s, n) -> window(api, s, n, true));
    m.put("command_previous_window", (api, ctx, s, n) -> window(api, s, n, false));
    m.put("command_to_next_section", (api, ctx, s, n) -> step(api, s, n, HEADING, true));
    m.put("command_to_previous_section", (api, ctx, s, n) -> step(api, s, n, HEADING, false));
    m.put("command_to_next_link", (api, ctx, s, n) -> step(api, s, n, LINK, true));
    m.put("command_to_previous_link", (api, ctx, s, n) -> step(api, s, n, LINK, false));
    m.put("command_to_next_control", (api, ctx, s, n) -> step(api, s, n, CONTROL, true));
    m.put("command_to_previous_control", (api, ctx, s, n) -> step(api, s, n, CONTROL, false));
    m.put("command_to_next_char", (api, ctx, s, n) -> text(api, s, n, Unit.CHAR, true));
    m.put("command_to_previous_char", (api, ctx, s, n) -> text(api, s, n, Unit.CHAR, false));
    m.put("command_to_next_word", (api, ctx, s, n) -> text(api, s, n, Unit.WORD, true));
    m.put("command_to_previous_word", (api, ctx, s, n) -> text(api, s, n, Unit.WORD, false));
    m.put("command_to_next_row", (api, ctx, s, n) -> text(api, s, n, Unit.SENTENCE, true));
    m.put("command_to_previous_row", (api, ctx, s, n) -> text(api, s, n, Unit.SENTENCE, false));
    m.put("command_to_next_paragraph", (api, ctx, s, n) -> text(api, s, n, Unit.PARAGRAPH, true));
    m.put("command_to_previous_paragraph", (api, ctx, s, n) -> text(api, s, n, Unit.PARAGRAPH, false));
  }

  // ------------------------------------------------------------------ network

  private static boolean speakNet(LuaServiceApi api, Context ctx) {
    ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
    if (cm == null) {
      api.speak("Network information is not available");
      return false;
    }
    Network active = cm.getActiveNetwork();
    NetworkCapabilities caps = active == null ? null : cm.getNetworkCapabilities(active);
    if (caps == null) {
      api.speak("No network connection");
      return true;
    }
    StringBuilder sb = new StringBuilder();
    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
      sb.append("Wi-Fi");
      try {
        WifiManager wm = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wi = wm == null ? null : wm.getConnectionInfo();
        if (wi != null) {
          String ssid = wi.getSSID();
          if (ssid != null && !ssid.contains("unknown ssid")) {
            sb.append(' ').append(ssid.replace("\"", ""));
          }
          int rssi = wi.getRssi();
          if (rssi > -127) {
            sb.append(", signal ").append(WifiManager.calculateSignalLevel(rssi, 5)).append(" of 4");
          }
        }
      } catch (RuntimeException ignored) {
        // Wi-Fi details are optional; the connection type is already in the text.
      }
    } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
      sb.append("Mobile data");
      try {
        TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        String op = tm == null ? null : tm.getNetworkOperatorName();
        if (op != null && !op.isEmpty()) {
          sb.append(' ').append(op);
        }
      } catch (RuntimeException ignored) {
        // Operator name is optional.
      }
    } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
      sb.append("Ethernet");
    } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
      sb.append("VPN");
    } else {
      sb.append("Network");
    }
    sb.append(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        ? ", internet available" : ", no internet access");
    if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == false) {
      sb.append(", metered");
    }
    api.speak(sb.toString());
    return true;
  }

  // ------------------------------------------------------------------ windows

  private static boolean window(LuaServiceApi api, TalkBackService s, AccessibilityNodeInfo cur, boolean forward) {
    List<AccessibilityWindowInfo> all = s.getWindows();
    List<AccessibilityWindowInfo> usable = new ArrayList<>();
    if (all != null) {
      for (AccessibilityWindowInfo w : all) {
        if (w != null && w.getRoot() != null) {
          usable.add(w);
        }
      }
    }
    if (usable.size() < 2) {
      api.speak("Only one screen area");
      return false;
    }
    int curId = cur == null ? -1 : cur.getWindowId();
    int idx = -1;
    for (int i = 0; i < usable.size(); i++) {
      if (usable.get(i).getId() == curId) {
        idx = i;
        break;
      }
    }
    int size = usable.size();
    int next = idx < 0 ? 0 : (idx + (forward ? 1 : size - 1)) % size;
    AccessibilityWindowInfo target = usable.get(next);
    AccessibilityNodeInfo first = firstFocusable(target.getRoot());
    if (first == null || !first.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)) {
      api.speak("Could not move to the screen area");
      return false;
    }
    CharSequence title = target.getTitle();
    if (title != null && title.length() > 0) {
      api.speak(title + ", screen area " + (next + 1) + " of " + size);
    }
    return true;
  }

  private static AccessibilityNodeInfo firstFocusable(AccessibilityNodeInfo root) {
    List<AccessibilityNodeInfo> flat = new ArrayList<>();
    flatten(root, flat, new int[] {NODE_BUDGET});
    for (AccessibilityNodeInfo n : flat) {
      if (n.isVisibleToUser() && (hasText(n) || n.isClickable() || n.isFocusable())) {
        return n;
      }
    }
    return null;
  }

  // ------------------------------------------------------------------ heading / link / control

  private static boolean step(LuaServiceApi api, TalkBackService s, AccessibilityNodeInfo cur, int kind, boolean forward) {
    AccessibilityNodeInfo root = s.getRootInActiveWindow();
    if (root == null) {
      return false;
    }
    List<AccessibilityNodeInfo> flat = new ArrayList<>();
    flatten(root, flat, new int[] {NODE_BUDGET});
    int idx = -1;
    if (cur != null) {
      for (int i = 0; i < flat.size(); i++) {
        if (flat.get(i).equals(cur)) {
          idx = i;
          break;
        }
      }
    }
    int i = idx;
    while (true) {
      i += forward ? 1 : -1;
      if (i < 0 || i >= flat.size()) {
        break;
      }
      AccessibilityNodeInfo c = flat.get(i);
      if (c.isVisibleToUser() && matches(c, kind)
          && c.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)) {
        return true;
      }
    }
    api.speak((forward ? "No next " : "No previous ") + (kind == HEADING ? "heading" : kind == LINK ? "link" : "control"));
    return false;
  }

  private static boolean matches(AccessibilityNodeInfo n, int kind) {
    switch (kind) {
      case HEADING:
        return Build.VERSION.SDK_INT >= 28 && n.isHeading();
      case LINK:
        return isLink(n);
      default:
        return isControl(n);
    }
  }

  private static boolean isLink(AccessibilityNodeInfo n) {
    // The framework class has no getRoleDescription(); the compat library stores it in the extras.
    Bundle extras = n.getExtras();
    CharSequence role = extras == null ? null : extras.getCharSequence("AccessibilityNodeInfo.roleDescription");
    if (role != null && role.toString().toLowerCase(Locale.ROOT).contains("link")) {
      return true;
    }
    CharSequence t = n.getText();
    if (t instanceof Spanned) {
      Spanned sp = (Spanned) t;
      return sp.getSpans(0, sp.length(), ClickableSpan.class).length > 0;
    }
    return false;
  }

  private static boolean isControl(AccessibilityNodeInfo n) {
    if (n.isEditable() || n.isCheckable()) {
      return true;
    }
    CharSequence cls = n.getClassName();
    String c = cls == null ? "" : cls.toString();
    boolean widget = c.endsWith("Button") || c.endsWith("CheckBox") || c.endsWith("RadioButton")
        || c.endsWith("Switch") || c.endsWith("Spinner") || c.endsWith("SeekBar")
        || c.endsWith("EditText") || c.endsWith("ToggleButton");
    return widget || (n.isClickable() && hasText(n) && n.getChildCount() == 0);
  }

  private static boolean hasText(AccessibilityNodeInfo n) {
    CharSequence t = n.getText();
    CharSequence d = n.getContentDescription();
    return (t != null && t.length() > 0) || (d != null && d.length() > 0);
  }

  private static void flatten(AccessibilityNodeInfo n, List<AccessibilityNodeInfo> out, int[] budget) {
    if (n == null || budget[0] <= 0) {
      return;
    }
    budget[0]--;
    out.add(n);
    int count = n.getChildCount();
    for (int i = 0; i < count; i++) {
      flatten(n.getChild(i), out, budget);
    }
  }

  // ------------------------------------------------------------------ text units

  private enum Unit { CHAR, WORD, SENTENCE, PARAGRAPH }

  // Reading position inside the last node used (between characters, 0..length).
  private static String lastKey = "";
  private static int lastPos = 0;

  private static boolean text(LuaServiceApi api, TalkBackService s, AccessibilityNodeInfo given, Unit unit, boolean forward) {
    AccessibilityNodeInfo n = given;
    if (n == null) {
      return false;
    }
    CharSequence cs = n.getText();
    if (cs == null || cs.length() == 0) {
      cs = n.getContentDescription();
    }
    if (cs == null || cs.length() == 0) {
      api.speak("No text");
      return false;
    }
    String text = cs.toString();
    int len = text.length();
    boolean editable = n.isEditable();
    String key = n.getWindowId() + ":" + n.hashCode() + ":" + len + ":" + text.hashCode();
    int pos;
    if (editable) {
      pos = Math.max(0, Math.min(len, n.getTextSelectionEnd()));
    } else if (key.equals(lastKey)) {
      pos = lastPos;
    } else {
      pos = forward ? 0 : len;
    }
    int[] seg = forward ? nextSegment(text, pos, unit) : previousSegment(text, pos, unit);
    if (seg == null) {
      api.speak(forward ? "End of text" : "Start of text");
      return false;
    }
    int start = seg[0];
    int end = seg[1];
    int newPos = forward ? end : start;
    lastKey = key;
    lastPos = newPos;
    if (editable) {
      Bundle b = new Bundle();
      b.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, newPos);
      b.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, newPos);
      n.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, b);
    }
    api.speak(spoken(text.substring(start, end), unit));
    return true;
  }

  private static String spoken(String piece, Unit unit) {
    if (unit == Unit.CHAR) {
      if (piece.equals(" ")) {
        return "space";
      }
      if (piece.equals("\n")) {
        return "new line";
      }
      if (piece.equals("\t")) {
        return "tab";
      }
    }
    String t = piece.trim();
    return t.isEmpty() ? "blank" : t;
  }

  /** Next segment that starts at or after {@code pos}; null at the end. */
  static int[] nextSegment(String text, int pos, Unit unit) {
    int len = text.length();
    if (pos >= len) {
      return null;
    }
    if (unit == Unit.CHAR) {
      int e = text.offsetByCodePoints(pos, 1);
      return new int[] {pos, e};
    }
    if (unit == Unit.PARAGRAPH) {
      int s = pos;
      while (s < len && text.charAt(s) == '\n') {
        s++;
      }
      if (s >= len) {
        return null;
      }
      int e = text.indexOf('\n', s);
      return new int[] {s, e < 0 ? len : e};
    }
    BreakIterator it = unit == Unit.WORD ? BreakIterator.getWordInstance(Locale.getDefault())
        : BreakIterator.getSentenceInstance(Locale.getDefault());
    it.setText(text);
    int start = it.isBoundary(pos) ? pos : it.following(pos);
    while (start != BreakIterator.DONE && start < len) {
      int end = it.following(start);
      if (end == BreakIterator.DONE) {
        end = len;
      }
      if (hasLetterOrDigit(text, start, end)) {
        return new int[] {start, end};
      }
      start = end;
    }
    return null;
  }

  /** Previous segment that ends at or before {@code pos}; null at the start. */
  static int[] previousSegment(String text, int pos, Unit unit) {
    if (pos <= 0) {
      return null;
    }
    int len = text.length();
    pos = Math.min(pos, len);
    if (unit == Unit.CHAR) {
      int s = text.offsetByCodePoints(pos, -1);
      return new int[] {s, pos};
    }
    if (unit == Unit.PARAGRAPH) {
      int e = pos;
      while (e > 0 && text.charAt(e - 1) == '\n') {
        e--;
      }
      if (e <= 0) {
        return null;
      }
      int s = text.lastIndexOf('\n', e - 1) + 1;
      return new int[] {s, e};
    }
    BreakIterator it = unit == Unit.WORD ? BreakIterator.getWordInstance(Locale.getDefault())
        : BreakIterator.getSentenceInstance(Locale.getDefault());
    it.setText(text);
    int end = it.isBoundary(pos) ? pos : it.preceding(pos);
    while (end != BreakIterator.DONE && end > 0) {
      int start = it.preceding(end);
      if (start == BreakIterator.DONE) {
        start = 0;
      }
      if (hasLetterOrDigit(text, start, end)) {
        return new int[] {start, end};
      }
      end = start;
    }
    return null;
  }

  private static boolean hasLetterOrDigit(String t, int s, int e) {
    for (int i = s; i < e; i++) {
      if (Character.isLetterOrDigit(t.charAt(i))) {
        return true;
      }
    }
    return false;
  }
}
