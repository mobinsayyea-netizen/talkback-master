/*
 * Copyright 2026 Advanced Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Assigns an extension or a tool to a one-finger swipe gesture. Saved as text in the preference
 * "ms_ext_gestures": lines "gestureId TAB kind TAB name". A gesture with an assignment is taken
 * from TalkBack BEFORE its own action runs, and the script runs on its own thread. The script's
 * return value is not used yet (Jieshuo lets "false" fall back to the default gesture).
 */
public final class ExtensionGestures {
  private ExtensionGestures() {}

  static final String KEY = "ms_ext_gestures";

  /** AccessibilityService GESTURE_* ids 1 to 16 (the 16 one-finger swipes) and their names. */
  public static final int[] IDS = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

  public static final String[] NAMES = {
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
    "Swipe down then right"
  };

  /** One assignment. */
  public static final class Target {
    public final String kind;
    public final String name;

    Target(String kind, String name) {
      this.kind = kind;
      this.name = name;
    }
  }

  private static SharedPreferences prefs(Context c) {
    return SharedPreferencesUtils.getSharedPreferences(c);
  }

  public static String nameOf(int gestureId) {
    for (int i = 0; i < IDS.length; i++) {
      if (IDS[i] == gestureId) {
        return NAMES[i];
      }
    }
    return "Gesture " + gestureId;
  }

  static Map<Integer, Target> load(Context c) {
    Map<Integer, Target> m = new LinkedHashMap<>();
    String text = prefs(c).getString(KEY, "");
    if (text == null) {
      return m;
    }
    for (String line : text.split("\n")) {
      String[] p = line.split("\t", -1);
      if (p.length == 3) {
        try {
          m.put(Integer.parseInt(p[0].trim()), new Target(p[1], p[2]));
        } catch (NumberFormatException ignored) {
          // skip a broken line
        }
      }
    }
    return m;
  }

  private static void save(Context c, Map<Integer, Target> m) {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<Integer, Target> e : m.entrySet()) {
      sb.append(e.getKey()).append('\t').append(e.getValue().kind).append('\t')
          .append(e.getValue().name).append('\n');
    }
    prefs(c).edit().putString(KEY, sb.toString()).apply();
  }

  public static Target get(Context c, int gestureId) {
    return load(c).get(gestureId);
  }

  public static void set(Context c, int gestureId, String kind, String name) {
    Map<Integer, Target> m = load(c);
    m.put(gestureId, new Target(kind, name));
    save(c, m);
  }

  public static void clear(Context c, int gestureId) {
    Map<Integer, Target> m = load(c);
    m.remove(gestureId);
    save(c, m);
  }

  /** Keeps assignments when an extension or tool is renamed. */
  static void renameFor(Context c, String kind, String oldName, String newName) {
    Map<Integer, Target> m = load(c);
    for (Map.Entry<Integer, Target> e : m.entrySet()) {
      if (e.getValue().kind.equals(kind) && e.getValue().name.equals(oldName)) {
        e.setValue(new Target(kind, newName));
      }
    }
    save(c, m);
    AppGestureSchemes.renameFor(c, kind, oldName, newName);
  }

  static void removeFor(Context c, String kind, String name) {
    Map<Integer, Target> m = load(c);
    java.util.Iterator<Map.Entry<Integer, Target>> it = m.entrySet().iterator();
    while (it.hasNext()) {
      Target t = it.next().getValue();
      if (t.kind.equals(kind) && t.name.equals(name)) {
        it.remove();
      }
    }
    save(c, m);
    AppGestureSchemes.removeFor(c, kind, name);
  }

  /** Called by the service for every gesture. Returns true when an extension took the gesture. */
  public static boolean handle(Context c, int gestureId) {
    // The scheme of the app in the foreground comes first, then the global scheme.
    Target t = AppGestureSchemes.forForegroundApp(c, gestureId);
    if (t == null) {
      t = get(c, gestureId);
    }
    if (t == null) {
      return false;
    }
    com.google.android.accessibility.talkback.MsErrorLog.crumb(
        "gesture " + nameOf(gestureId) + " -> " + t.kind + " " + t.name);
    CustomVoiceCommands.run(c, new CustomVoiceCommands.Entry("", t.kind, t.name));
    return true;
  }
}
