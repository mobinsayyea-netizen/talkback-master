package com.google.android.accessibility.talkback.translate;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Bitmap;
import android.graphics.Rect;
import com.google.android.accessibility.utils.screencapture.ScreenshotCapture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

/** Reads text from a part of the screen on the phone itself (English, Hindi, Marathi). */
public final class OcrTool {

  /** Called once. If text is null or empty, message says why. */
  public interface Callback {
    void done(String text, String message);
  }

  private OcrTool() {}

  public static void recognize(AccessibilityService service, final Rect region, final Callback cb) {
    if (android.os.Build.VERSION.SDK_INT < 30) {
      cb.done(null, "Text reading needs Android 11 or newer");
      return;
    }
    ScreenshotCapture.takeScreenshot(
        service,
        (bitmap, isFormatSupported) -> {
          if (bitmap == null) {
            cb.done(null, "Could not read the screen");
            return;
          }
          Bitmap target = crop(bitmap, region);
          InputImage image = InputImage.fromBitmap(target, 0);
          runDevanagari(image, cb);
        });
  }

  private static Bitmap crop(Bitmap bitmap, Rect region) {
    if (region == null) {
      return bitmap;
    }
    int left = Math.max(0, region.left);
    int top = Math.max(0, region.top);
    int right = Math.min(bitmap.getWidth(), region.right);
    int bottom = Math.min(bitmap.getHeight(), region.bottom);
    if (right - left < 4 || bottom - top < 4) {
      return bitmap;
    }
    try {
      return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top);
    } catch (RuntimeException e) {
      return bitmap;
    }
  }

  // The Devanagari model reads Hindi, Marathi and English. If it is not ready yet, use English only.
  private static void runDevanagari(final InputImage image, final Callback cb) {
    final TextRecognizer recognizer =
        TextRecognition.getClient(new DevanagariTextRecognizerOptions.Builder().build());
    recognizer
        .process(image)
        .addOnSuccessListener(
            result -> {
              String text = result.getText();
              recognizer.close();
              if (text != null && !text.trim().isEmpty()) {
                cb.done(text.trim(), null);
              } else {
                runLatin(image, cb, false);
              }
            })
        .addOnFailureListener(
            e -> {
              recognizer.close();
              runLatin(image, cb, true);
            });
  }

  private static void runLatin(
      final InputImage image, final Callback cb, final boolean devanagariFailed) {
    final TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    recognizer
        .process(image)
        .addOnSuccessListener(
            result -> {
              String text = result.getText();
              recognizer.close();
              if (text != null && !text.trim().isEmpty()) {
                cb.done(text.trim(), null);
              } else if (devanagariFailed) {
                cb.done(
                    null,
                    "The Hindi and Marathi text model is not ready yet. Please try again in a moment.");
              } else {
                cb.done(null, "No text found");
              }
            })
        .addOnFailureListener(
            e -> {
              recognizer.close();
              cb.done(null, "Could not read the text");
            });
  }
}
