/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.preference.base;

import com.google.android.accessibility.talkback.R;

/** MS Screen Reader: Advanced settings screen. */
public class MsAdvancedFragment extends TalkbackBaseFragment {
  public MsAdvancedFragment() {
    super(R.xml.ms_advanced_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.ms_advanced_title);
  }
}
