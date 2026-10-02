/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */

package com.google.android.accessibility.talkback.preference.base;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.utils.output.TextToSpeechUtils;
import java.util.ArrayList;
import java.util.List;

/** Fills the engine list and wires the "system TTS settings" button for the MS TTS screens. */
final class MsTtsEngineHelper {
  private MsTtsEngineHelper() {}

  static void setUpEngineList(Context context, ListPreference pref) {
    if (pref == null) {
      return;
    }
    PackageManager pm = context.getPackageManager();
    List<String> engines = new ArrayList<>();
    TextToSpeechUtils.reloadInstalledTtsEngines(pm, engines);
    List<CharSequence> labels = new ArrayList<>();
    List<CharSequence> values = new ArrayList<>();
    labels.add(context.getString(R.string.ms_tts_engine_system_default));
    values.add("");
    for (String pkg : engines) {
      CharSequence label = pkg;
      try {
        label = pm.getApplicationInfo(pkg, 0).loadLabel(pm);
      } catch (PackageManager.NameNotFoundException e) {
        // Keep the package name as label.
      }
      labels.add(label);
      values.add(pkg);
    }
    pref.setEntries(labels.toArray(new CharSequence[0]));
    pref.setEntryValues(values.toArray(new CharSequence[0]));
    if (pref.getValue() == null) {
      pref.setValue("");
    }
  }

  static void setUpSystemSettingsButton(Context context, Preference pref) {
    if (pref == null) {
      return;
    }
    Intent intent = new Intent(TalkBackService.INTENT_TTS_SETTINGS);
    if (context.getPackageManager().resolveActivity(intent, 0) == null) {
      intent = new Intent("android.settings.TTS_SETTINGS");
    }
    pref.setIntent(intent);
  }
}
