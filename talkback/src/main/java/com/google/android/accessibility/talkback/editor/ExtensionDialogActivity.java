/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.List;

/**
 * The default "Extension" function (Jieshuo: swipe up then left): a dialog with every extension,
 * then Manage, Custom voice commands and Cancel. Has no window of its own. Not compiled yet.
 */
public class ExtensionDialogActivity extends Activity {
  static boolean show(Context c) {
    try {
      Intent i = new Intent(c, ExtensionDialogActivity.class);
      i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
      c.startActivity(i);
      return true;
    } catch (RuntimeException e) {
      return false;
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final List<String> names = new ArrayList<>();
    java.io.File[] dirs = LuaPaths.extensions(this).listFiles();
    if (dirs != null) {
      java.util.Arrays.sort(dirs);
      for (java.io.File d : dirs) {
        if (d.isDirectory() && new java.io.File(d, "main.lua").isFile()) {
          names.add(d.getName());
        }
      }
    }
    final List<String> items = new ArrayList<>(names);
    final int manage = items.size();
    items.add("Manage");
    items.add("Custom voice commands");
    items.add("Cancel");
    new AlertDialog.Builder(this)
        .setTitle("Extension")
        .setItems(
            items.toArray(new String[0]),
            (d, which) -> {
              if (which < manage) {
                CustomVoiceCommands.run(
                    this, new CustomVoiceCommands.Entry("", "ext", names.get(which)));
              } else if (which == manage) {
                ExtensionListActivity.show(this, "ext", false);
              } else if (which == manage + 1) {
                startActivity(new Intent(this, VoiceCommandsActivity.class));
              }
              finish();
            })
        .setOnCancelListener(d -> finish())
        .show();
  }
}
