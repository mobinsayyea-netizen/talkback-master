/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import com.google.android.accessibility.utils.SharedPreferencesUtils;

/**
 * MS Screen Reader: makes the "Operation settings" work. Handles volume-key actions (long press,
 * both keys, locked screen), the volume panel option, shake actions and wrap navigation.
 */
public final class MsOperationController {
  private static final String P = "ms_op_";

  private final TalkBackService service;
  private final SharedPreferences prefs;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final AudioManager audio;

  private boolean upDown;
  private boolean downDown;
  private boolean upConsumed;
  private boolean downConsumed;
  private boolean upFired;
  private boolean downFired;

  private SensorManager sensorManager;
  private long lastShake;

  private final Runnable upLong = () -> fireLong(true);
  private final Runnable downLong = () -> fireLong(false);

  private final SharedPreferences.OnSharedPreferenceChangeListener prefListener =
      (p, key) -> {
        if (key != null && key.startsWith(P + "shake")) {
          updateShake();
        }
      };

  public MsOperationController(TalkBackService service) {
    this.service = service;
    this.prefs = SharedPreferencesUtils.getSharedPreferences(service);
    this.audio = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
  }

  public void start() {
    prefs.registerOnSharedPreferenceChangeListener(prefListener);
    updateShake();
  }

  public void stop() {
    prefs.unregisterOnSharedPreferenceChangeListener(prefListener);
    handler.removeCallbacksAndMessages(null);
    if (sensorManager != null) {
      sensorManager.unregisterListener(shakeListener);
    }
  }

  /** Used by navigation: whether reaching the last item may wrap to the first. */
  public static boolean isWrapEnabled(Context context) {
    return SharedPreferencesUtils.getSharedPreferences(context)
        .getBoolean(P + "loop_move", true);
  }

  // ---------------------------------------------------------------- volume keys

  /** Returns true if the key event was consumed. */
  public boolean onKeyEvent(KeyEvent e) {
    int code = e.getKeyCode();
    if (code == KeyEvent.KEYCODE_HEADSETHOOK || code == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
      if (!prefs.getBoolean(P + "headset_key", false)) {
        return false;
      }
      String a = action("headset_action", "suspend");
      if ("nothing".equals(a)) {
        return false;
      }
      if (e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() == 0) {
        perform(a);
      }
      return true;
    }
    if (code != KeyEvent.KEYCODE_VOLUME_UP && code != KeyEvent.KEYCODE_VOLUME_DOWN) {
      return false;
    }
    if (!prefs.getBoolean(P + "key_shortcut", true)) {
      return false;
    }
    boolean up = code == KeyEvent.KEYCODE_VOLUME_UP;
    if (e.getAction() == KeyEvent.ACTION_DOWN) {
      if (e.getRepeatCount() > 0) {
        return up ? upConsumed : downConsumed;
      }
      if (up) {
        upDown = true;
      } else {
        downDown = true;
      }
      if (upDown && downDown) {
        String both = action("up_down_volume_short_key", "suspend");
        if (!"nothing".equals(both)) {
          handler.removeCallbacks(upLong);
          handler.removeCallbacks(downLong);
          upConsumed = true;
          downConsumed = true;
          upFired = true;
          downFired = true;
          perform(both);
          return true;
        }
      }
      String longAction = longAction(up);
      if ("nothing".equals(longAction)) {
        setConsumed(up, false);
        return false;
      }
      setConsumed(up, true);
      setFired(up, false);
      handler.postDelayed(up ? upLong : downLong, ViewConfiguration.getLongPressTimeout());
      return true;
    }
    if (e.getAction() == KeyEvent.ACTION_UP) {
      if (up) {
        upDown = false;
      } else {
        downDown = false;
      }
      handler.removeCallbacks(up ? upLong : downLong);
      boolean consumed = up ? upConsumed : downConsumed;
      boolean fired = up ? upFired : downFired;
      setConsumed(up, false);
      setFired(up, false);
      if (!consumed) {
        return false;
      }
      if (!fired) {
        shortPress(up);
      }
      return true;
    }
    return false;
  }

  private void setConsumed(boolean up, boolean v) {
    if (up) {
      upConsumed = v;
    } else {
      downConsumed = v;
    }
  }

  private void setFired(boolean up, boolean v) {
    if (up) {
      upFired = v;
    } else {
      downFired = v;
    }
  }

  private String longAction(boolean up) {
    KeyguardManager km = (KeyguardManager) service.getSystemService(Context.KEYGUARD_SERVICE);
    boolean locked = km != null && km.isKeyguardLocked();
    String base = up ? "up_volume_key" : "down_volume_key";
    String def = up ? "suspend" : "assistant";
    return locked ? action(base + "_off", "nothing") : action(base, def);
  }

  private void fireLong(boolean up) {
    if (up ? upFired : downFired) {
      return;
    }
    setFired(up, true);
    perform(longAction(up));
  }

  private int upCount;
  private int downCount;
  private final Runnable upMulti = () -> flushMulti(true);
  private final Runnable downMulti = () -> flushMulti(false);

  private void shortPress(boolean up) {
    if (!prefs.getBoolean(P + "multi_hot_key", false)) {
      adjustVolume(up);
      return;
    }
    if (up) {
      upCount++;
    } else {
      downCount++;
    }
    handler.removeCallbacks(up ? upMulti : downMulti);
    handler.postDelayed(up ? upMulti : downMulti, 350);
  }

  private void flushMulti(boolean up) {
    int n = up ? upCount : downCount;
    if (up) {
      upCount = 0;
    } else {
      downCount = 0;
    }
    String a = "nothing";
    if (n == 2) {
      a = action(up ? "up_volume_2_key" : "down_volume_2_key", "nothing");
    } else if (n >= 3) {
      a = action(up ? "up_volume_3_key" : "down_volume_3_key", "nothing");
    }
    if ("nothing".equals(a)) {
      for (int i = 0; i < n; i++) {
        adjustVolume(up);
      }
    } else {
      perform(a);
    }
  }

  private void adjustVolume(boolean up) {
    if (audio == null) {
      return;
    }
    int flags = prefs.getBoolean(P + "show_volume_ui", false) ? AudioManager.FLAG_SHOW_UI : 0;
    audio.adjustSuggestedStreamVolume(
        up ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER,
        AudioManager.USE_DEFAULT_STREAM_TYPE,
        flags);
  }

  // ---------------------------------------------------------------- shake

  private final SensorEventListener shakeListener =
      new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent ev) {
          float x = ev.values[0];
          float y = ev.values[1];
          float z = ev.values[2];
          double g = Math.sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH;
          if (g < shakeThreshold()) {
            return;
          }
          long now = SystemClock.elapsedRealtime();
          if (now - lastShake < 1500) {
            return;
          }
          lastShake = now;
          if (prefs.getBoolean(P + "shake_action_call", false)
              && audio != null
              && audio.getMode() == AudioManager.MODE_RINGTONE) {
            try {
              android.telecom.TelecomManager tm =
                  (android.telecom.TelecomManager)
                      service.getSystemService(Context.TELECOM_SERVICE);
              if (tm != null) {
                tm.acceptRingingCall();
                return;
              }
            } catch (RuntimeException ex) {
              // Phone permission not granted: fall through to the normal shake action.
            }
          }
          perform(action("shake_action", "notifications"));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
      };

  private double shakeThreshold() {
    int sens = 9;
    try {
      sens = Integer.parseInt(prefs.getString(P + "shake_action_sens", "9"));
    } catch (NumberFormatException e) {
      // keep default
    }
    return 3.0 - sens * 0.08;
  }

  private void updateShake() {
    if (sensorManager == null) {
      sensorManager = (SensorManager) service.getSystemService(Context.SENSOR_SERVICE);
    }
    if (sensorManager == null) {
      return;
    }
    sensorManager.unregisterListener(shakeListener);
    if ("nothing".equals(action("shake_action", "notifications"))
        && !prefs.getBoolean(P + "shake_action_call", false)) {
      return;
    }
    Sensor s = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    if (s != null) {
      sensorManager.registerListener(shakeListener, s, SensorManager.SENSOR_DELAY_UI);
    }
  }

  // ---------------------------------------------------------------- actions

  private String action(String key, String def) {
    String v = prefs.getString(P + key, def);
    return v == null ? def : v;
  }

  private void perform(String action) {
    switch (action) {
      case "suspend":
        service.interruptAllFeedback(/* stopTtsSpeechCompletely= */ true);
        break;
      case "assistant":
        // TalkBack's own voice commands; falls back to the phone's assistant if not ready.
        if (!service.msStartVoiceCommands()) {
          launchAssistant();
        }
        break;
      case "system_assistant":
        launchAssistant();
        break;
      case "home":
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
        break;
      case "back":
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
        break;
      case "notifications":
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS);
        break;
      case "recents":
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
        break;
      case "quick":
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS);
        break;
      default:
        break;
    }
  }

  private void launchAssistant() {
    try {
      service.startActivity(
          new Intent(Intent.ACTION_VOICE_COMMAND).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    } catch (RuntimeException e) {
      try {
        service.startActivity(
            new Intent(Intent.ACTION_ASSIST).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
      } catch (RuntimeException e2) {
        // No assistant installed.
      }
    }
  }
}
