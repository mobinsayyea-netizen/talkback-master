package com.google.android.accessibility.talkback.clipboard;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import androidx.annotation.Nullable;

/**
 * Opens the clipboard screen. When it is opened from an edit box, the screen gets a paste target so
 * that a double tap on an item pastes it straight into that edit box.
 */
public final class ClipboardBridge {

  /** Pastes into the edit box the clipboard was opened from. */
  public interface PasteTarget {
    /** Called after the clipboard screen has been closed by choosing an item. */
    void pasteChosenText();
  }

  private static final long TARGET_LIFETIME_MS = 5000;

  private static final Object LOCK = new Object();
  private static @Nullable PasteTarget target;
  private static long expiresAt;

  private ClipboardBridge() {}

  /** Opens the clipboard screen. {@code pasteTarget} is null when no edit box is focused. */
  public static void open(Context context, @Nullable PasteTarget pasteTarget) {
    synchronized (LOCK) {
      target = pasteTarget;
      expiresAt = SystemClock.uptimeMillis() + TARGET_LIFETIME_MS;
    }
    Intent intent = new Intent(context, ClipboardActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    context.startActivity(intent);
  }

  /** Returns the paste target once, and only if it was set a moment ago. */
  static @Nullable PasteTarget takeTarget() {
    synchronized (LOCK) {
      PasteTarget result = SystemClock.uptimeMillis() <= expiresAt ? target : null;
      target = null;
      return result;
    }
  }
}
