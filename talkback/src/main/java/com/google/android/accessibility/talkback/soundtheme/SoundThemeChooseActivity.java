package com.google.android.accessibility.talkback.soundtheme;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import java.util.List;

/**
 * "Sound theme" (like Jieshuo's "Sound scheme"): a single-choice list of the Default theme and every
 * added theme. Picking one makes it the active theme.
 */
public class SoundThemeChooseActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final SoundThemeManager manager = new SoundThemeManager(this);
    final List<SoundThemeManager.Theme> themes = manager.getThemes();
    String[] names = new String[themes.size()];
    int checked = 0;
    String activeId = manager.getActiveThemeId();
    for (int i = 0; i < themes.size(); i++) {
      names[i] = themes.get(i).name;
      if (themes.get(i).id.equals(activeId)) {
        checked = i;
      }
    }
    new AlertDialog.Builder(this)
        .setTitle("Sound theme")
        .setSingleChoiceItems(
            names,
            checked,
            (dialog, which) -> {
              SoundThemeManager.Theme theme = themes.get(which);
              manager.setActiveThemeId(theme.id);
              dialog.dismiss();
            })
        .setNegativeButton("Cancel", null)
        .setOnDismissListener(dialog -> finish())
        .show();
  }
}
