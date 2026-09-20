package com.google.android.accessibility.talkback;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;

/** Plays a short sound from res/raw on the accessibility audio stream. */
public final class ExtraSounds {

  private ExtraSounds() {}

  public static void play(Context context, int rawResId) {
    try {
      AudioAttributes attributes =
          new AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .build();
      MediaPlayer player =
          MediaPlayer.create(context, rawResId, attributes, AudioManager.AUDIO_SESSION_ID_GENERATE);
      if (player == null) {
        return;
      }
      player.setOnCompletionListener(MediaPlayer::release);
      player.setOnErrorListener(
          (mp, what, extra) -> {
            mp.release();
            return true;
          });
      player.start();
    } catch (Exception e) {
      // A missing sound must never break the screen reader.
    }
  }
}
