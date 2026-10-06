/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.preference.base;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.preference.Preference;
import com.google.android.accessibility.talkback.training.OnboardingInitiator;
import com.google.android.accessibility.talkback.R;

/** MS Screen Reader settings group: Other Settings. */
public class MsGroupOtherFragment extends TalkbackBaseFragment {
  public MsGroupOtherFragment() {
    super(R.xml.ms_group_9_other);
  }

  @Override
  public CharSequence getTitle() {
    return "Other Settings";
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);
    Context context = getContext();
    if (context == null) {
      return;
    }
    Preference policy = findPreferenceByResId(R.string.pref_policy_key);
    if (policy != null) {
      PreferenceActionHelper.assignWebIntentToPreference(
          this, policy, PreferenceActionHelper.WebPage.WEB_PAGE_PRIVACY_POLICY);
    }
    Preference whatsNew = findPreferenceByResId(R.string.pref_new_feature_in_talkback_entry_point_key);
    if (whatsNew != null) {
      whatsNew.setIntent(OnboardingInitiator.createOnboardingIntentForSettings(context));
    }
    Preference feedback = findPreference("ms_g9_feedback");
    if (feedback != null) {
      feedback.setIntent(
          new Intent(
              Intent.ACTION_VIEW,
              Uri.parse("https://github.com/mobinsayyea-netizen/talkback-master/issues")));
    }
  }
}
