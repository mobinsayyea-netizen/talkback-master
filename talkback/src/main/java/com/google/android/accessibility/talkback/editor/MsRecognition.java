/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityNodeInfo;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.translate.OcrTool;
import com.google.android.accessibility.utils.screencapture.ScreenshotCapture;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Commands that read the screen: text recognition, CAPTCHA, focus screenshot, subtitles, and
 * questions to Gemini about the focus or the whole screen. Not compiled yet.
 */
final class MsRecognition {
  private MsRecognition() {}

  private static final Handler MAIN = new Handler(Looper.getMainLooper());
  private static final long SUBTITLE_PERIOD_MS = 1500;
  private static final long SUBTITLE_MAX_MS = 30 * 60 * 1000L;

  private static Rect boundsOf(AccessibilityNodeInfo n) {
    if (n == null) {
      return null;
    }
    Rect r = new Rect();
    n.getBoundsInScreen(r);
    return r.isEmpty() ? null : r;
  }

  private static Bitmap argb(Bitmap b) {
    return b == null ? null : b.copy(Bitmap.Config.ARGB_8888, false); // screenshots can be hardware
  }

  private static Bitmap crop(Bitmap full, Rect r) {
    if (full == null || r == null) {
      return full;
    }
    int l = Math.max(0, r.left);
    int t = Math.max(0, r.top);
    int rr = Math.min(full.getWidth(), r.right);
    int bb = Math.min(full.getHeight(), r.bottom);
    if (rr - l < 4 || bb - t < 4) {
      return full;
    }
    try {
      return Bitmap.createBitmap(full, l, t, rr - l, bb - t);
    } catch (RuntimeException e) {
      return full;
    }
  }

  // ---------------------------------------------------------------- Recognize text on the screen

  static boolean readScreen(final LuaServiceApi api, TalkBackService s) {
    api.speak("Reading the screen");
    OcrTool.recognize(
        s, null, (text, message) -> api.speak(text != null ? text : String.valueOf(message)));
    return true;
  }

  // ---------------------------------------------------------------- Get CAPTCHA

  /** Reads the letters and digits of the focused item (or of the screen) and copies them. */
  static boolean captcha(final LuaServiceApi api, TalkBackService s, AccessibilityNodeInfo focus) {
    OcrTool.recognize(
        s,
        boundsOf(focus),
        (text, message) -> {
          if (text == null) {
            api.speak(String.valueOf(message));
            return;
          }
          String code = text.replaceAll("[^\\p{L}\\p{N}]", "");
          if (code.isEmpty()) {
            api.speak("No code found");
            return;
          }
          api.copy(code);
          StringBuilder spaced = new StringBuilder();
          for (int i = 0; i < code.length(); i++) {
            spaced.append(code.charAt(i)).append(' ');
          }
          api.speak(spaced.toString().trim() + ". Copied");
        });
    return true;
  }

  // ---------------------------------------------------------------- Screenshot of the focus

  static boolean saveFocusShot(
      final LuaServiceApi api, final Context ctx, TalkBackService s, AccessibilityNodeInfo focus) {
    final Rect r = boundsOf(focus);
    if (android.os.Build.VERSION.SDK_INT < 30) {
      return false;
    }
    ScreenshotCapture.takeScreenshot(
        s,
        (bitmap, supported) -> {
          Bitmap full = argb(bitmap);
          if (full == null) {
            api.speak("Could not take the screenshot");
            return;
          }
          final Bitmap shot = crop(full, r);
          new Thread(
                  () -> {
                    File dir = new File(LuaPaths.base(ctx), "Screenshots");
                    dir.mkdirs();
                    String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
                    File out = new File(dir, "focus-" + stamp + ".png");
                    try (FileOutputStream f = new FileOutputStream(out)) {
                      shot.compress(Bitmap.CompressFormat.PNG, 100, f);
                      api.speak("Screenshot saved in the Screenshots folder");
                    } catch (IOException e) {
                      api.speak("Could not save the screenshot");
                    }
                  },
                  "ms-focus-shot")
              .start();
        });
    return true;
  }

  // ---------------------------------------------------------------- Recognize subtitles

  private static boolean subtitlesOn;
  private static long subtitlesStopAt;
  private static String lastSubtitle = "";

  /** Switches subtitle reading on or off: the lower part of the screen is read again and again. */
  static boolean toggleSubtitles(final LuaServiceApi api, final TalkBackService s) {
    if (subtitlesOn) {
      subtitlesOn = false;
      api.speak("Subtitle reading off");
      return true;
    }
    subtitlesOn = true;
    lastSubtitle = "";
    subtitlesStopAt = System.currentTimeMillis() + SUBTITLE_MAX_MS;
    api.speak("Subtitle reading on");
    MAIN.postDelayed(() -> subtitleRound(api, s), 1500);
    return true;
  }

  private static void subtitleRound(final LuaServiceApi api, final TalkBackService s) {
    if (!subtitlesOn) {
      return;
    }
    if (System.currentTimeMillis() > subtitlesStopAt) {
      subtitlesOn = false;
      api.speak("Subtitle reading stopped after 30 minutes");
      return;
    }
    android.util.DisplayMetrics m = s.getResources().getDisplayMetrics();
    Rect lower = new Rect(0, (int) (m.heightPixels * 0.55f), m.widthPixels, m.heightPixels);
    OcrTool.recognize(
        s,
        lower,
        (text, message) -> {
          if (subtitlesOn && text != null && !text.equals(lastSubtitle)) {
            lastSubtitle = text;
            api.speak(text);
          }
          MAIN.postDelayed(() -> subtitleRound(api, s), SUBTITLE_PERIOD_MS);
        });
  }

  // ---------------------------------------------------------------- Ask Gemini

  /** Sends the focus (or the screen) with a question to Gemini. */
  private static void sendToGemini(
      final TalkBackService s,
      final LuaServiceApi api,
      final String question,
      final Rect region) {
    if (android.os.Build.VERSION.SDK_INT < 30) {
      api.speak("This needs Android 11 or newer");
      return;
    }
    ScreenshotCapture.takeScreenshot(
        s,
        (bitmap, supported) -> {
          Bitmap img = crop(argb(bitmap), region);
          if (img == null) {
            api.speak("Could not take the screenshot");
            return;
          }
          if (!s.msGeminiAsk(question, img)) {
            api.speak("Gemini is not ready");
          }
        });
  }

  /** "Inquire by voice": you speak a question, Gemini answers about the focus or the screen. */
  static boolean askByVoice(
      final LuaServiceApi api,
      final Context ctx,
      final TalkBackService s,
      AccessibilityNodeInfo focus,
      final boolean wholeScreen) {
    final Rect region = wholeScreen ? null : boundsOf(focus);
    api.speak("Ask your question");
    MAIN.postDelayed(
        () ->
            MsSpeechInput.listen(
                ctx,
                false,
                (question, message) -> {
                  if (question == null) {
                    api.speak(String.valueOf(message));
                    return;
                  }
                  sendToGemini(s, api, question, region);
                }),
        1600);
    return true;
  }

  /** "Identify form": Gemini lists the fields of the form on the screen. */
  static boolean identifyForm(LuaServiceApi api, TalkBackService s) {
    sendToGemini(
        s,
        api,
        "List every field of the form on this screen in order, with its label and the value "
            + "already filled in, and say which fields are required.",
        null);
    return true;
  }
}
