/*
 * Copyright 2026 Advanced Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.preference.base;


import com.google.android.accessibility.talkback.R;

/** Advanced Screen Reader settings group: Reading & Verbosity Settings. */
public class MsGroupReadingFragment extends TalkbackBaseFragment {
  public MsGroupReadingFragment() {
    super(R.xml.ms_group_4_reading);
  }

  @Override
  public CharSequence getTitle() {
    return "Reading & Verbosity Settings";
  }

}
