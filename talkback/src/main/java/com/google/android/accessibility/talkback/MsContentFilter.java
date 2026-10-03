/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * MS Screen Reader: content filtering. Drops speech that matches the user's blacklist, and
 * (optionally) speech made only of symbols.
 *
 * <p>Blacklist lines: plain text = the whole spoken text must be equal (ignoring case);
 * "contains:word" = spoken text contains word; "re:pattern" = regular expression found in text.
 */
public final class MsContentFilter {
  public static final String K_BLACKLIST = "ms_rd_filter_blacklist";
  public static final String K_SMALL = "ms_rd_filter_small";
  public static final String K_AUTO_BLACKLIST = "ms_rd_auto_blacklist";

  private MsContentFilter() {}

  /** Returns "" when the text must not be spoken, otherwise the same text. */
  public static CharSequence apply(Context context, CharSequence text) {
    if (context == null || TextUtils.isEmpty(text)) {
      return text;
    }
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    String s = text.toString();
    if (prefs.getBoolean(K_SMALL, false) && s.trim().length() > 1 && !hasLetterOrDigit(s)) {
      return "";
    }
    String raw = prefs.getString(K_BLACKLIST, "");
    if (!TextUtils.isEmpty(raw) && matches(raw, s, false)) {
      return "";
    }
    return text;
  }

  /** True when a chat message text contains any word from the auto-reading blacklist. */
  public static boolean autoBlocked(Context context, CharSequence text) {
    if (context == null || TextUtils.isEmpty(text)) {
      return false;
    }
    String raw =
        SharedPreferencesUtils.getSharedPreferences(context).getString(K_AUTO_BLACKLIST, "");
    return !TextUtils.isEmpty(raw) && matches(raw, text.toString(), true);
  }

  private static boolean hasLetterOrDigit(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (Character.isLetterOrDigit(s.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  private static boolean matches(String raw, String text, boolean defaultContains) {
    String lower = text.toLowerCase();
    for (String line : raw.split("\\r?\\n")) {
      String l = line.trim();
      if (l.isEmpty()) {
        continue;
      }
      try {
        if (l.startsWith("re:")) {
          if (Pattern.compile(l.substring(3)).matcher(text).find()) {
            return true;
          }
        } else if (l.startsWith("contains:")) {
          String w = l.substring(9).trim().toLowerCase();
          if (!w.isEmpty() && lower.contains(w)) {
            return true;
          }
        } else if (defaultContains) {
          if (lower.contains(l.toLowerCase())) {
            return true;
          }
        } else if (lower.trim().equals(l.toLowerCase())) {
          return true;
        }
      } catch (PatternSyntaxException e) {
        // Skip a bad pattern.
      }
    }
    return false;
  }
}
