/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.preference.base;

import android.os.Bundle;
import androidx.preference.ListPreference;
import com.google.android.accessibility.talkback.R;

/** MS Screen Reader settings group: TTS Settings. */
public class MsGroupTtsFragment extends TalkbackBaseFragment {
  public MsGroupTtsFragment() {
    super(R.xml.ms_group_1_tts);
  }

  @Override
  public CharSequence getTitle() {
    return "TTS Settings";
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);
    MsTtsEngineHelper.setUpEngineList(getContext(), (ListPreference) findPreference("ms_tts_engine"));
    MsTtsEngineHelper.setUpSystemSettingsButton(getContext(), findPreference("ms_tts_system_settings"));
    MsTtsEngineHelper.setUpEngineList(
        getContext(), (ListPreference) findPreference("ms_async_tts_engine"));
  }
}
