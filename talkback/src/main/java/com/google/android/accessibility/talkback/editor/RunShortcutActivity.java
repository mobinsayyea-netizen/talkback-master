/*
 * Copyright 2026 Advanced Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.app.Activity;
import android.os.Bundle;

/** Invisible activity behind a home screen shortcut: runs one extension or tool, then closes. */
public class RunShortcutActivity extends Activity {
  public static final String ACTION_RUN = "com.google.android.accessibility.talkback.RUN_EXTENSION";
  public static final String EXTRA_KIND = "kind";
  public static final String EXTRA_NAME = "name";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    String kind = getIntent().getStringExtra(EXTRA_KIND);
    String name = getIntent().getStringExtra(EXTRA_NAME);
    if (name != null && !name.isEmpty()) {
      CustomVoiceCommands.run(this, new CustomVoiceCommands.Entry("", "tool".equals(kind) ? "tool" : "ext", name));
    }
    finish();
  }
}
