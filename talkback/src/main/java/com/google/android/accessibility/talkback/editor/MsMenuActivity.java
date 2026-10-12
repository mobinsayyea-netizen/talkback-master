/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

/** The window of the Main, Multi-column, Functions and Recognition menus. Not compiled yet. */
public class MsMenuActivity extends Activity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Intent in = getIntent();
    final String[] labels = in.getStringArrayExtra("labels");
    final String[] kinds = in.getStringArrayExtra("kinds");
    final String[] names = in.getStringArrayExtra("names");
    if (labels == null || kinds == null || names == null) {
      finish();
      return;
    }
    int cols = in.getIntExtra("cols", 0);
    CharSequence[] shown = new CharSequence[labels.length];
    for (int i = 0; i < labels.length; i++) {
      shown[i] =
          cols > 0
              ? "Row " + (i / cols + 1) + ", column " + (i % cols + 1) + ": " + labels[i]
              : labels[i];
    }
    final android.content.Context app = getApplicationContext();
    new AlertDialog.Builder(this)
        .setTitle(in.getStringExtra("title"))
        .setItems(
            shown,
            (d, which) -> {
              finish();
              MsMenus.runAfterClose(app, kinds[which], names[which], labels[which]);
            })
        .setNegativeButton("Cancel", (d, w) -> finish())
        .setOnCancelListener(d -> finish())
        .show();
  }
}
