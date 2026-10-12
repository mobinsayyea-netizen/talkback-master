/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.android.accessibility.utils.SharedPreferencesUtils;

/** Three switches for extensions (settings screen: Other > Extensions). All default to on. */
public final class MsExtSettings {
  private MsExtSettings() {}

  /** Run extensions that listen for "service_started" when the screen reader starts. */
  public static final String KEY_AUTOSTART = "ms_ext_autostart";
  /** Voice assistant may run extensions (custom voice commands of kind extension). */
  public static final String KEY_VOICE_EXT = "ms_ext_voice_allow_ext";
  /** Voice assistant may run tools. */
  public static final String KEY_VOICE_TOOL = "ms_ext_voice_allow_tool";

  private static SharedPreferences prefs(Context c) {
    return SharedPreferencesUtils.getSharedPreferences(c);
  }

  public static boolean autostart(Context c) {
    return prefs(c).getBoolean(KEY_AUTOSTART, true);
  }

  public static boolean voiceMayRun(Context c, String kind) {
    return prefs(c).getBoolean("tool".equals(kind) ? KEY_VOICE_TOOL : KEY_VOICE_EXT, true);
  }
}
