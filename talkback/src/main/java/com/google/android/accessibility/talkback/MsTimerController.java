/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import androidx.core.content.ContextCompat;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.util.Calendar;
import java.util.Locale;

/** MS Screen Reader: speaks time, date, year and battery after unlock and at an interval. */
public final class MsTimerController {
  private static final String P = "ms_adv_";

  private final TalkBackService service;
  private final SharedPreferences prefs;
  private final MsSecondaryTts secondary;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable tick = this::onTick;

  private final SharedPreferences.OnSharedPreferenceChangeListener listener =
      (p, key) -> {
        if (key != null && key.startsWith(P + "timer")) {
          schedule();
        }
      };

  private final BroadcastReceiver unlockReceiver =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          if (prefs.getBoolean(P + "unlock_announce", false)) {
            speak(compose());
          }
        }
      };

  public MsTimerController(TalkBackService service, MsSecondaryTts secondary) {
    this.service = service;
    this.secondary = secondary;
    this.prefs = SharedPreferencesUtils.getSharedPreferences(service);
  }

  public void start() {
    prefs.registerOnSharedPreferenceChangeListener(listener);
    ContextCompat.registerReceiver(
        service,
        unlockReceiver,
        new IntentFilter(Intent.ACTION_USER_PRESENT),
        ContextCompat.RECEIVER_NOT_EXPORTED);
    schedule();
  }

  public void stop() {
    prefs.unregisterOnSharedPreferenceChangeListener(listener);
    handler.removeCallbacks(tick);
    try {
      service.unregisterReceiver(unlockReceiver);
    } catch (IllegalArgumentException e) {
      // already unregistered
    }
  }

  /** Builds the sentence: time, date, year, battery (each part optional). */
  String compose() {
    Calendar c = Calendar.getInstance();
    StringBuilder sb = new StringBuilder();
    if (prefs.getBoolean(P + "say_time", true)) {
      String tf = prefs.getString("ms_rd_time_format", "def");
      java.text.DateFormat df;
      if ("12".equals(tf)) {
        df = new java.text.SimpleDateFormat("h:mm a", Locale.getDefault());
      } else if ("24".equals(tf)) {
        df = new java.text.SimpleDateFormat("HH:mm", Locale.getDefault());
      } else {
        df = DateFormat.getTimeFormat(service);
      }
      sb.append(df.format(c.getTime())).append(". ");
    }
    boolean date = prefs.getBoolean(P + "say_date", true);
    boolean year = prefs.getBoolean(P + "say_year", true);
    if (date) {
      sb.append(
          new java.text.SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(c.getTime()));
      if (year) {
        sb.append(' ').append(c.get(Calendar.YEAR));
      }
      sb.append(". ");
    } else if (year) {
      sb.append(c.get(Calendar.YEAR)).append(". ");
    }
    if (prefs.getBoolean(P + "say_battery", true)) {
      BatteryManager bm = (BatteryManager) service.getSystemService(Context.BATTERY_SERVICE);
      if (bm != null) {
        int pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        if (pct > 0) {
          sb.append("Battery ").append(pct).append("%.");
        }
      }
    }
    return sb.toString().trim();
  }

  private void speak(String text) {
    if (text.isEmpty()) {
      return;
    }
    if ("secondary".equals(prefs.getString(P + "announce_voice", "main"))) {
      secondary.speak(text, false);
    } else {
      service.msSpeakMain(text, true);
    }
  }

  private int intPref(String key, int def) {
    try {
      return Integer.parseInt(prefs.getString(P + key, String.valueOf(def)));
    } catch (NumberFormatException e) {
      return def;
    }
  }

  private void schedule() {
    handler.removeCallbacks(tick);
    if (!prefs.getBoolean(P + "timer_enabled", false)) {
      return;
    }
    Calendar c = Calendar.getInstance();
    int minuteOfDay = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
    int step = Math.max(1, intPref("timer_interval", 30));
    int next = ((minuteOfDay / step) + 1) * step;
    long delay =
        (next - minuteOfDay) * 60_000L - c.get(Calendar.SECOND) * 1000L - c.get(Calendar.MILLISECOND);
    handler.postDelayed(tick, Math.max(1000L, delay));
  }

  private void onTick() {
    int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    int from = intPref("timer_from", 7);
    int to = intPref("timer_to", 23);
    boolean inside = from <= to ? (h >= from && h <= to) : (h >= from || h <= to);
    if (inside) {
      speak(compose());
    }
    schedule();
  }
}
