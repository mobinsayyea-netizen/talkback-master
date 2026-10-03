/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.preference.base;

import com.google.android.accessibility.talkback.R;

/** MS Screen Reader: ms_rd_chars_title. */
public class MsReadingCharsFragment extends TalkbackBaseFragment {
  public MsReadingCharsFragment() {
    super(R.xml.ms_reading_chars_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.ms_rd_chars_title);
  }
}
