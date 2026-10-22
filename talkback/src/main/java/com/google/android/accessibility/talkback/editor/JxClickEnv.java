/*
 * Copyright 2026 Advanced Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.labeling.TalkBackLabelManager;
import com.google.android.accessibility.utils.labeling.Label;
import com.google.android.accessibility.utils.screencapture.ScreenshotCapture;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/** Phone side of the auto-click language: finds and presses items on the real screen. */
final class JxClickEnv implements JxClickScript.Env {
  private static final int MAX_NODES = 6000;
  private static final long OPEN_APP_WAIT_MS = 5000;

  private static final Executor SHOT_EXECUTOR = Executors.newSingleThreadExecutor();
  private static final long SHOT_WAIT_MS = 4000;

  private final LuaServiceApi api;
  private final Context ctx;

  JxClickEnv(LuaServiceApi api, Context ctx) {
    this.api = api;
    this.ctx = ctx;
  }

  /** "a*b" matches "a...b"; no star = whole text must be equal. Case is ignored. */
  static Pattern wildcard(String p) {
    StringBuilder sb = new StringBuilder();
    String[] parts = p.split("\\*", -1);
    for (int i = 0; i < parts.length; i++) {
      if (i > 0) {
        sb.append(".*");
      }
      if (!parts[i].isEmpty()) {
        sb.append(Pattern.quote(parts[i]));
      }
    }
    return Pattern.compile(
        sb.toString(), Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
  }

  private AccessibilityNodeInfo root() {
    TalkBackService s = TalkBackService.getInstance();
    return s == null ? null : s.getRootInActiveWindow();
  }

  private static boolean matches(Pattern p, AccessibilityNodeInfo n) {
    CharSequence t = n.getText();
    if (t != null && p.matcher(t.toString().trim()).matches()) {
      return true;
    }
    CharSequence d = n.getContentDescription();
    return d != null && p.matcher(d.toString().trim()).matches();
  }

  private static void collect(
      AccessibilityNodeInfo n, Pattern p, boolean firstOnly, List<Object> out, int[] count) {
    if (n == null || count[0]++ > MAX_NODES || (firstOnly && !out.isEmpty())) {
      return;
    }
    if (n.isVisibleToUser() && matches(p, n)) {
      out.add(n);
    }
    for (int i = 0; i < n.getChildCount(); i++) {
      collect(n.getChild(i), p, firstOnly, out, count);
    }
  }

  /** Text of the user's own label (saved with the label feature) for this node, or null. */
  private static String customLabelOf(AccessibilityNodeInfo n) {
    try {
      TalkBackService s = TalkBackService.getInstance();
      String id = n.getViewIdResourceName();
      if (s == null || id == null) {
        return null;
      }
      TalkBackLabelManager m = s.getLabelManager();
      Label l = m == null ? null : m.getLabelForViewIdFromCache(id);
      return l == null ? null : l.getText();
    } catch (RuntimeException e) {
      return null; // label manager not ready: treat as "no label"
    }
  }

  private static void collectByLabel(
      AccessibilityNodeInfo n, Pattern p, List<Object> out, int[] count) {
    if (n == null || count[0]++ > MAX_NODES) {
      return;
    }
    if (n.isVisibleToUser()) {
      String t = customLabelOf(n);
      if (t != null && p.matcher(t.trim()).matches()) {
        out.add(n);
      }
    }
    for (int i = 0; i < n.getChildCount(); i++) {
      collectByLabel(n.getChild(i), p, out, count);
    }
  }

  @Override
  public List<Object> findAll(String pattern, boolean byLabel) {
    List<Object> out = new ArrayList<>();
    AccessibilityNodeInfo r = root();
    if (r == null) {
      return out;
    }
    if (byLabel) {
      collectByLabel(r, wildcard(pattern), out, new int[1]); // "#name": user's own label
    } else {
      collect(r, wildcard(pattern), false, out, new int[1]);
    }
    return out;
  }

  @Override
  public boolean click(Object node) {
    AccessibilityNodeInfo n = (AccessibilityNodeInfo) node;
    AccessibilityNodeInfo p = n;
    int guard = 0;
    while (p != null && guard++ < 20) {
      if (p.isClickable() && p.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
        return true;
      }
      p = p.getParent();
    }
    return api.toClick(n); // nothing clickable around it: real tap at the centre
  }

  @Override
  public boolean locate(Object node) {
    return ((AccessibilityNodeInfo) node).performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
  }

  @Override
  public boolean rawTap(Object node, long ms) {
    return api.tapNode((AccessibilityNodeInfo) node, ms);
  }

  @Override
  public boolean screenContains(String pattern) {
    AccessibilityNodeInfo r = root();
    if (r == null) {
      return false;
    }
    List<Object> out = new ArrayList<>();
    collect(r, wildcard(pattern), true, out, new int[1]);
    return !out.isEmpty();
  }

  @Override
  public boolean focusIs(String type) {
    AccessibilityNodeInfo f = api.focusedNode();
    switch (type.trim().toLowerCase(Locale.ROOT)) {
      case "可编辑":
      case "editable":
        return f != null && f.isEditable();
      case "可点击":
      case "clickable":
        return f != null && f.isClickable();
      case "列表":
      case "list":
        return f != null && api.isListView(f);
      case "列表项目":
      case "listitem":
        return f != null && api.isInListView(f);
      case "编辑框":
      case "edit":
        return api.inputNode() != null;
      default:
        return false;
    }
  }

  private String foregroundPackage() {
    AccessibilityNodeInfo r = root();
    CharSequence p = r == null ? null : r.getPackageName();
    return p == null ? "" : p.toString();
  }

  @Override
  public boolean inApp(String name) {
    String pkg = foregroundPackage();
    if (pkg.isEmpty() || name == null || name.isEmpty()) {
      return false;
    }
    String lower = name.toLowerCase(Locale.ROOT);
    if (pkg.toLowerCase(Locale.ROOT).contains(lower)) {
      return true;
    }
    try {
      PackageManager pm = ctx.getPackageManager();
      String label = String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)));
      return label.toLowerCase(Locale.ROOT).contains(lower);
    } catch (PackageManager.NameNotFoundException e) {
      return false;
    }
  }

  @Override
  public boolean openApp(String name) {
    PackageManager pm = ctx.getPackageManager();
    Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
    String found = null;
    String lower = name.toLowerCase(Locale.ROOT);
    for (ResolveInfo ri : pm.queryIntentActivities(main, 0)) {
      String label = String.valueOf(ri.loadLabel(pm));
      String pkg = ri.activityInfo.packageName;
      if (label.equalsIgnoreCase(name) || pkg.equalsIgnoreCase(name)) {
        found = pkg;
        break;
      }
      if (found == null && label.toLowerCase(Locale.ROOT).contains(lower)) {
        found = pkg;
      }
    }
    if (found == null) {
      return false;
    }
    Intent launch = pm.getLaunchIntentForPackage(found);
    if (launch == null) {
      return false;
    }
    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    ctx.startActivity(launch);
    long end = SystemClock.uptimeMillis() + OPEN_APP_WAIT_MS;
    while (SystemClock.uptimeMillis() < end) {
      if (foregroundPackage().equals(found)) {
        return true;
      }
      try {
        Thread.sleep(150);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
    return false;
  }

  @Override
  public boolean command(String name) {
    return api.execute(name);
  }

  @Override
  public Boolean callFunction(String name, String args) {
    return api.callLuaFunction(name, args);
  }

  @Override
  public boolean tap(double x, double y, long ms) {
    return api.press(x, y, (int) ms);
  }

  @Override
  public boolean swipe(double x1, double y1, double x2, double y2, long ms) {
    return api.swipe(x1, y1, x2, y2, (int) ms);
  }

  @Override
  public boolean tapFocus(long ms) {
    AccessibilityNodeInfo f = api.focusedNode();
    return f != null && api.tapNode(f, ms);
  }

  /** Fraction (0 < v < 1) of the size, otherwise already pixels. */
  private static int px(double v, int size) {
    int p = (int) Math.round(v > 0 && v < 1 ? v * size : v);
    return Math.max(0, Math.min(size - 1, p));
  }

  @Override
  public boolean clickColor(
      double x1, double y1, double x2, double y2, int r, int g, int b, int tol) {
    TalkBackService s = TalkBackService.getInstance();
    if (s == null || Build.VERSION.SDK_INT < 30) {
      return false; // taking a screenshot from the service needs Android 11
    }
    final Bitmap[] shot = new Bitmap[1];
    final CountDownLatch done = new CountDownLatch(1);
    try {
      ScreenshotCapture.takeScreenshot(
          s,
          (bitmap, supported) -> {
            shot[0] = bitmap;
            done.countDown();
          },
          SHOT_EXECUTOR);
      if (!done.await(SHOT_WAIT_MS, TimeUnit.MILLISECONDS)) {
        return false;
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    } catch (RuntimeException e) {
      return false;
    }
    if (shot[0] == null) {
      return false;
    }
    Bitmap bmp = shot[0].copy(Bitmap.Config.ARGB_8888, false); // screenshots may be hardware bitmaps
    if (bmp == null) {
      return false;
    }
    int w = bmp.getWidth();
    int h = bmp.getHeight();
    int left = Math.min(px(x1, w), px(x2, w));
    int right = Math.max(px(x1, w), px(x2, w));
    int top = Math.min(px(y1, h), px(y2, h));
    int bottom = Math.max(px(y1, h), px(y2, h));
    int rw = right - left + 1;
    int rh = bottom - top + 1;
    int[] pixels = new int[rw * rh];
    bmp.getPixels(pixels, 0, rw, left, top, rw, rh);
    for (int i = 0; i < pixels.length; i++) {
      int c = pixels[i];
      if (Math.abs(((c >> 16) & 0xFF) - r) <= tol
          && Math.abs(((c >> 8) & 0xFF) - g) <= tol
          && Math.abs((c & 0xFF) - b) <= tol) {
        return tap(left + (i % rw), top + (i / rw), 50);
      }
    }
    return false;
  }

  @Override
  public void sleep(long ms) throws InterruptedException {
    Thread.sleep(ms);
  }

  @Override
  public long now() {
    return SystemClock.uptimeMillis();
  }
}
