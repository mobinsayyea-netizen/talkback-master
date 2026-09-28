package com.google.android.accessibility.talkback.soundtheme;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * "Sound theme settings" (like Jieshuo's "Sound scheme settings"): opens the per-sound list of the
 * currently active theme, so there is no need to pick a theme first.
 */
public class SoundThemeSettingsActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    SoundThemeManager manager = new SoundThemeManager(this);
    String activeId = manager.getActiveThemeId();
    String activeName = "Default";
    for (SoundThemeManager.Theme theme : manager.getThemes()) {
      if (theme.id.equals(activeId)) {
        activeName = theme.name;
        break;
      }
    }
    Intent intent = new Intent(this, SoundThemeCustomizeActivity.class);
    intent.putExtra(SoundThemeCustomizeActivity.EXTRA_THEME_ID, activeId);
    intent.putExtra(SoundThemeCustomizeActivity.EXTRA_THEME_NAME, activeName);
    startActivity(intent);
    finish();
  }
}
