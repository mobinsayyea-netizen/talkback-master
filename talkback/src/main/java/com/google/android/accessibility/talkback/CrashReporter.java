package com.google.android.accessibility.talkback;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/**
 * Keeps the text of the last crash. The next time the screen reader starts, that text is copied to
 * the clipboard so the user can paste it into a chat and the cause can be found.
 */
public final class CrashReporter {

  private static final String FILE = "last_crash.txt";
  private static final int MAX_CHARS = 4500;

  private CrashReporter() {}

  /** Called from the crash handler. Must never throw. */
  public static void save(Context context, Thread thread, Throwable error) {
    try {
      StringWriter sw = new StringWriter();
      error.printStackTrace(new PrintWriter(sw));
      String text =
          "MS Screen Reader crash\n"
              + "Time: "
              + new java.util.Date()
              + "\nAndroid "
              + Build.VERSION.RELEASE
              + " (SDK "
              + Build.VERSION.SDK_INT
              + "), "
              + Build.MANUFACTURER
              + " "
              + Build.MODEL
              + "\nThread: "
              + (thread == null ? "?" : thread.getName())
              + "\n\n"
              + sw;
      if (text.length() > MAX_CHARS) {
        text = text.substring(0, MAX_CHARS) + "\n...(cut)";
      }
      try (FileOutputStream out =
          new FileOutputStream(new File(context.getFilesDir(), FILE))) {
        out.write(text.getBytes(StandardCharsets.UTF_8));
      }
    } catch (Throwable ignored) {
      // A crash reporter must not crash.
    }
  }

  /** Copies the saved crash text to the clipboard (once) and tells the user. */
  public static void deliverIfAny(Context context) {
    try {
      File f = new File(context.getFilesDir(), FILE);
      if (!f.exists()) {
        return;
      }
      byte[] bytes = new byte[(int) Math.min(f.length(), 20000)];
      try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
        int n = in.read(bytes);
        if (n < 0) {
          n = 0;
        }
        String text = new String(bytes, 0, n, StandardCharsets.UTF_8);
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null && !text.isEmpty()) {
          cm.setPrimaryClip(ClipData.newPlainText("MS Screen Reader crash report", text));
          Toast.makeText(
                  context,
                  "The screen reader crashed earlier. The crash report is copied. Paste it in the chat.",
                  Toast.LENGTH_LONG)
              .show();
        }
      }
      // Remove it only after it was handed over.
      //noinspection ResultOfMethodCallIgnored
      f.delete();
    } catch (Throwable ignored) {
      // Ignore.
    }
  }
}
