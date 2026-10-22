/*
 * Copyright 2026 Advanced Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure Java (no Android) text format of the per-application gesture schemes, so it can be tested on
 * a computer. One preference string:
 *
 * <pre>
 * \@com.whatsapp TAB on
 * 9 TAB ext TAB Copy text
 * 5 TAB tool TAB Recorder
 * \@com.google.android.youtube TAB off
 * </pre>
 *
 * A line starting with "@" starts a scheme (package name, then "on" or "off"). The lines below it
 * are "gestureId TAB kind TAB name" (kind is "ext" or "tool"). A scheme with no gesture lines is
 * still kept, so a scheme created but not filled in yet shows in the list.
 */
final class AppGestureSchemeData {
  private AppGestureSchemeData() {}

  /** One scheme: the app, whether it is turned on, and its gestures. */
  static final class Scheme {
    final String pkg;
    boolean enabled = true;
    final Map<Integer, String[]> gestures = new LinkedHashMap<>(); // id -> {kind, name}

    Scheme(String pkg) {
      this.pkg = pkg;
    }
  }

  static String clean(String s) {
    return s == null ? "" : s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim();
  }

  static Map<String, Scheme> parse(String text) {
    Map<String, Scheme> out = new LinkedHashMap<>();
    if (text == null) {
      return out;
    }
    Scheme cur = null;
    for (String line : text.split("\n")) {
      if (line.isEmpty()) {
        continue;
      }
      if (line.charAt(0) == '@') {
        String[] p = line.substring(1).split("\t", -1);
        String pkg = p[0].trim();
        if (pkg.isEmpty()) {
          cur = null;
          continue;
        }
        cur = new Scheme(pkg);
        cur.enabled = !(p.length > 1 && "off".equals(p[1].trim()));
        out.put(pkg, cur);
      } else if (cur != null) {
        String[] p = line.split("\t", -1);
        if (p.length == 3) {
          try {
            cur.gestures.put(Integer.parseInt(p[0].trim()), new String[] {p[1], p[2]});
          } catch (NumberFormatException ignored) {
            // skip a broken line
          }
        }
      }
    }
    return out;
  }

  static String format(Map<String, Scheme> all) {
    StringBuilder sb = new StringBuilder();
    for (Scheme s : all.values()) {
      sb.append('@').append(s.pkg).append('\t').append(s.enabled ? "on" : "off").append('\n');
      for (Map.Entry<Integer, String[]> e : s.gestures.entrySet()) {
        sb.append(e.getKey())
            .append('\t')
            .append(clean(e.getValue()[0]))
            .append('\t')
            .append(clean(e.getValue()[1]))
            .append('\n');
      }
    }
    return sb.toString();
  }

  /** The target on this gesture, or null when the app has no scheme, it is off, or the gesture is free. */
  static String[] lookup(Map<String, Scheme> all, String pkg, int gestureId) {
    Scheme s = all.get(pkg);
    if (s == null || !s.enabled) {
      return null;
    }
    return s.gestures.get(gestureId);
  }

  static boolean anyEnabled(Map<String, Scheme> all) {
    for (Scheme s : all.values()) {
      if (s.enabled && !s.gestures.isEmpty()) {
        return true;
      }
    }
    return false;
  }

  /** Renames an extension or tool in every scheme. Returns true when something changed. */
  static boolean rename(Map<String, Scheme> all, String kind, String oldName, String newName) {
    boolean changed = false;
    for (Scheme s : all.values()) {
      for (Map.Entry<Integer, String[]> e : s.gestures.entrySet()) {
        if (e.getValue()[0].equals(kind) && e.getValue()[1].equals(oldName)) {
          e.setValue(new String[] {kind, newName});
          changed = true;
        }
      }
    }
    return changed;
  }

  /** Removes an extension or tool from every scheme. The scheme itself stays. */
  static boolean remove(Map<String, Scheme> all, String kind, String name) {
    boolean changed = false;
    for (Scheme s : all.values()) {
      List<Integer> drop = new ArrayList<>();
      for (Map.Entry<Integer, String[]> e : s.gestures.entrySet()) {
        if (e.getValue()[0].equals(kind) && e.getValue()[1].equals(name)) {
          drop.add(e.getKey());
        }
      }
      for (Integer id : drop) {
        s.gestures.remove(id);
        changed = true;
      }
    }
    return changed;
  }
}
