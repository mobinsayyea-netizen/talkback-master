/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import java.util.ArrayList;

/** Listens once with the phone's speech recogniser. Used by "ask by voice" and dictation. */
final class MsSpeechInput {
  private MsSpeechInput() {}

  interface Callback {
    /** text is null when nothing was understood; message then says why. */
    void done(String text, String message);
  }

  private static SpeechRecognizer active;

  /** Starts listening. Safe to call from any thread. */
  static void listen(final Context context, final boolean partialAsFinal, final Callback cb) {
    final Context app = context.getApplicationContext();
    new Handler(Looper.getMainLooper())
        .post(
            () -> {
              if (!SpeechRecognizer.isRecognitionAvailable(app)) {
                cb.done(null, "Speech recognition is not available on this phone");
                return;
              }
              stop();
              final SpeechRecognizer r = SpeechRecognizer.createSpeechRecognizer(app);
              active = r;
              r.setRecognitionListener(
                  new RecognitionListener() {
                    private boolean finished;

                    private void finish(String text, String message) {
                      if (finished) {
                        return;
                      }
                      finished = true;
                      if (active == r) {
                        active = null;
                      }
                      try {
                        r.destroy();
                      } catch (RuntimeException ignored) {
                      }
                      cb.done(text, message);
                    }

                    @Override
                    public void onResults(Bundle results) {
                      ArrayList<String> l =
                          results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                      if (l != null && !l.isEmpty() && !l.get(0).trim().isEmpty()) {
                        finish(l.get(0).trim(), null);
                      } else {
                        finish(null, "Nothing heard");
                      }
                    }

                    @Override
                    public void onPartialResults(Bundle partial) {
                      if (!partialAsFinal) {
                        return;
                      }
                      ArrayList<String> l =
                          partial.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                      if (l != null && !l.isEmpty() && !l.get(0).trim().isEmpty()) {
                        finish(l.get(0).trim(), null);
                      }
                    }

                    @Override
                    public void onError(int error) {
                      finish(
                          null,
                          error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
                              ? "Microphone permission is needed"
                              : "Nothing heard");
                    }

                    @Override public void onReadyForSpeech(Bundle params) {}
                    @Override public void onBeginningOfSpeech() {}
                    @Override public void onRmsChanged(float rmsdB) {}
                    @Override public void onBufferReceived(byte[] buffer) {}
                    @Override public void onEndOfSpeech() {}
                    @Override public void onEvent(int eventType, Bundle params) {}
                  });
              Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
              i.putExtra(
                  RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
              i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, partialAsFinal);
              i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
              r.startListening(i);
            });
  }

  /** Stops a recognition that is still running. */
  static void stop() {
    SpeechRecognizer r = active;
    active = null;
    if (r != null) {
      try {
        r.cancel();
        r.destroy();
      } catch (RuntimeException ignored) {
      }
    }
  }
}
