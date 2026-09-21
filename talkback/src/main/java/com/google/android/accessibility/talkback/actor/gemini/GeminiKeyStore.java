package com.google.android.accessibility.talkback.actor.gemini;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

/** Stores the Gemini key and model chosen by the user, and the one-time prompt / quota state. */
public final class GeminiKeyStore {

  public static final String PREFS = "ms_gemini";
  public static final String DEFAULT_MODEL = "gemini-flash-latest";

  private static final long QUOTA_BLOCK_MS = 2 * 60 * 1000L;
  private static final long QUOTA_FULL_MESSAGE_EVERY_MS = 10 * 60 * 1000L;

  private GeminiKeyStore() {}

  private static SharedPreferences prefs(Context context) {
    return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
  }

  public static String key(Context context) {
    return prefs(context).getString("key", "").trim();
  }

  public static void setKey(Context context, String key) {
    prefs(context).edit().putString("key", key == null ? "" : key.trim()).apply();
  }

  public static String model(Context context) {
    String m = prefs(context).getString("model", "").trim();
    return TextUtils.isEmpty(m) ? DEFAULT_MODEL : m;
  }

  public static void setModel(Context context, String model) {
    prefs(context).edit().putString("model", model == null ? "" : model.trim()).apply();
  }

  public static boolean promptDone(Context context) {
    return prefs(context).getBoolean("prompt_done", false);
  }

  public static void setPromptDone(Context context) {
    prefs(context).edit().putBoolean("prompt_done", true).apply();
  }

  /** Shows the key box only once ever; afterwards just a short reminder. */
  public static void onMissingKey(Context context) {
    if (!promptDone(context)) {
      setPromptDone(context);
      Intent intent = new Intent(context.getApplicationContext(), GeminiKeyActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.getApplicationContext().startActivity(intent);
    } else {
      toast(context, "Gemini key is not set. Add it in settings, Recognition, Gemini.");
    }
  }

  public static void onInvalidKey(Context context) {
    toast(context, "Gemini key is not valid. Check it in settings, Recognition, Gemini.");
  }

  /** Called when Gemini answers that the quota is finished. */
  public static void onQuotaFinished(Context context) {
    SharedPreferences p = prefs(context);
    long now = System.currentTimeMillis();
    long lastFull = p.getLong("quota_full_msg", 0L);
    p.edit().putLong("quota_until", now + QUOTA_BLOCK_MS).apply();
    if (now - lastFull > QUOTA_FULL_MESSAGE_EVERY_MS) {
      p.edit().putLong("quota_full_msg", now).apply();
      toast(context, "Your Gemini quota is finished. Please try again later.");
    } else {
      toast(context, "Quota finished");
    }
  }

  public static boolean inQuotaCooldown(Context context) {
    return System.currentTimeMillis() < prefs(context).getLong("quota_until", 0L);
  }

  public static void quotaShortMessage(Context context) {
    toast(context, "Quota finished");
  }

  private static void toast(final Context context, final String message) {
    final Context app = context.getApplicationContext();
    new Handler(Looper.getMainLooper())
        .post(() -> Toast.makeText(app, message, Toast.LENGTH_LONG).show());
  }
}
