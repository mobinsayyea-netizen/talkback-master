/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.accessibility.AccessibilityNodeInfo;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.output.TextToSpeechUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Table of Jieshuo command ids -> one small handler each. To add a command, add one line in the
 * static block. Lookup is by Jieshuo's string id (same ids as JxCommands.TABLE). Handlers must not
 * throw; JxCommands already wraps everything in try/catch. Not compiled yet.
 */
final class JxCommandTable {
  private JxCommandTable() {}

  interface Handler {
    boolean run(LuaServiceApi api, Context ctx, TalkBackService s, AccessibilityNodeInfo n);
  }

  private static final Map<String, Handler> MAP = new HashMap<>();

  static {
    // ---- speech
    MAP.put("command_speed_to_fast", (api, ctx, s, n) -> s.msChangeSpeechRate(true));
    MAP.put("command_speed_to_slow", (api, ctx, s, n) -> s.msChangeSpeechRate(false));
    MAP.put("command_set_tts_engine", (api, ctx, s, n) -> openTtsSettings(ctx));
    MAP.put("command_verbatim_speak2", (api, ctx, s, n) -> spell(api, n));

    // ---- switches (Step 8): suspend / resume browse by touch (auto-resumes after 2 minutes)
    MAP.put("command_disable_touch", (api, ctx, s, n) -> s.msToggleTouchBrowse());

    // Step 9: switch to the next installed TTS engine (the setting is read by FailoverTextToSpeech).
    MAP.put("command_toggle_tts_engine", (api, ctx, s, n) -> nextTtsEngine(api, ctx));
    // Step 9: "Custom voice commands" folder and the automatic click builder
    for (String id : new String[] {"directory_cmd"}) {
      MAP.put(id, (api, ctx, s, n) -> VoiceCommandsActivity.show(ctx));
    }

    // Step 10: tools list, system timers, keyboard picker
    for (String id : new String[] {"directory_tools", "tool_title"}) {
      MAP.put(id, (api, ctx, s, n) -> JxPickerActivity.show(ctx, JxPickerActivity.KIND_TOOLS));
    }
    MAP.put("directory_timer", (api, ctx, s, n) -> openTimers(ctx));
    MAP.put("input_method", (api, ctx, s, n) -> pickKeyboard(ctx));

    // Step 11: read the screen (group C)
    MAP.put("command_ocr_text", (api, ctx, s, n) -> MsRecognition.readScreen(api, s));
    MAP.put("command_verification", (api, ctx, s, n) -> MsRecognition.captcha(api, s, n));
    MAP.put("command_take_screenshot2", (api, ctx, s, n) -> MsRecognition.saveFocusShot(api, ctx, s, n));
    MAP.put("command_recognition_caption", (api, ctx, s, n) -> MsRecognition.toggleSubtitles(api, s));
    MAP.put("voice_picture_description", (api, ctx, s, n) -> MsRecognition.askByVoice(api, ctx, s, n, false));
    MAP.put("voice_picture_description2", (api, ctx, s, n) -> MsRecognition.askByVoice(api, ctx, s, n, true));
    MAP.put("command_virtual_table", (api, ctx, s, n) -> MsRecognition.identifyForm(api, s));

    // Step 11: dictation (group G)
    MAP.put("command_voice_input", (api, ctx, s, n) -> MsDictation.once(api, ctx, s, n));
    MAP.put("voice_input_mode", (api, ctx, s, n) -> MsDictation.toggleContinuous(api, ctx, s, n));

    // Step 12: navigation types, auto browsing backwards, text screens, long hold (groups D, E)
    MAP.put("select_granularity_mode", (api, ctx, s, n) -> JxPickerActivity.show(ctx, JxPickerActivity.KIND_NAVTYPE));
    MAP.put("to_next_granularity_item", (api, ctx, s, n) -> MsNavModes.step(api, ctx, s, n, false));
    MAP.put("to_previous_granularity_item", (api, ctx, s, n) -> MsNavModes.step(api, ctx, s, n, true));
    MAP.put("auto_previous", (api, ctx, s, n) -> MsNavModes.toggleAutoBackward(api));
    MAP.put("command_long_hold", (api, ctx, s, n) -> MsNavModes.longHold(api, n));
    MAP.put("all_text_title", (api, ctx, s, n) -> TextViewerActivity.show(ctx, TextViewerActivity.MODE_VIEW, n));
    MAP.put("command_long_edit", (api, ctx, s, n) -> TextViewerActivity.show(ctx, TextViewerActivity.MODE_EDIT, n));
    MAP.put("command_long_copy", (api, ctx, s, n) -> TextViewerActivity.show(ctx, TextViewerActivity.MODE_LIST, n));

    // Step 13: switches that need hooks in the service, and the focus monitor (groups B, F)
    MAP.put("command_pause_gesture", (api, ctx, s, n) -> s.msToggleGesturesPaused());
    MAP.put("command_disable_main_tts", (api, ctx, s, n) -> s.msToggleMainTts());
    MAP.put("command_disable_feedback", (api, ctx, s, n) -> s.msToggleFeedbackSuspend());
    MAP.put("command_false_touch_prevention", (api, ctx, s, n) -> MsTouchLock.toggle(api, s));
    MAP.put("add_node_changed_list", (api, ctx, s, n) -> MsFocusMonitor.addCurrent(api, ctx, n));
    MAP.put("node_changed_list_manager", (api, ctx, s, n) -> FocusMonitorsActivity.show(ctx));

    // ---- screen
    MAP.put("command_enable_dim_screen", (api, ctx, s, n) -> s.msDimScreen());

    // ---- recognition (these run the same code as the main-menu items)
    MAP.put("command_trans", (api, ctx, s, n) -> text(s, 2));
    MAP.put("command_ocr_focus", (api, ctx, s, n) -> text(s, 3));
    MAP.put("video_description", (api, ctx, s, n) -> s.msStartVideoDescription());

    // ---- reading
    MAP.put("command_auto_next", (api, ctx, s, n) -> start(s));

    // ---- sensors
    MAP.put("command_speak_light", (api, ctx, s, n) -> speakLight(api, ctx));
    // ---- Step 5: menus, screens and pickers that reuse existing features
    for (String id : new String[] {"directory_plugins", "plugin_title"}) {
      MAP.put(id, (api, ctx, s, n) -> JxPickerActivity.show(ctx, JxPickerActivity.KIND_EXTENSIONS));
    }
    MAP.put("app", (api, ctx, s, n) -> JxPickerActivity.show(ctx, JxPickerActivity.KIND_APPS));
    // Jieshuo has separate main / recognition / functions / per-app menus; here they are all
    // TalkBack's grouped menu (Recognition, Navigation, Functions, Extensions groups).
    for (String id :
        new String[] {
          "main_menu", "advanced_menu_title", "quick_menu_title", "action_menu_title", "command_node_menu"
        }) {
      MAP.put(id, (api, ctx, s, n) -> s.msShowMenu(false));
    }
    MAP.put("actions", (api, ctx, s, n) -> s.msShowMenu(true));
    for (String id : new String[] {"command_gesture_help", "command_gestures_edit"}) {
      MAP.put(id, (api, ctx, s, n) -> s.msOpenSettings(
          "com.google.android.accessibility.talkback.preference.base.TalkBackGestureShortcutPreferenceFragment"));
    }
    MAP.put("edit_dict", (api, ctx, s, n) -> s.msOpenSettings(
        "com.google.android.accessibility.talkback.preference.base.MsReadingDictFragment"));
    MAP.put("command_edit_node_name", (api, ctx, s, n) -> s.msOpenSettings(
        "com.google.android.accessibility.talkback.preference.base.MsReadingLabelFragment"));
    MAP.put("command_add_content_blacklist", (api, ctx, s, n) -> s.msOpenSettings(
        "com.google.android.accessibility.talkback.preference.base.MsReadingFilterFragment"));
    for (String id : new String[] {"clipboard_menu_title", "favorites_menu_title"}) {
      MAP.put(id, (api, ctx, s, n) -> openClipboard(ctx));
    }
    MAP.put("url_menu_title", (api, ctx, s, n) -> openUrl(api, ctx, n));
    for (String id :
        new String[] {"command_image_to_text", "picture_description", "command_recognition_icon"}) {
      MAP.put(id, (api, ctx, s, n) -> s.msDescribe(false));
    }
    for (String id : new String[] {"command_image_to_text2", "picture_description2"}) {
      MAP.put(id, (api, ctx, s, n) -> s.msDescribe(true));
    }
    // ---- Step 6: switches (they change a saved flag that the code reads) and modes
    MAP.put("command_disable_hotkey", (api, ctx, s, n) -> toggleFlag(api, ctx, "ms_cmd_disable_hotkey", "Hotkeys off", "Hotkeys on"));
    MAP.put("command_disable_async_tts", (api, ctx, s, n) -> toggleFlag(api, ctx, "ms_cmd_disable_async_tts", "Secondary voice off", "Secondary voice on"));
    MAP.put("command_raw_tap", (api, ctx, s, n) -> s.msPassThrough());
    MAP.put("command_selection_mode", (api, ctx, s, n) -> selectionToggle(s));
    MAP.put("command_read_mode", (api, ctx, s, n) -> start2(s));
    MAP.put("char_mode", (api, ctx, s, n) -> s.msSetGranularity(com.google.android.accessibility.utils.input.CursorGranularity.CHARACTER));
    MAP.put("command_node_mode", (api, ctx, s, n) -> s.msSetGranularity(com.google.android.accessibility.utils.input.CursorGranularity.DEFAULT));
    MAP.put("command_timer_mode", (api, ctx, s, n) -> timerStartOrReport(api));
    MAP.put("command_pasue_timer", (api, ctx, s, n) -> timerPauseOrResume(api));

    // Step 14: the last 30 commands (notifications, chat, node lists, switches, location ...)
    MsExtras.register(MAP);
  }

  /** Returns null when the id is not in the table. */
  static Boolean run(
      LuaServiceApi api, Context ctx, TalkBackService s, String id, AccessibilityNodeInfo n) {
    Handler h = MAP.get(id);
    return h == null ? null : h.run(api, ctx, s, n);
  }

  static boolean has(String id) {
    return MAP.containsKey(id);
  }

  private static boolean text(TalkBackService s, int action) {
    s.msRunTextAction(action);
    return true;
  }

  private static boolean start(TalkBackService s) {
    s.msStartReading(false);
    return true;
  }

  private static boolean openTtsSettings(Context ctx) {
    Intent i = new Intent("com.android.settings.TTS_SETTINGS");
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    ctx.startActivity(i);
    return true;
  }

  private static boolean spell(LuaServiceApi api, AccessibilityNodeInfo n) {
    String t = n == null ? null : api.getText(n);
    if (t == null || t.isEmpty()) {
      return false;
    }
    if (t.length() > 200) {
      t = t.substring(0, 200);
    }
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < t.length(); i++) {
      b.append(t.charAt(i)).append(", ");
    }
    api.speak(b.toString());
    return true;
  }

  private static boolean speakLight(final LuaServiceApi api, Context ctx) {
    final SensorManager sm = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
    Sensor light = sm == null ? null : sm.getDefaultSensor(Sensor.TYPE_LIGHT);
    if (light == null) {
      return false;
    }
    sm.registerListener(
        new SensorEventListener() {
          @Override
          public void onSensorChanged(SensorEvent e) {
            sm.unregisterListener(this);
            api.speak(Math.round(e.values[0]) + " lux");
          }

          @Override
          public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        },
        light,
        SensorManager.SENSOR_DELAY_NORMAL);
    return true;
  }

  private static boolean openClipboard(Context ctx) {
    try {
      Intent i =
          new Intent(ctx, com.google.android.accessibility.talkback.clipboard.ClipboardActivity.class);
      i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      ctx.startActivity(i);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /** Opens the first web address found in the focused text, else the one on the clipboard. */
  private static boolean openUrl(LuaServiceApi api, Context ctx, AccessibilityNodeInfo n) {
    String url = findUrl(n == null ? null : api.getText(n));
    if (url == null) {
      android.content.ClipboardManager cm =
          (android.content.ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
      if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
        CharSequence c = cm.getPrimaryClip().getItemAt(0).coerceToText(ctx);
        url = findUrl(c == null ? null : c.toString());
      }
    }
    if (url == null) {
      return false;
    }
    try {
      Intent i = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
      i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      ctx.startActivity(i);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private static String findUrl(String text) {
    if (text == null) {
      return null;
    }
    java.util.regex.Matcher m = java.util.regex.Pattern.compile("https?://[^\\s]+").matcher(text);
    return m.find() ? m.group() : null;
  }

  /** Flips a saved on/off flag and says which way it went. */
  private static boolean toggleFlag(LuaServiceApi api, Context ctx, String key, String onText, String offText) {
    android.content.SharedPreferences p =
        com.google.android.accessibility.utils.SharedPreferencesUtils.getSharedPreferences(ctx);
    boolean now = !p.getBoolean(key, false);
    p.edit().putBoolean(key, now).apply();
    api.speak(now ? onText : offText);
    return true;
  }

  private static boolean selectionOn = false;

  private static boolean selectionToggle(TalkBackService s) {
    boolean ok = s.msSelectionMode(!selectionOn);
    if (ok) {
      selectionOn = !selectionOn;
    }
    return ok;
  }

  private static boolean start2(TalkBackService s) {
    s.msStartReading(true);
    return true;
  }

  // ---- simple stopwatch: first call starts it, later calls say the time; "Pause timer" pauses/resumes
  private static long timerStartedAt = 0;
  private static long timerElapsedBefore = 0;
  private static boolean timerRunning = false;

  private static long timerNow() {
    return timerElapsedBefore + (timerRunning ? android.os.SystemClock.elapsedRealtime() - timerStartedAt : 0);
  }

  private static String timerText(long ms) {
    long sec = ms / 1000;
    return (sec / 60) + " minutes " + (sec % 60) + " seconds";
  }

  private static boolean timerStartOrReport(LuaServiceApi api) {
    if (!timerRunning && timerElapsedBefore == 0) {
      timerStartedAt = android.os.SystemClock.elapsedRealtime();
      timerRunning = true;
      api.speak("Timer started");
    } else {
      api.speak(timerText(timerNow()));
    }
    return true;
  }

  private static boolean timerPauseOrResume(LuaServiceApi api) {
    if (timerRunning) {
      timerElapsedBefore = timerNow();
      timerRunning = false;
      api.speak("Timer paused at " + timerText(timerElapsedBefore));
    } else if (timerElapsedBefore > 0) {
      timerStartedAt = android.os.SystemClock.elapsedRealtime();
      timerRunning = true;
      api.speak("Timer resumed");
    } else {
      return false;
    }
    return true;
  }

  /** Cycles ms_tts_engine through the installed engines and says the name of the new one. */
  private static boolean nextTtsEngine(LuaServiceApi api, Context ctx) {
    PackageManager pm = ctx.getPackageManager();
    List<String> engines = new ArrayList<>();
    TextToSpeechUtils.reloadInstalledTtsEngines(pm, engines);
    if (engines.size() < 2) {
      return false; // nothing to switch to
    }
    SharedPreferences sp = SharedPreferencesUtils.getSharedPreferences(ctx);
    String current = sp.getString("ms_tts_engine", "");
    if (current == null || current.isEmpty()) {
      current = android.provider.Settings.Secure.getString(
          ctx.getContentResolver(), android.provider.Settings.Secure.TTS_DEFAULT_SYNTH);
    }
    String next = engines.get((Math.max(0, engines.indexOf(current)) + 1) % engines.size());
    sp.edit().putString("ms_tts_engine", next).apply();
    CharSequence label = next;
    try {
      label = pm.getApplicationInfo(next, 0).loadLabel(pm);
    } catch (PackageManager.NameNotFoundException e) {
      // keep the package name
    }
    api.speak(String.valueOf(label));
    return true;
  }

  /** Opens the clock app's timer screen. */
  private static boolean openTimers(Context ctx) {
    try {
      Intent i = new Intent(android.provider.AlarmClock.ACTION_SHOW_TIMERS);
      i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      ctx.startActivity(i);
      return true;
    } catch (RuntimeException e) {
      return false; // no clock app that answers this
    }
  }

  /** Shows the system list of keyboards. */
  private static boolean pickKeyboard(Context ctx) {
    android.view.inputmethod.InputMethodManager m =
        (android.view.inputmethod.InputMethodManager)
            ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
    if (m == null) {
      return false;
    }
    m.showInputMethodPicker();
    return true;
  }
}
