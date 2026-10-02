/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.preference.base;

import com.google.android.accessibility.talkback.R;

/** MS Screen Reader: operation settings (navigation, clicking, volume keys, shake). */
public class MsOperationFragment extends TalkbackBaseFragment {
  public MsOperationFragment() {
    super(R.xml.ms_operation_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.ms_operation_title);
  }
}
