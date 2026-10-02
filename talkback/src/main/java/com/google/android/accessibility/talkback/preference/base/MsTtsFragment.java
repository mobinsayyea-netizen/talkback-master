/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */

package com.google.android.accessibility.talkback.preference.base;

import android.os.Bundle;
import androidx.preference.ListPreference;
import com.google.android.accessibility.talkback.R;

/** MS Screen Reader: main TTS settings (engine, speed, pitch, volume, audio focus, proximity). */
public class MsTtsFragment extends TalkbackBaseFragment {
  public MsTtsFragment() {
    super(R.xml.ms_tts_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.ms_tts_title);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);
    MsTtsEngineHelper.setUpEngineList(getContext(), (ListPreference) findPreference("ms_tts_engine"));
    MsTtsEngineHelper.setUpSystemSettingsButton(getContext(), findPreference("ms_tts_system_settings"));
  }
}
