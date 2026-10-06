/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.preference.base;


import com.google.android.accessibility.talkback.R;

/** MS Screen Reader settings group: Display Settings. */
public class MsGroupDisplayFragment extends TalkbackBaseFragment {
  public MsGroupDisplayFragment() {
    super(R.xml.ms_group_3_display);
  }

  @Override
  public CharSequence getTitle() {
    return "Display Settings";
  }

}
