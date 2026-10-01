package com.google.android.accessibility.talkback.editor;

import android.content.Context;
import android.content.Intent;

/** Entry point used by the screen reader menu: opens the editor in "create" mode. */
public class ProjectCreateDialog {
  public static void show(Context context, boolean isExtension) {
    Intent intent = new Intent(context, LuaEditorActivity.class);
    intent.putExtra(LuaEditorActivity.EXTRA_CREATE_TYPE, isExtension ? "extension" : "tool");
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    context.startActivity(intent);
  }
}
