/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.TextUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;

/** MS Screen Reader: a second, independent TTS voice that follows "Secondary TTS settings". */
public final class MsSecondaryTts {
  private static final String P = "ms_async_tts_";

  private final Context context;
  private final SharedPreferences prefs;
  private final AudioManager audio;
  private TextToSpeech tts;
  private String loadedEngine;
  private boolean ready;
  private AudioFocusRequest focusRequest;
  private int counter;
  private SensorManager sensors;
  private final java.util.ArrayList<String> pending = new java.util.ArrayList<>();

  private final SharedPreferences.OnSharedPreferenceChangeListener listener =
      (p, key) -> {
        if (("ms_async_tts_engine").equals(key)) {
          reload();
        } else if (("ms_async_tts_proximity").equals(key)) {
          updateProximity();
        }
      };

  private final SensorEventListener proximityListener =
      new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent ev) {
          Sensor s = ev.sensor;
          if (ev.values[0] < Math.min(5f, s.getMaximumRange())) {
            stop();
          }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
      };

  public MsSecondaryTts(Context context) {
    this.context = context;
    this.prefs = SharedPreferencesUtils.getSharedPreferences(context);
    this.audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
  }

  public void start() {
    prefs.registerOnSharedPreferenceChangeListener(listener);
    updateProximity();
  }

  public void shutdown() {
    prefs.unregisterOnSharedPreferenceChangeListener(listener);
    if (sensors != null) {
      sensors.unregisterListener(proximityListener);
    }
    if (tts != null) {
      tts.shutdown();
      tts = null;
    }
    ready = false;
  }

  public void stop() {
    pending.clear();
    if (tts != null && ready) {
      tts.stop();
    }
  }

  /** Stops speech when the screen is touched, if the option is on. */
  public void onTouchStart() {
    if (prefs.getBoolean(P + "touch_stop", false)) {
      stop();
    }
  }

  public void speak(CharSequence text, boolean interrupt) {
    if (TextUtils.isEmpty(text)) {
      return;
    }
    ensureLoaded();
    if (!ready) {
      pending.add(text.toString());
      return;
    }
    doSpeak(text.toString(), interrupt);
  }

  private void doSpeak(String text, boolean interrupt) {
    if (prefs.getBoolean(P + "audio_focus", false)) {
      requestFocus();
    }
    tts.setSpeechRate(readFloat("speed", 1.0f));
    tts.setPitch(readFloat("pitch", 1.0f));
    boolean a11y = prefs.getBoolean(P + "accessibility_volume", false);
    tts.setAudioAttributes(
        new AudioAttributes.Builder()
            .setUsage(
                a11y
                    ? AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY
                    : AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build());
    Bundle params = new Bundle();
    float vol = readFloat("volume", 100f) / 100f;
    params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, Math.max(0.05f, Math.min(1f, vol)));
    tts.speak(
        text,
        interrupt ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD,
        params,
        "ms2-" + (counter++));
  }

  private float readFloat(String key, float def) {
    try {
      return Float.parseFloat(prefs.getString(P + key, String.valueOf(def)));
    } catch (RuntimeException e) {
      return def;
    }
  }

  private void ensureLoaded() {
    String engine = prefs.getString(P + "engine", "");
    if (tts != null && TextUtils.equals(engine, loadedEngine)) {
      return;
    }
    reload();
  }

  private void reload() {
    ready = false;
    if (tts != null) {
      tts.shutdown();
      tts = null;
    }
    String engine = prefs.getString(P + "engine", "");
    loadedEngine = engine;
    TextToSpeech.OnInitListener init =
        status -> {
          if (status != TextToSpeech.SUCCESS) {
            ready = false;
            return;
          }
          ready = true;
          tts.setOnUtteranceProgressListener(
              new UtteranceProgressListener() {
                @Override
                public void onStart(String id) {}

                @Override
                public void onDone(String id) {
                  abandonFocus();
                }

                @Override
                public void onError(String id) {
                  abandonFocus();
                }
              });
          for (String t : new java.util.ArrayList<>(pending)) {
            doSpeak(t, false);
          }
          pending.clear();
        };
    tts = TextUtils.isEmpty(engine) ? new TextToSpeech(context, init) : new TextToSpeech(context, init, engine);
  }

  private void requestFocus() {
    if (audio == null) {
      return;
    }
    if (focusRequest == null) {
      focusRequest =
          new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
              .setAudioAttributes(
                  new AudioAttributes.Builder()
                      .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                      .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                      .build())
              .build();
    }
    audio.requestAudioFocus(focusRequest);
  }

  private void abandonFocus() {
    if (audio != null && focusRequest != null) {
      audio.abandonAudioFocusRequest(focusRequest);
    }
  }

  private void updateProximity() {
    if (sensors == null) {
      sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }
    if (sensors == null) {
      return;
    }
    sensors.unregisterListener(proximityListener);
    if (prefs.getBoolean(P + "proximity", false)) {
      Sensor s = sensors.getDefaultSensor(Sensor.TYPE_PROXIMITY);
      if (s != null) {
        sensors.registerListener(proximityListener, s, SensorManager.SENSOR_DELAY_NORMAL);
      }
    }
  }
}
