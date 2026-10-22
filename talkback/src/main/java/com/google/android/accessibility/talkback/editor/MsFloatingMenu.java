/*
 * Copyright 2026 Advanced Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import java.util.List;

/**
 * The floating menu: a strip right under the status bar with your chosen entries (default: Main
 * menu). Uses an accessibility overlay window, so it needs no extra permission. Not compiled yet.
 */
public final class MsFloatingMenu {
  private MsFloatingMenu() {}

  private static final Handler MAIN = new Handler(Looper.getMainLooper());
  private static View strip;

  /** Shows or hides the strip to match "Use the floating menu". Pass the accessibility service. */
  public static void refresh(final Context service) {
    MAIN.post(
        () -> {
          hideNow(service);
          if (MsMenus.prefs(service).getBoolean("ms_jx_use_float_menu", false)) {
            showNow(service);
          }
        });
  }

  public static void hide(final Context service) {
    MAIN.post(() -> hideNow(service));
  }

  private static void hideNow(Context service) {
    if (strip == null) {
      return;
    }
    try {
      ((WindowManager) service.getSystemService(Context.WINDOW_SERVICE)).removeView(strip);
    } catch (RuntimeException e) {
      // already gone
    }
    strip = null;
  }

  private static void showNow(final Context service) {
    try {
      LinearLayout row = new LinearLayout(service);
      row.setOrientation(LinearLayout.HORIZONTAL);
      row.setBackgroundColor(Color.argb(220, 32, 32, 32));
      List<MsMenus.Item> items = MsMenus.load(service, MsMenus.K_FLOAT);
      for (final MsMenus.Item it : items) {
        Button b = new Button(service);
        b.setText(it.label);
        b.setAllCaps(false);
        b.setTextSize(12);
        b.setContentDescription(it.label);
        b.setOnClickListener(
            v ->
                MsMenus.runNow(
                    service.getApplicationContext(), it.kind, it.name, it.label));
        row.addView(b, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
      }
      WindowManager.LayoutParams lp =
          new WindowManager.LayoutParams(
              WindowManager.LayoutParams.MATCH_PARENT,
              WindowManager.LayoutParams.WRAP_CONTENT,
              WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
              WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                  | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
              PixelFormat.TRANSLUCENT);
      lp.gravity = Gravity.TOP;
      int id = service.getResources().getIdentifier("status_bar_height", "dimen", "android");
      lp.y = id > 0 ? service.getResources().getDimensionPixelSize(id) : 0;
      ((WindowManager) service.getSystemService(Context.WINDOW_SERVICE)).addView(row, lp);
      strip = row;
    } catch (RuntimeException e) {
      strip = null;
    }
  }
}
