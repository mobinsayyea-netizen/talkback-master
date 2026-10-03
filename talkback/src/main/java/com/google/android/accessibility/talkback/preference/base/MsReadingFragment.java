/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.preference.base;

import com.google.android.accessibility.talkback.R;

/** MS Screen Reader: ms_reading_title. */
public class MsReadingFragment extends TalkbackBaseFragment {
  public MsReadingFragment() {
    super(R.xml.ms_reading_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.ms_reading_title);
  }
}
