/*
 * Copyright 2026 Advanced Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.accessibility.AccessibilityNodeInfo;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Per-application gesture schemes (like Jieshuo's "per-application gesture scheme"). The global
 * scheme is {@link ExtensionGestures}. A scheme of an app only holds the gestures that app changes;
 * every other gesture in that app still uses the global scheme. Saved as text in the preference
 * "ms_app_gesture_schemes" (format: see {@link AppGestureSchemeData}). Not compiled yet.
 */
public final class AppGestureSchemes {
  private AppGestureSchemes() {}

  static final String KEY = "ms_app_gesture_schemes";

  private static SharedPreferences prefs(Context c) {
    return SharedPreferencesUtils.getSharedPreferences(c);
  }

  static Map<String, AppGestureSchemeData.Scheme> load(Context c) {
    return AppGestureSchemeData.parse(prefs(c).getString(KEY, ""));
  }

  private static void save(Context c, Map<String, AppGestureSchemeData.Scheme> all) {
    prefs(c).edit().putString(KEY, AppGestureSchemeData.format(all)).apply();
  }

  /** Package names of all schemes, in creation order. */
  static List<String> packages(Context c) {
    return new ArrayList<>(load(c).keySet());
  }

  static AppGestureSchemeData.Scheme scheme(Context c, String pkg) {
    return load(c).get(pkg);
  }

  /** Creates an empty scheme (turned on). Does nothing when one exists. */
  static void create(Context c, String pkg) {
    Map<String, AppGestureSchemeData.Scheme> all = load(c);
    if (!all.containsKey(pkg)) {
      all.put(pkg, new AppGestureSchemeData.Scheme(pkg));
      save(c, all);
    }
  }

  static void delete(Context c, String pkg) {
    Map<String, AppGestureSchemeData.Scheme> all = load(c);
    if (all.remove(pkg) != null) {
      save(c, all);
    }
  }

  static void setEnabled(Context c, String pkg, boolean enabled) {
    Map<String, AppGestureSchemeData.Scheme> all = load(c);
    AppGestureSchemeData.Scheme s = all.get(pkg);
    if (s != null) {
      s.enabled = enabled;
      save(c, all);
    }
  }

  static ExtensionGestures.Target get(Context c, String pkg, int gestureId) {
    String[] t = AppGestureSchemeData.lookup(load(c), pkg, gestureId);
    if (t == null) {
      // Not "lookup": the editor also shows gestures of a scheme that is turned off.
      AppGestureSchemeData.Scheme s = load(c).get(pkg);
      t = s == null ? null : s.gestures.get(gestureId);
    }
    return t == null ? null : new ExtensionGestures.Target(t[0], t[1]);
  }

  static void set(Context c, String pkg, int gestureId, String kind, String name) {
    Map<String, AppGestureSchemeData.Scheme> all = load(c);
    AppGestureSchemeData.Scheme s = all.get(pkg);
    if (s == null) {
      s = new AppGestureSchemeData.Scheme(pkg);
      all.put(pkg, s);
    }
    s.gestures.put(gestureId, new String[] {kind, name});
    save(c, all);
  }

  static void clear(Context c, String pkg, int gestureId) {
    Map<String, AppGestureSchemeData.Scheme> all = load(c);
    AppGestureSchemeData.Scheme s = all.get(pkg);
    if (s != null && s.gestures.remove(gestureId) != null) {
      save(c, all);
    }
  }

  static void renameFor(Context c, String kind, String oldName, String newName) {
    Map<String, AppGestureSchemeData.Scheme> all = load(c);
    if (AppGestureSchemeData.rename(all, kind, oldName, newName)) {
      save(c, all);
    }
  }

  static void removeFor(Context c, String kind, String name) {
    Map<String, AppGestureSchemeData.Scheme> all = load(c);
    if (AppGestureSchemeData.remove(all, kind, name)) {
      save(c, all);
    }
  }

  /** Package of the app in the foreground, or null. */
  static String foregroundPackage(Context c) {
    if (!(c instanceof AccessibilityService)) {
      return null;
    }
    try {
      AccessibilityNodeInfo root = ((AccessibilityService) c).getRootInActiveWindow();
      if (root == null) {
        return null;
      }
      CharSequence p = root.getPackageName();
      return p == null ? null : p.toString();
    } catch (RuntimeException e) {
      return null;
    }
  }

  /**
   * The target a gesture has in the app that is in the foreground now, or null (use the global
   * scheme). Costs almost nothing when no scheme with a gesture is turned on.
   */
  static ExtensionGestures.Target forForegroundApp(Context c, int gestureId) {
    Map<String, AppGestureSchemeData.Scheme> all = load(c);
    if (!AppGestureSchemeData.anyEnabled(all)) {
      return null;
    }
    String pkg = foregroundPackage(c);
    if (pkg == null) {
      return null;
    }
    String[] t = AppGestureSchemeData.lookup(all, pkg, gestureId);
    return t == null ? null : new ExtensionGestures.Target(t[0], t[1]);
  }

  static String label(Context c, String pkg) {
    try {
      PackageManager pm = c.getPackageManager();
      ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
      CharSequence l = pm.getApplicationLabel(ai);
      return l == null ? pkg : l.toString();
    } catch (PackageManager.NameNotFoundException | RuntimeException e) {
      return pkg;
    }
  }
}
