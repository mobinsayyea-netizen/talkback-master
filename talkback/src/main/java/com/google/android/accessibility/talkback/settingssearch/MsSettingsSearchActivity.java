/*
 * Copyright 2026 Advanced Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.settingssearch;

import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.android.talkback.TalkBackPreferencesActivity;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.utils.preference.BasePreferencesActivity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Simple search over every setting title and summary. Tapping a result opens its screen. */
public class MsSettingsSearchActivity extends Activity {
  private static final String BASE = "com.google.android.accessibility.talkback.preference.base.";

  private static final class Entry {
    final String title;
    final String summary;
    final String group;
    final String fragment;

    Entry(String title, String summary, String group, String fragment) {
      this.title = title;
      this.summary = summary;
      this.group = group;
      this.fragment = fragment;
    }

    String label() {
      return title + ", " + group;
    }
  }

  private final List<Entry> all = new ArrayList<>();
  private final List<Entry> shown = new ArrayList<>();
  private ArrayAdapter<String> adapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTitle("Search settings");
    buildIndex();

    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    int pad = (int) (16 * getResources().getDisplayMetrics().density);
    root.setPadding(pad, pad, pad, pad);

    EditText box = new EditText(this);
    box.setHint("Search settings");
    box.setSingleLine(true);
    box.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
    root.addView(box, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    ListView list = new ListView(this);
    adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<String>());
    list.setAdapter(adapter);
    root.addView(list, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    setContentView(root);

    list.setOnItemClickListener(
        (parent, view, position, id) -> {
          if (position < 0 || position >= shown.size()) {
            return;
          }
          Intent intent = new Intent(this, TalkBackPreferencesActivity.class);
          intent.putExtra(BasePreferencesActivity.FRAGMENT_NAME, shown.get(position).fragment);
          startActivity(intent);
        });
    box.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int a, int b, int c) {}

          @Override
          public void onTextChanged(CharSequence s, int a, int b, int c) {}

          @Override
          public void afterTextChanged(Editable s) {
            filter(s.toString());
          }
        });
    filter("");
  }

  private void filter(String query) {
    String q = query.trim().toLowerCase(Locale.ROOT);
    shown.clear();
    adapter.clear();
    for (Entry e : all) {
      String hay = (e.title + " " + e.summary + " " + e.group).toLowerCase(Locale.ROOT);
      if (q.isEmpty() || hay.contains(q)) {
        shown.add(e);
        adapter.add(e.label());
      }
    }
    if (shown.isEmpty()) {
      adapter.add("No settings found");
    }
    adapter.notifyDataSetChanged();
  }

  private void buildIndex() {
    add(R.xml.ms_group_1_tts, "TTS Settings", "MsGroupTtsFragment");
    add(R.xml.sound_and_vibration_preferences, "Feedback Settings", "SoundAndVibrationFragment");
    add(R.xml.focus_indicator_preferences, "Display Settings", "FocusIndicatorPrefFragment");
    add(R.xml.ms_group_3_display, "Display Settings", "MsGroupDisplayFragment");
    add(R.xml.latency_reduction_preferences, "Display Settings", "TalkBackLatencyReductionPreferenceFragment");
    add(R.xml.ms_group_4_reading, "Reading & Verbosity Settings", "MsGroupReadingFragment");
    add(R.xml.verbosity_preferences, "Reading & Verbosity Settings", "VerbosityPrefFragment");
    add(R.xml.ms_group_5_shortcuts, "Shortcut Settings", "MsGroupShortcutsFragment");
    add(R.xml.gesture_preferences, "Shortcut Settings", "TalkBackGestureShortcutPreferenceFragment");
    add(R.xml.ms_group_6_typing, "Typing Settings", "MsGroupTypingFragment");
    add(R.xml.on_screen_keyboard_preferences, "Typing Settings", "OnScreenKeyboardFragment");
    add(R.xml.ms_group_7_datetime, "Date & Time Settings", "MsGroupDateTimeFragment");
    add(R.xml.ms_group_8_notifications, "Notification Settings", "MsGroupNotificationsFragment");
    add(R.xml.ms_group_9_other, "Other Settings", "MsGroupOtherFragment");
    // BEGIN ms_jx index (generated by devtools/step19_gen/gen_settings.py)
    add(R.xml.ms_jx_adv, "Advanced settings", "MsJxAdvFragment");
    add(R.xml.ms_jx_adv_article_reading_mode_settings, "Advanced settings > Article reading mode settings", "MsJxAdvArticleReadingModeSettingsFragment");
    add(R.xml.ms_jx_adv_backup_and_restore, "Advanced settings > Backup and restore", "MsJxAdvBackupAndRestoreFragment");
    add(R.xml.ms_jx_adv_menu_customization, "Advanced settings > Menu customization", "MsJxAdvMenuCustomizationFragment");
    add(R.xml.ms_jx_adv_ocr_settings, "Advanced settings > OCR settings", "MsJxAdvOcrSettingsFragment");
    add(R.xml.ms_jx_adv_ocr_settings_baidu_ocr, "Advanced settings > OCR settings > Baidu OCR", "MsJxAdvOcrSettingsBaiduOcrFragment");
    add(R.xml.ms_jx_adv_operation_settings, "Advanced settings > Operation settings", "MsJxAdvOperationSettingsFragment");
    add(R.xml.ms_jx_adv_other_settings, "Advanced settings > Other settings", "MsJxAdvOtherSettingsFragment");
    add(R.xml.ms_jx_adv_power_management_settings, "Advanced settings > Power management settings", "MsJxAdvPowerManagementSettingsFragment");
    add(R.xml.ms_jx_adv_timer, "Advanced settings > Timer settings", "MsJxAdvTimerFragment");
    add(R.xml.ms_jx_adv_typing_settings, "Advanced settings > Typing settings", "MsJxAdvTypingSettingsFragment");
    add(R.xml.ms_jx_adv_voice, "Advanced settings > Voice assistant and translation settings", "MsJxAdvVoiceFragment");
    add(R.xml.ms_jx_op, "Operation settings", "MsJxOpFragment");
    add(R.xml.ms_jx_op_click_setting, "Operation settings > Clicking settings", "MsJxOpClickSettingFragment");
    add(R.xml.ms_jx_op_default_gesture_setting, "Operation settings > Gestures default settings", "MsJxOpDefaultGestureSettingFragment");
    add(R.xml.ms_jx_op_fingerprint_reader_gestures, "Operation settings > Fingerprint reader gestures", "MsJxOpFingerprintReaderGesturesFragment");
    add(R.xml.ms_jx_op_hotkey_scheme_settings, "Operation settings > Hotkey scheme settings", "MsJxOpHotkeySchemeSettingsFragment");
    add(R.xml.ms_jx_op_key_shortcut_setting, "Operation settings > Shortcut keys settings", "MsJxOpKeyShortcutSettingFragment");
    add(R.xml.ms_jx_op_navigation_settings, "Operation settings > Navigation settings", "MsJxOpNavigationSettingsFragment");
    add(R.xml.ms_jx_op_other_settings, "Operation settings > Other settings", "MsJxOpOtherSettingsFragment");
    // END ms_jx index
  }

  private void add(int xmlRes, String group, String fragmentSimpleName) {
    String fragment = BASE + fragmentSimpleName;
    try (XmlResourceParser parser = getResources().getXml(xmlRes)) {
      AttributeSet attrs = Xml.asAttributeSet(parser);
      int[] wanted = {android.R.attr.title, android.R.attr.summary};
      int type;
      while ((type = parser.next()) != XmlResourceParser.END_DOCUMENT) {
        if (type != XmlResourceParser.START_TAG) {
          continue;
        }
        String tag = parser.getName();
        if ("PreferenceScreen".equals(tag) || "intent".equals(tag) || tag.endsWith("PreferenceCategory")) {
          continue;
        }
        TypedArray ta = obtainStyledAttributes(attrs, wanted);
        CharSequence title = ta.getText(0);
        CharSequence summary = ta.getText(1);
        ta.recycle();
        if (title == null || title.length() == 0) {
          continue;
        }
        all.add(new Entry(title.toString(), summary == null ? "" : summary.toString().replace("%s", ""), group, fragment));
      }
    } catch (Exception e) {
      // Skip a screen that cannot be parsed; search still works for the others.
    }
  }
}
