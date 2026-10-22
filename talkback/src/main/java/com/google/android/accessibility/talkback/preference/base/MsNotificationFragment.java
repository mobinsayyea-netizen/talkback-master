/*
 * Copyright 2026 Advanced Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.preference.base;

import com.google.android.accessibility.talkback.R;

/** Advanced Screen Reader: Notification settings screen. */
public class MsNotificationFragment extends TalkbackBaseFragment {
  public MsNotificationFragment() {
    super(R.xml.ms_notification_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.ms_notification_title);
  }
}
