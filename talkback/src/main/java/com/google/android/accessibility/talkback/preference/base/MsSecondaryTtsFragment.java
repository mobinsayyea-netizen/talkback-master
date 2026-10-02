/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */

package com.google.android.accessibility.talkback.preference.base;

import android.os.Bundle;
import androidx.preference.ListPreference;
import com.google.android.accessibility.talkback.R;

/** MS Screen Reader: secondary TTS settings (values are saved; used by the second voice). */
public class MsSecondaryTtsFragment extends TalkbackBaseFragment {
  public MsSecondaryTtsFragment() {
    super(R.xml.ms_secondary_tts_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.ms_secondary_tts_title);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);
    MsTtsEngineHelper.setUpEngineList(
        getContext(), (ListPreference) findPreference("ms_async_tts_engine"));
    MsTtsEngineHelper.setUpSystemSettingsButton(
        getContext(), findPreference("ms_async_tts_system_settings"));
  }
}
