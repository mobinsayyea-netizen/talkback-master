/*
 * Copyright 2026 Advanced Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.preference.base;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import androidx.preference.Preference;
import com.google.android.accessibility.utils.PreferenceSettingsUtils;
import com.google.android.accessibility.utils.monitor.InputDeviceMonitor;
import com.google.android.accessibility.talkback.R;

/** Advanced Screen Reader settings group: Typing Settings. */
public class MsGroupTypingFragment extends TalkbackBaseFragment {
  public MsGroupTypingFragment() {
    super(R.xml.ms_group_6_typing);
  }

  @Override
  public CharSequence getTitle() {
    return "Typing Settings";
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);
    Context context = getContext();
    if (context == null) {
      return;
    }
    Preference preference = findPreferenceByResId(R.string.pref_physical_keyboard_key);
    if (preference == null) {
      return;
    }
    if (new InputDeviceMonitor(context).hasPhysicalKeyboard()) {
      Intent intent = new Intent(Settings.ACTION_HARD_KEYBOARD_SETTINGS);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
      if (PreferenceSettingsUtils.canHandleIntent(context, intent)) {
        preference.setIntent(intent);
      }
    } else {
      preference.setVisible(false);
    }
  }
}
