/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.speech.tts.TextToSpeech;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import androidx.core.content.ContextCompat;
import com.google.android.accessibility.utils.SharedPreferencesUtils;

/**
 * MS Screen Reader: caller announcement. Says who is calling (name from contacts if allowed, else
 * the number digit by digit), can repeat it, can use the ringtone volume, and says the call length
 * when a call ends.
 */
public final class MsCallController {
  public static final String K_ANNOUNCE = "ms_rd_call_announce";
  public static final String K_REPEAT = "ms_rd_call_repeat";
  public static final String K_RING_VOLUME = "ms_rd_call_ringvol";
  public static final String K_DURATION = "ms_rd_call_time";

  private static final long REPEAT_GAP_MS = 4000;

  private final TalkBackService service;
  private final SharedPreferences prefs;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private boolean registered;
  private boolean ringing;
  private String lastNumber;
  private long offhookStart;
  private int sessionId;

  private TextToSpeech ringTts;
  private boolean ringReady;
  private String ringPending;

  private final BroadcastReceiver receiver =
      new BroadcastReceiver() {
        @Override
        @SuppressWarnings("deprecation")
        public void onReceive(Context context, Intent intent) {
          if (intent == null) {
            return;
          }
          String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
          if (state == null) {
            return;
          }
          if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            onRinging(number);
          } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
            stopRepeats();
            if (offhookStart == 0) {
              offhookStart = System.currentTimeMillis();
            }
          } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            onIdle();
          }
        }
      };

  public MsCallController(TalkBackService service) {
    this.service = service;
    this.prefs = SharedPreferencesUtils.getSharedPreferences(service);
  }

  public void start() {
    if (registered) {
      return;
    }
    ContextCompat.registerReceiver(
        service,
        receiver,
        new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED),
        ContextCompat.RECEIVER_EXPORTED);
    registered = true;
  }

  public void stop() {
    handler.removeCallbacksAndMessages(null);
    if (registered) {
      try {
        service.unregisterReceiver(receiver);
      } catch (IllegalArgumentException e) {
        // already unregistered
      }
      registered = false;
    }
    if (ringTts != null) {
      ringTts.shutdown();
      ringTts = null;
      ringReady = false;
    }
  }

  private void onRinging(String number) {
    ringing = true;
    if (!prefs.getBoolean(K_ANNOUNCE, true)) {
      return;
    }
    // Android may send RINGING twice: first without the number, then with it.
    if (TextUtils.isEmpty(number) || number.equals(lastNumber)) {
      return;
    }
    lastNumber = number;
    final int id = ++sessionId;
    final String num = number;
    new Thread(
            new Runnable() {
              @Override
              public void run() {
                final String name = lookupName(num);
                handler.post(
                    new Runnable() {
                      @Override
                      public void run() {
                        if (id == sessionId && ringing) {
                          announce(name, num, id);
                        }
                      }
                    });
              }
            })
        .start();
  }

  private void announce(String name, String number, final int id) {
    final String who = TextUtils.isEmpty(name) ? spellNumber(number) : name;
    final String text = service.getString(R.string.ms_rd_call_incoming, who);
    speak(text);
    int times = 2;
    try {
      times = Integer.parseInt(prefs.getString(K_REPEAT, "2"));
    } catch (NumberFormatException e) {
      // keep default
    }
    for (int i = 1; i < times; i++) {
      handler.postDelayed(
          new Runnable() {
            @Override
            public void run() {
              if (id == sessionId && ringing) {
                speak(text);
              }
            }
          },
          i * REPEAT_GAP_MS);
    }
  }

  private void stopRepeats() {
    ringing = false;
    sessionId++;
    handler.removeCallbacksAndMessages(null);
  }

  private void onIdle() {
    stopRepeats();
    lastNumber = null;
    long start = offhookStart;
    offhookStart = 0;
    if (start > 0 && prefs.getBoolean(K_DURATION, true)) {
      long secs = (System.currentTimeMillis() - start) / 1000;
      if (secs > 0) {
        long m = secs / 60;
        long s = secs % 60;
        String text =
            m > 0
                ? service.getString(R.string.ms_rd_call_duration_ms, (int) m, (int) s)
                : service.getString(R.string.ms_rd_call_duration_s, (int) s);
        service.msSpeakMain(text, /* queue= */ true);
      }
    }
  }

  private static String spellNumber(String number) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < number.length(); i++) {
      char c = number.charAt(i);
      sb.append(c);
      if (Character.isDigit(c)) {
        sb.append(' ');
      }
    }
    return sb.toString().trim();
  }

  private String lookupName(String number) {
    if (ContextCompat.checkSelfPermission(service, Manifest.permission.READ_CONTACTS)
        != PackageManager.PERMISSION_GRANTED) {
      return null;
    }
    Cursor c = null;
    try {
      Uri uri =
          Uri.withAppendedPath(
              ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
      c =
          service
              .getContentResolver()
              .query(uri, new String[] {ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
      if (c != null && c.moveToFirst()) {
        return c.getString(0);
      }
    } catch (RuntimeException e) {
      // no name
    } finally {
      if (c != null) {
        c.close();
      }
    }
    return null;
  }

  private void speak(String text) {
    if (!prefs.getBoolean(K_RING_VOLUME, false)) {
      service.msSpeakMain(text, /* queue= */ false);
      return;
    }
    if (ringTts == null) {
      ringPending = text;
      ringTts =
          new TextToSpeech(
              service,
              new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                  if (status == TextToSpeech.SUCCESS && ringTts != null) {
                    ringTts.setAudioAttributes(
                        new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build());
                    ringReady = true;
                    if (ringPending != null) {
                      ringTts.speak(ringPending, TextToSpeech.QUEUE_FLUSH, null, "ms-call");
                      ringPending = null;
                    }
                  }
                }
              });
      return;
    }
    if (ringReady) {
      ringTts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ms-call");
    } else {
      ringPending = text;
    }
  }
}
