/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.preference.base;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.utils.SharedPreferencesUtils;

/**
 * Typing settings. Step 20: five switches now drive TalkBack's own typing settings (echo of typed
 * characters and words, capital letters, example words for letters, double tap to type). The
 * switches show TalkBack's current values when the screen opens. Not compiled yet.
 */
public class MsJxAdvTypingSettingsFragment extends MsJxBaseFragment {
  private static final String K_CHARS = "ms_jx_use_input_method_speak_commit";
  private static final String K_WORDS = "ms_jx_use_input_method_speak_commit_space";
  private static final String K_CAPITAL = "ms_jx_use_pinyin_speak";
  private static final String K_PHONETIC = "ms_jx_use_input_method_speak_word";
  private static final String K_DOUBLE_TAP = "ms_jx_use_input_method_double_tap";

  public MsJxAdvTypingSettingsFragment() {
    super(R.xml.ms_jx_adv_typing_settings);
  }

  @Override
  public CharSequence getTitle() {
    return "Typing settings";
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);
    final Context context = getContext();
    if (context == null) {
      return;
    }
    final SharedPreferences tb = SharedPreferencesUtils.getSharedPreferences(context);
    final String echoKey = context.getString(R.string.pref_keyboard_echo_on_screen_key);
    final String echoPhysicalKey = context.getString(R.string.pref_keyboard_echo_physical_key);
    final String capitalKey = context.getString(R.string.pref_capital_letters_key);
    final String phoneticKey = context.getString(R.string.pref_phonetic_letters_key);
    final String confirmKey = context.getString(R.string.pref_typing_confirmation_key);

    // Show what TalkBack really does now. Echo values: 0 = characters and words, 1 = characters,
    // 3 = words, 2 = nothing. Capital letters: 0 = nothing. Typing confirmation: 0 = double tap.
    String echo = tb.getString(echoKey, context.getString(R.string.pref_keyboard_echo_default));
    setChecked(K_CHARS, "0".equals(echo) || "1".equals(echo));
    setChecked(K_WORDS, "0".equals(echo) || "3".equals(echo));
    setChecked(
        K_CAPITAL,
        !"0".equals(tb.getString(capitalKey, context.getString(R.string.pref_capital_letters_default))));
    setChecked(
        K_PHONETIC,
        tb.getBoolean(phoneticKey, context.getResources().getBoolean(R.bool.pref_phonetic_letters_default)));
    setChecked(
        K_DOUBLE_TAP,
        "0".equals(tb.getString(confirmKey, context.getString(R.string.pref_typing_confirmation_default))));

    Preference.OnPreferenceChangeListener echoListener =
        (p, value) -> {
          boolean on = Boolean.TRUE.equals(value);
          boolean chars = K_CHARS.equals(p.getKey()) ? on : isChecked(K_CHARS);
          boolean words = K_WORDS.equals(p.getKey()) ? on : isChecked(K_WORDS);
          String v = chars && words ? "0" : chars ? "1" : words ? "3" : "2";
          tb.edit().putString(echoKey, v).putString(echoPhysicalKey, v).apply();
          return true;
        };
    listen(K_CHARS, echoListener);
    listen(K_WORDS, echoListener);
    listen(
        K_CAPITAL,
        (p, value) -> {
          // 1 = say "capital", 0 = do nothing (TalkBack also has pitch and sound, set elsewhere).
          tb.edit().putString(capitalKey, Boolean.TRUE.equals(value) ? "1" : "0").apply();
          return true;
        });
    listen(
        K_PHONETIC,
        (p, value) -> {
          tb.edit().putBoolean(phoneticKey, Boolean.TRUE.equals(value)).apply();
          return true;
        });
    listen(
        K_DOUBLE_TAP,
        (p, value) -> {
          // On = double tap to type. Off = lift finger to type.
          tb.edit().putString(confirmKey, Boolean.TRUE.equals(value) ? "0" : "1").apply();
          return true;
        });
  }

  private void setChecked(String key, boolean value) {
    Preference p = findPreference(key);
    if (p instanceof SwitchPreferenceCompat) {
      ((SwitchPreferenceCompat) p).setChecked(value);
    }
  }

  private boolean isChecked(String key) {
    Preference p = findPreference(key);
    return p instanceof SwitchPreferenceCompat && ((SwitchPreferenceCompat) p).isChecked();
  }

  private void listen(String key, Preference.OnPreferenceChangeListener l) {
    Preference p = findPreference(key);
    if (p != null) {
      p.setOnPreferenceChangeListener(l);
    }
  }
}
