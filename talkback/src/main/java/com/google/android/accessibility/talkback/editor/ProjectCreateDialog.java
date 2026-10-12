package com.google.android.accessibility.talkback.editor;

import android.content.Context;

/**
 * Entry point used by the screen reader menu (Create extension / Create tool). Like Jieshuo: the
 * name is asked first, then the text editor opens with the big text box.
 */
public class ProjectCreateDialog {
  public static void show(Context context, boolean isExtension) {
    ExtensionListActivity.show(context, isExtension ? "ext" : "tool", true);
  }
}
