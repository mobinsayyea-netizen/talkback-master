/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.preference.base;

import com.google.android.accessibility.talkback.R;

/** MS Screen Reader: scenario voices (chat messages). */
public class MsScenarioFragment extends TalkbackBaseFragment {
  public MsScenarioFragment() {
    super(R.xml.ms_scenario_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.ms_scenario_title);
  }
}
