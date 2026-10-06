/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.preference.base;


import com.google.android.accessibility.talkback.R;

/** MS Screen Reader settings group: Date & Time Settings. */
public class MsGroupDateTimeFragment extends TalkbackBaseFragment {
  public MsGroupDateTimeFragment() {
    super(R.xml.ms_group_7_datetime);
  }

  @Override
  public CharSequence getTitle() {
    return "Date & Time Settings";
  }

}
