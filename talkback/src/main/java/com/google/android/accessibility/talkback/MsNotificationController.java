/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback;

import android.app.KeyguardManager;
import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import com.google.android.accessibility.utils.SharedPreferencesUtils;

/** MS Screen Reader: makes the "Notification settings" work. */
public final class MsNotificationController {
  private static final String P = "ms_notif_";

  private final TalkBackService service;
  private final SharedPreferences prefs;
  private final MsSecondaryTts secondary;

  public MsNotificationController(TalkBackService service, MsSecondaryTts secondary) {
    this.service = service;
    this.secondary = secondary;
    this.prefs = SharedPreferencesUtils.getSharedPreferences(service);
  }

  /**
   * Handles a notification-state event. Returns true if the event was fully handled here and TalkBack
   * must not process it again.
   */
  public boolean handle(AccessibilityEvent event, boolean fingerOnScreen) {
    CharSequence cls = event.getClassName();
    boolean isToast = cls != null && cls.toString().contains("Toast");
    if (isToast) {
      return !prefs.getBoolean(P + "read_toast", true);
    }
    Parcelable data = event.getParcelableData();
    if (!(data instanceof Notification)) {
      return false;
    }
    Notification n = (Notification) data;
    if ((n.flags & (Notification.FLAG_ONGOING_EVENT | Notification.FLAG_GROUP_SUMMARY)) != 0) {
      return true;
    }
    if (!allowedNow(fingerOnScreen)) {
      return true;
    }
    String text = buildText(event, n);
    if (TextUtils.isEmpty(text)) {
      return true;
    }
    text = MsContentFilter.apply(service, text).toString();
    if (TextUtils.isEmpty(text)) {
      return true;
    }
    boolean queue = prefs.getBoolean(P + "queue", false);
    boolean useSecondary = prefs.getBoolean(P + "use_secondary_tts", true);
    if (isChatApp(event.getPackageName())) {
      if (!prefs.getBoolean("ms_scn_chat_read", true)) {
        return true;
      }
      if (MsContentFilter.autoBlocked(service, text)) {
        return true;
      }
      useSecondary = "secondary".equals(prefs.getString("ms_scn_chat_voice", "secondary"));
    }
    if (useSecondary) {
      secondary.speak(text, !queue);
    } else {
      service.msSpeakMain(text, queue);
    }
    return true;
  }

  private boolean isChatApp(CharSequence pkg) {
    if (pkg == null) {
      return false;
    }
    String list =
        prefs.getString("ms_scn_chat_apps", "org.telegram.messenger,com.whatsapp,com.whatsapp.w4b");
    for (String s : list.split(",")) {
      if (s.trim().equals(pkg.toString())) {
        return true;
      }
    }
    return false;
  }

  private boolean allowedNow(boolean fingerOnScreen) {
    String mode = prefs.getString(P + "auto_read", "both");
    KeyguardManager km = (KeyguardManager) service.getSystemService(Context.KEYGUARD_SERVICE);
    boolean locked = km != null && km.isKeyguardLocked();
    if ("off".equals(mode)) {
      return false;
    }
    if ("locked".equals(mode) && !locked) {
      return false;
    }
    if ("unlocked".equals(mode) && locked) {
      return false;
    }
    if (prefs.getBoolean(P + "no_read_touching", true) && fingerOnScreen) {
      return false;
    }
    if (prefs.getBoolean(P + "no_read_in_call", true)) {
      AudioManager am = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
      if (am != null
          && (am.getMode() == AudioManager.MODE_IN_CALL
              || am.getMode() == AudioManager.MODE_IN_COMMUNICATION)) {
        return false;
      }
    }
    return true;
  }

  private String buildText(AccessibilityEvent event, Notification n) {
    StringBuilder sb = new StringBuilder();
    if (prefs.getBoolean(P + "read_source", true) && event.getPackageName() != null) {
      sb.append(appLabel(event.getPackageName().toString())).append(". ");
    }
    CharSequence title = null;
    CharSequence body = null;
    CharSequence big = null;
    CharSequence sub = null;
    if (n.extras != null) {
      title = n.extras.getCharSequence(Notification.EXTRA_TITLE);
      body = n.extras.getCharSequence(Notification.EXTRA_TEXT);
      big = n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
      sub = n.extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
    }
    if (TextUtils.isEmpty(title) && TextUtils.isEmpty(body) && !TextUtils.isEmpty(n.tickerText)) {
      body = n.tickerText;
    }
    boolean summaryOnly = prefs.getBoolean(P + "summary_only", true);
    append(sb, title);
    if (!summaryOnly && !TextUtils.isEmpty(big)) {
      append(sb, big);
    } else {
      append(sb, body);
    }
    if (!summaryOnly) {
      append(sb, sub);
    }
    if (sb.length() == 0 || (event.getText() != null && sb.length() < 3)) {
      for (CharSequence c : event.getText()) {
        append(sb, c);
      }
    }
    return sb.toString().trim();
  }

  private static void append(StringBuilder sb, CharSequence c) {
    if (!TextUtils.isEmpty(c)) {
      if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
        sb.append(' ');
      }
      sb.append(c).append('.');
    }
  }

  private String appLabel(String pkg) {
    try {
      PackageManager pm = service.getPackageManager();
      return pm.getApplicationInfo(pkg, 0).loadLabel(pm).toString();
    } catch (PackageManager.NameNotFoundException e) {
      return pkg;
    }
  }
}
