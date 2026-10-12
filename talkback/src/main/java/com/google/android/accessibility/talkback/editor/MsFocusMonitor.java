/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.content.Context;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * "Monitor the currently focused element": the app and the id of the element are remembered; when
 * its text changes later, the new text is spoken. Elements with an id are kept in a file and
 * survive a restart; elements without an id are watched by their place on the screen until the
 * service restarts. Not compiled yet.
 */
public final class MsFocusMonitor {
  private MsFocusMonitor() {}

  /** One watched element. */
  static final class Monitor {
    final String pkg;
    final String viewId; // may be empty
    final String bounds; // used when viewId is empty
    final String label;
    String lastText = "";

    Monitor(String pkg, String viewId, String bounds, String label) {
      this.pkg = pkg;
      this.viewId = viewId;
      this.bounds = bounds;
      this.label = label;
    }
  }

  private static final List<Monitor> LIST = new ArrayList<>();
  private static boolean loaded;

  private static File file(Context c) {
    return new File(LuaPaths.base(c), "Focus monitors.txt");
  }

  private static synchronized void ensureLoaded(Context c) {
    if (loaded) {
      return;
    }
    loaded = true;
    File f = file(c);
    if (!f.isFile()) {
      return;
    }
    try (BufferedReader r = new BufferedReader(new FileReader(f))) {
      String line;
      while ((line = r.readLine()) != null) {
        String[] p = line.split("\t", -1);
        if (p.length == 3 && !p[1].isEmpty()) {
          LIST.add(new Monitor(p[0], p[1], "", p[2]));
        }
      }
    } catch (IOException e) {
      // unreadable: start empty
    }
  }

  private static synchronized void save(Context c) {
    try (FileWriter w = new FileWriter(file(c))) {
      for (Monitor m : LIST) {
        if (!m.viewId.isEmpty()) {
          w.write(m.pkg + "\t" + m.viewId + "\t" + m.label.replace('\t', ' ').replace('\n', ' ') + "\n");
        }
      }
    } catch (IOException e) {
      // best effort
    }
  }

  private static String textOf(AccessibilityNodeInfo n) {
    CharSequence t = n.getText();
    if (t == null || t.length() == 0) {
      t = n.getContentDescription();
    }
    return t == null ? "" : t.toString().trim();
  }

  /** Starts watching the focused element. */
  static synchronized boolean addCurrent(LuaServiceApi api, Context c, AccessibilityNodeInfo n) {
    ensureLoaded(c);
    if (n == null) {
      api.speak("Nothing is focused");
      return false;
    }
    String pkg = n.getPackageName() == null ? "" : n.getPackageName().toString();
    String id = n.getViewIdResourceName() == null ? "" : n.getViewIdResourceName();
    Rect r = new Rect();
    n.getBoundsInScreen(r);
    if (pkg.isEmpty() || (id.isEmpty() && r.isEmpty())) {
      api.speak("This element cannot be watched");
      return false;
    }
    String label = textOf(n);
    if (label.isEmpty()) {
      label = id.isEmpty() ? "element" : id;
    }
    Monitor m = new Monitor(pkg, id, r.flattenToString(), label);
    m.lastText = textOf(n);
    LIST.add(m);
    save(c);
    api.speak("Watching " + label);
    return true;
  }

  static synchronized List<Monitor> snapshot(Context c) {
    ensureLoaded(c);
    return new ArrayList<>(LIST);
  }

  static synchronized void remove(Context c, Monitor m) {
    LIST.remove(m);
    save(c);
  }

  /** Called for every accessibility event. Cheap when nothing is watched. */
  public static void onEvent(Context c, AccessibilityEvent e) {
    try {
      int type = e.getEventType();
      if (type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
          && type != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
        return;
      }
      List<Monitor> watched;
      synchronized (MsFocusMonitor.class) {
        ensureLoaded(c);
        if (LIST.isEmpty()) {
          return;
        }
        watched = new ArrayList<>(LIST);
      }
      String pkg = e.getPackageName() == null ? "" : e.getPackageName().toString();
      AccessibilityNodeInfo src = e.getSource();
      if (src == null) {
        return;
      }
      for (Monitor m : watched) {
        if (!m.pkg.equals(pkg)) {
          continue;
        }
        List<AccessibilityNodeInfo> hits = new ArrayList<>();
        if (!m.viewId.isEmpty()) {
          if (m.viewId.equals(src.getViewIdResourceName())) {
            hits.add(src);
          }
          List<AccessibilityNodeInfo> found = src.findAccessibilityNodeInfosByViewId(m.viewId);
          if (found != null) {
            hits.addAll(found);
          }
        } else {
          Rect r = new Rect();
          src.getBoundsInScreen(r);
          if (r.flattenToString().equals(m.bounds)) {
            hits.add(src);
          }
        }
        for (AccessibilityNodeInfo h : hits) {
          String t = textOf(h);
          if (!t.isEmpty() && !t.equals(m.lastText)) {
            m.lastText = t;
            LuaServiceApi.speakStatic(t);
            break;
          }
        }
      }
    } catch (RuntimeException ignored) {
      // never disturb the screen reader because of a watcher
    }
  }
}
