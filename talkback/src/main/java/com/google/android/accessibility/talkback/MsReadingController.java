/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.app.Notification;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Parcelable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.utils.AccessibilityEventUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;

/**
 * MS Screen Reader: makes the "Reading settings" that have no TalkBack equivalent work: volume
 * reading, reading the whole window, window-name and focused-item-change switches.
 */
public final class MsReadingController {
  public static final String K_VOLUME = "ms_rd_volume";
  public static final String K_WINDOW_ALL = "ms_rd_window_all";
  public static final String K_WINDOW_TITLE = "ms_rd_window_title";
  public static final String K_FOCUS_CHANGED = "ms_rd_focus_content_changed";
  public static final String K_SCREEN_ON = "ms_rd_screen_on";
  public static final String K_SCREEN_OFF = "ms_rd_screen_off";
  public static final String K_SCREEN_BATTERY = "ms_rd_screen_battery";
  public static final String K_SCREEN_NOTIF = "ms_rd_screen_notif";

  private static final String ACTION_VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION";
  private static final String EXTRA_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE";
  private static final String EXTRA_STREAM_VALUE = "android.media.EXTRA_VOLUME_STREAM_VALUE";
  private static final int MAX_NODES = 80;
  private static final int MAX_CHARS = 1500;

  private final TalkBackService service;
  private final SharedPreferences prefs;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private boolean registered;
  private int pendingStream = -1;
  private int pendingValue = -1;
  private boolean screenIsOff;
  private int newNotifications;

  private final BroadcastReceiver screenReceiver =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          if (intent == null || intent.getAction() == null) {
            return;
          }
          if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
            screenIsOff = true;
            newNotifications = 0;
            if (prefs.getBoolean(K_SCREEN_OFF, true)) {
              service.msSpeakMain(service.getString(R.string.ms_rd_locked), /* queue= */ false);
            }
          } else if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
            screenIsOff = false;
            speakUnlockState();
          }
        }
      };

  private final Runnable speakVolume =
      new Runnable() {
        @Override
        public void run() {
          AudioManager am = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
          if (am == null || pendingStream < 0) {
            return;
          }
          int max = am.getStreamMaxVolume(pendingStream);
          if (max <= 0) {
            return;
          }
          int percent = Math.round(pendingValue * 100f / max);
          service.msSpeakMain(
              service.getString(R.string.ms_rd_volume_spoken, percent), /* queue= */ false);
        }
      };

  private final BroadcastReceiver volumeReceiver =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          if (!prefs.getBoolean(K_VOLUME, true) || intent == null) {
            return;
          }
          int stream = intent.getIntExtra(EXTRA_STREAM_TYPE, -1);
          int value = intent.getIntExtra(EXTRA_STREAM_VALUE, -1);
          if (stream < 0 || value < 0) {
            return;
          }
          pendingStream = stream;
          pendingValue = value;
          handler.removeCallbacks(speakVolume);
          handler.postDelayed(speakVolume, 150);
        }
      };

  private final Runnable readWindow =
      new Runnable() {
        @Override
        public void run() {
          AccessibilityNodeInfo root = service.getRootInActiveWindow();
          if (root == null) {
            return;
          }
          StringBuilder sb = new StringBuilder();
          int[] count = {0};
          collect(root, sb, count, 0);
          if (sb.length() > 0) {
            service.msSpeakMain(sb.toString(), /* queue= */ true);
          }
        }
      };

  public MsReadingController(TalkBackService service) {
    this.service = service;
    this.prefs = SharedPreferencesUtils.getSharedPreferences(service);
  }

  public void start() {
    if (registered) {
      return;
    }
    ContextCompat.registerReceiver(
        service,
        volumeReceiver,
        new IntentFilter(ACTION_VOLUME_CHANGED),
        ContextCompat.RECEIVER_EXPORTED);
    ContextCompat.registerReceiver(
        service,
        screenReceiver,
        screenFilter(),
        ContextCompat.RECEIVER_NOT_EXPORTED);
    registered = true;
  }

  private static IntentFilter screenFilter() {
    IntentFilter f = new IntentFilter(Intent.ACTION_SCREEN_OFF);
    f.addAction(Intent.ACTION_USER_PRESENT);
    return f;
  }

  private void speakUnlockState() {
    StringBuilder sb = new StringBuilder();
    if (prefs.getBoolean(K_SCREEN_ON, true)) {
      sb.append(service.getString(R.string.ms_rd_unlocked)).append(". ");
    }
    if (prefs.getBoolean(K_SCREEN_BATTERY, false)) {
      BatteryManager bm = (BatteryManager) service.getSystemService(Context.BATTERY_SERVICE);
      if (bm != null) {
        int pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        if (pct > 0) {
          sb.append(service.getString(R.string.ms_rd_battery_spoken, pct)).append(". ");
        }
      }
    }
    if (prefs.getBoolean(K_SCREEN_NOTIF, false) && newNotifications > 0) {
      if (newNotifications == 1) {
        sb.append(service.getString(R.string.ms_rd_notif_spoken_one)).append(". ");
      } else {
        sb.append(service.getString(R.string.ms_rd_notif_spoken, newNotifications)).append(". ");
      }
    }
    newNotifications = 0;
    String text = sb.toString().trim();
    if (!text.isEmpty()) {
      service.msSpeakMain(text, /* queue= */ true);
    }
  }

  public void stop() {
    handler.removeCallbacksAndMessages(null);
    if (registered) {
      try {
        service.unregisterReceiver(volumeReceiver);
      } catch (IllegalArgumentException e) {
        // already unregistered
      }
      try {
        service.unregisterReceiver(screenReceiver);
      } catch (IllegalArgumentException e) {
        // already unregistered
      }
      registered = false;
    }
  }

  /** Called for every accessibility event. Starts "read the whole window" after a window change. */
  public void onEvent(AccessibilityEvent event) {
    if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED && screenIsOff) {
      Parcelable data = event.getParcelableData();
      if (data instanceof Notification) {
        int flags = ((Notification) data).flags;
        if ((flags & (Notification.FLAG_ONGOING_EVENT | Notification.FLAG_GROUP_SUMMARY)) == 0) {
          newNotifications++;
        }
      }
    }
    if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        || !prefs.getBoolean(K_WINDOW_ALL, false)) {
      return;
    }
    CharSequence pkg = event.getPackageName();
    if (pkg == null || "com.android.systemui".contentEquals(pkg)) {
      return;
    }
    handler.removeCallbacks(readWindow);
    handler.postDelayed(readWindow, 700);
  }

  private void collect(AccessibilityNodeInfo node, StringBuilder sb, int[] count, int depth) {
    if (node == null || count[0] >= MAX_NODES || sb.length() >= MAX_CHARS || depth > 30) {
      return;
    }
    if (!node.isVisibleToUser() || node.isPassword()) {
      return;
    }
    count[0]++;
    CharSequence t = node.getText();
    if (TextUtils.isEmpty(t)) {
      t = node.getContentDescription();
    }
    if (!TextUtils.isEmpty(t)) {
      String s = t.toString().trim();
      if (!s.isEmpty() && !sb.toString().endsWith(s + ". ")) {
        sb.append(s).append(". ");
      }
    }
    for (int i = 0; i < node.getChildCount(); i++) {
      collect(node.getChild(i), sb, count, depth + 1);
    }
  }

  /**
   * Returns true when the event must not reach TalkBack's speech rules because the matching
   * "Reading settings" switch is off.
   */
  public static boolean shouldDropEvent(Context context, AccessibilityEvent event) {
    int type = event.getEventType();
    if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
      return !SharedPreferencesUtils.getSharedPreferences(context).getBoolean(K_WINDOW_TITLE, true);
    }
    if (type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
      if (SharedPreferencesUtils.getSharedPreferences(context).getBoolean(K_FOCUS_CHANGED, true)) {
        return false;
      }
      AccessibilityNodeInfoCompat source = AccessibilityEventUtils.sourceCompat(event);
      return source != null
          && source.isAccessibilityFocused()
          && source.getLiveRegion() == ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE;
    }
    return false;
  }
}
