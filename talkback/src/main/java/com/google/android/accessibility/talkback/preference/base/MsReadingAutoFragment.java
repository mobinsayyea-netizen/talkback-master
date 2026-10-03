/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.preference.base;

import com.google.android.accessibility.talkback.R;

/** MS Screen Reader: ms_rd_auto_title. */
public class MsReadingAutoFragment extends TalkbackBaseFragment {
  public MsReadingAutoFragment() {
    super(R.xml.ms_reading_auto_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.ms_rd_auto_title);
  }
}
