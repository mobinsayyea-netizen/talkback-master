/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Pure Java (no Android): turns Jieshuo's gesture files into assignments for MS's gesture schemes
 * ({@link ExtensionGestures} global scheme, {@link AppGestureSchemes} per app).
 *
 * <p>Jieshuo's Gestures/&lt;scheme&gt;/config is a JSON object "gesture name" -> "Function/Go back",
 * "Plugins/My extension", "Tools/My tool", "Application/WhatsApp" or "Default". MS schemes know the 16
 * one-finger swipes (ids 1 to 16) and the multi-part pairs (see {@link MsMultiPartGestures}). Gestures
 * MS has no slot for (taps, two-, three-, four-finger swipes, long swipes, edge gestures) are listed as
 * skipped, never guessed.
 */
final class JxGestureImport {
  private JxGestureImport() {}

  /** What the Android side must answer; everything else here is plain logic. */
  interface Resolver {
    /** Name that service.execute accepts for this Jieshuo function name, or null. */
    String command(String name);

    /** Real folder name of the restored extension (trim and case ignored), or null. */
    String extension(String name);

    String tool(String name);

    /** Package of the installed app with this label, or null. */
    String appPackage(String appName);
  }

  static final class Assign {
    final String scheme; // null = global scheme, else package name
    final int id;
    final String kind; // ext, tool, cmd, mp
    final String name;

    Assign(String scheme, int id, String kind, String name) {
      this.scheme = scheme;
      this.id = id;
      this.kind = kind;
      this.name = name;
    }
  }

  static final class Plan {
    final List<Assign> assigns = new ArrayList<>();
    /** extension folder name -> app label: small extensions that open an app (service.startApp). */
    final Map<String, String> launchers = new LinkedHashMap<>();
    /** package -> scheme turned on (true) or off (false). */
    final Map<String, Boolean> schemes = new LinkedHashMap<>();
    final List<String> skipped = new ArrayList<>();
    int global;
    int perApp;
    int multiPart;
  }

  /** Jieshuo's gesture names in the order of the Android GESTURE_ ids 1 to 16. */
  static final String[] SWIPES = {
    "up", "down", "left", "right", "left_right", "right_left", "up_down", "down_up",
    "left_up", "left_down", "right_up", "right_down", "up_left", "up_right", "down_left", "down_right"
  };

  static int swipeId(String name) {
    for (int i = 0; i < SWIPES.length; i++) {
      if (SWIPES[i].equals(name)) {
        return i + 1;
      }
    }
    return 0;
  }

  /** Same number as MsMultiPartGestures.key(first, second). */
  static int multiKey(int first, int second) {
    return 100000 + first * 1000 + second;
  }

  /** {kind, name} for a Jieshuo target text, or null with the reason in {@code why[0]}. */
  static String[] target(String value, Resolver r, Plan plan, String[] why) {
    String v = value == null ? "" : value.trim();
    if (v.isEmpty() || v.equalsIgnoreCase("Default") || v.equalsIgnoreCase("No command")) {
      why[0] = null;
      return null;
    }
    int slash = v.indexOf('/');
    String head = slash > 0 ? v.substring(0, slash).trim().toLowerCase(Locale.ROOT) : "";
    String rest = slash > 0 ? v.substring(slash + 1).trim() : v;
    if (head.equals("plugins") || head.equals("extensions") || head.equals("extension")) {
      String e = r.extension(rest);
      if (e == null) {
        why[0] = "extension \"" + rest + "\" was not found";
        return null;
      }
      return new String[] {"ext", e};
    }
    if (head.equals("tools") || head.equals("tool")) {
      String t = r.tool(rest);
      if (t == null) {
        why[0] = "tool \"" + rest + "\" was not found";
        return null;
      }
      return new String[] {"tool", t};
    }
    if (head.equals("application") || head.equals("app")) {
      if (r.appPackage(rest) == null) {
        why[0] = "app \"" + rest + "\" is not installed";
        return null;
      }
      String ext = "Open " + rest;
      plan.launchers.put(ext, rest);
      return new String[] {"ext", ext};
    }
    String fn = head.equals("function") || head.equals("functions") ? rest : v;
    String c = r.command(fn);
    if (c == null && fn.startsWith("[") && fn.endsWith("]")) {
      c = r.command(fn.substring(1, fn.length() - 1));
    }
    if (c == null) {
      why[0] = "function \"" + fn + "\" is not in MS";
      return null;
    }
    return new String[] {"cmd", c};
  }

  private static void addConfig(
      Map<String, String> config, String scheme, String label, Resolver r, Plan plan) {
    for (Map.Entry<String, String> e : config.entrySet()) {
      String value = e.getValue();
      if (value == null || value.trim().isEmpty() || value.trim().equalsIgnoreCase("Default")) {
        continue;
      }
      int id = swipeId(e.getKey());
      if (id == 0) {
        plan.skipped.add(label + ": gesture \"" + e.getKey() + "\" (" + value + ") has no slot in MS");
        continue;
      }
      String[] why = new String[1];
      String[] t = target(value, r, plan, why);
      if (t == null) {
        if (why[0] != null) {
          plan.skipped.add(label + ": " + e.getKey() + " -> " + why[0]);
        }
        continue;
      }
      plan.assigns.add(new Assign(scheme, id, t[0], t[1]));
      if (scheme == null) {
        plan.global++;
      } else {
        plan.perApp++;
      }
    }
  }

  /**
   * @param global the config of the scheme named in Jieshuo's "gesture_package" (may be empty)
   * @param perApp app label -> that app's config
   * @param settings the Jieshuo settings (multi-part gestures, which app schemes are on)
   */
  static Plan plan(
      Map<String, String> global,
      String globalName,
      Map<String, Map<String, String>> perApp,
      Map<String, Object> settings,
      Resolver r) {
    Plan plan = new Plan();
    addConfig(global, null, "Scheme " + globalName, r, plan);
    Object on = settings.get("app_gesture_package");
    Set<?> enabledApps = on instanceof Set ? (Set<?>) on : null;
    for (Map.Entry<String, Map<String, String>> e : perApp.entrySet()) {
      String pkg = r.appPackage(e.getKey());
      if (pkg == null) {
        plan.skipped.add("App scheme \"" + e.getKey() + "\": app is not installed");
        continue;
      }
      addConfig(e.getValue(), pkg, "App " + e.getKey(), r, plan);
      plan.schemes.put(pkg, enabledApps == null || enabledApps.contains(e.getKey()));
    }
    for (String first : SWIPES) {
      int f = swipeId(first);
      Object sw = settings.get("use_" + first + "_double_fling_gesture");
      boolean any = false;
      for (String second : new String[] {"up", "down", "left", "right"}) {
        Object v = settings.get(first + "_double_fling_" + second + "_gesture");
        if (!(v instanceof String)) {
          continue;
        }
        String[] why = new String[1];
        String[] t = target((String) v, r, plan, why);
        if (t == null) {
          if (why[0] != null) {
            plan.skipped.add("Multi-part " + first + " then " + second + " -> " + why[0]);
          }
          continue;
        }
        plan.assigns.add(new Assign(null, multiKey(f, swipeId(second)), t[0], t[1]));
        plan.multiPart++;
        any = true;
      }
      if (Boolean.TRUE.equals(sw)) {
        plan.assigns.add(new Assign(null, multiKey(f, 0), "mp", "on"));
        any = true;
      }
    }
    return plan;
  }
}
