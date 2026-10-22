/*
 * Copyright 2026 Advanced Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.preference.base;


import com.google.android.accessibility.talkback.R;

/** Advanced Screen Reader settings group: Shortcut Settings. */
public class MsGroupShortcutsFragment extends TalkbackBaseFragment {
  public MsGroupShortcutsFragment() {
    super(R.xml.ms_group_5_shortcuts);
  }

  @Override
  public CharSequence getTitle() {
    return "Shortcut Settings";
  }

}
