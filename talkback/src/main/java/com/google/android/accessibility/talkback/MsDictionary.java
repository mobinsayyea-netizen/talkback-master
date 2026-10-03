/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * MS Screen Reader: user dictionary. Each line is "word=spoken text". Applied to spoken text just
 * before it reaches the voice.
 */
public final class MsDictionary {
  public static final String K_USE = "ms_rd_dict_use";
  public static final String K_REGEX = "ms_rd_dict_regex";
  public static final String K_TEXT = "ms_rd_dict_text";

  private static String cachedText = null;
  private static boolean cachedRegex = false;
  private static List<Pattern> cachedPatterns = new ArrayList<>();
  private static List<String> cachedReplacements = new ArrayList<>();

  private MsDictionary() {}

  /** Returns the text with dictionary replacements applied, or the same object if nothing changed. */
  public static CharSequence apply(Context context, CharSequence text) {
    if (context == null || TextUtils.isEmpty(text)) {
      return text;
    }
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    if (!prefs.getBoolean(K_USE, false)) {
      return text;
    }
    String raw = prefs.getString(K_TEXT, "");
    boolean regex = prefs.getBoolean(K_REGEX, false);
    load(raw, regex);
    if (cachedPatterns.isEmpty()) {
      return text;
    }
    String out = text.toString();
    for (int i = 0; i < cachedPatterns.size(); i++) {
      try {
        out =
            cachedPatterns
                .get(i)
                .matcher(out)
                .replaceAll(Matcher.quoteReplacement(cachedReplacements.get(i)));
      } catch (RuntimeException e) {
        // Skip a rule that fails.
      }
    }
    return out.equals(text.toString()) ? text : out;
  }

  private static synchronized void load(String raw, boolean regex) {
    if (raw == null) {
      raw = "";
    }
    if (raw.equals(cachedText) && regex == cachedRegex) {
      return;
    }
    cachedText = raw;
    cachedRegex = regex;
    cachedPatterns = new ArrayList<>();
    cachedReplacements = new ArrayList<>();
    for (String line : raw.split("\\r?\\n")) {
      int eq = line.indexOf('=');
      if (eq <= 0) {
        continue;
      }
      String from = line.substring(0, eq).trim();
      String to = line.substring(eq + 1).trim();
      if (from.isEmpty()) {
        continue;
      }
      try {
        Pattern p =
            regex
                ? Pattern.compile(from)
                : Pattern.compile(Pattern.quote(from), Pattern.CASE_INSENSITIVE);
        cachedPatterns.add(p);
        cachedReplacements.add(to);
      } catch (PatternSyntaxException e) {
        // Skip a rule with a broken expression.
      }
    }
  }
}
