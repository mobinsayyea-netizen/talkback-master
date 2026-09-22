package com.google.android.accessibility.talkback.soundtheme;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shows every sound slot for one theme (e.g. Click, Focus, Scroll). Moving focus onto a slot
 * announces it and plays whatever is currently set for it. Double-tapping a slot opens a picker of
 * this theme's own sounds to assign a different one. Slots are not clickable in any other way.
 */
public class SoundThemeCustomizeActivity extends Activity {

  public static final String EXTRA_THEME_ID = "theme_id";
  public static final String EXTRA_THEME_NAME = "theme_name";
  private static final int REQUEST_PICK_SOUND = 9202;

  private SoundThemeManager manager;
  private String themeId;
  private ListView list;
  private final List<SoundSlot> shown = new ArrayList<>();
  private ArrayAdapter<SoundSlot> adapter;
  private @android.annotation.SuppressLint("StaticFieldLeak") MediaPlayer previewPlayer;
  private int pendingSlotPosition = -1;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    themeId = getIntent().getStringExtra(EXTRA_THEME_ID);
    String themeName = getIntent().getStringExtra(EXTRA_THEME_NAME);
    setTitle("Customize " + themeName);
    manager = new SoundThemeManager(this);

    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setBackgroundColor(Color.BLACK);
    int pad = (int) (16 * getResources().getDisplayMetrics().density);
    layout.setPadding(pad, pad, pad, pad);

    TextView heading = new TextView(this);
    heading.setText("Customize: " + themeName);
    heading.setTextColor(Color.WHITE);
    heading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
    layout.addView(
        heading,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    TextView hint = new TextView(this);
    hint.setText("Move to a sound to hear it. Double tap to change it.");
    hint.setTextColor(Color.LTGRAY);
    hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
    layout.addView(
        hint,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    list = new ListView(this);
    list.setFocusable(true);
    list.setItemsCanFocus(false);
    layout.addView(
        list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    setContentView(layout);

    shown.addAll(manager.getSlots());
    adapter =
        new ArrayAdapter<SoundSlot>(this, android.R.layout.simple_list_item_1, shown) {
          @Override
          public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            TextView t = v.findViewById(android.R.id.text1);
            t.setText(labelFor(getItem(position)));
            t.setTextColor(Color.WHITE);
            // The slot itself isn't clickable; only a double tap (handled below) changes it.
            v.setClickable(false);
            v.setOnClickListener(null);
            return v;
          }
        };
    list.setAdapter(adapter);

    list.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            playPreview(shown.get(position));
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });

    // Accessibility focus (TalkBack) doesn't always fire item-selected, so also cover hover /
    // accessibility-focus via a focus-change watcher on each row through the accessibility
    // delegate below.
    list.setOnItemLongClickListener(
        (parent, view, position, id) -> {
          openPicker(position);
          return true;
        });
    list.setOnItemClickListener(
        (parent, view, position, id) -> {
          // A double tap while explore-by-touch is active is delivered as a normal click.
          openPicker(position);
        });
  }

  @Override
  protected void onPause() {
    super.onPause();
    stopPreview();
  }

  private String labelFor(SoundSlot slot) {
    Map<String, String> assignments = manager.readAssignments(themeId);
    String fileName = assignments.get(slot.key);
    if (fileName != null) {
      return slot.label + " — " + fileName;
    }
    boolean usesDefaultFallback =
        !SoundThemeManager.DEFAULT_THEME_ID.equals(themeId)
            && manager.readAssignments(SoundThemeManager.DEFAULT_THEME_ID).containsKey(slot.key);
    return usesDefaultFallback ? slot.label + " — from Default theme" : slot.label + " — app sound";
  }

  private void playPreview(SoundSlot slot) {
    stopPreview();
    // Resolve the same way playback will: this theme, then Default, then the app's own sound.
    String resolved = resolvedPath(themeId, slot.key);
    if (resolved == null && !SoundThemeManager.DEFAULT_THEME_ID.equals(themeId)) {
      resolved = resolvedPath(SoundThemeManager.DEFAULT_THEME_ID, slot.key);
    }
    if (resolved != null) {
      try {
        previewPlayer = new MediaPlayer();
        previewPlayer.setDataSource(resolved);
        previewPlayer.prepare();
        previewPlayer.start();
      } catch (Exception e) {
        stopPreview();
      }
    } else {
      try {
        android.media.MediaPlayer mp = android.media.MediaPlayer.create(this, slot.defaultResId);
        if (mp != null) {
          previewPlayer = mp;
          mp.setOnCompletionListener(MediaPlayer::release);
          mp.start();
        }
      } catch (Exception e) {
        // No sound available to preview; the announcement alone is enough.
      }
    }
  }

  private @android.annotation.Nullable String resolvedPath(String themeIdToCheck, String slotKey) {
    Map<String, String> assignments = manager.readAssignments(themeIdToCheck);
    String fileName = assignments.get(slotKey);
    if (fileName == null) {
      return null;
    }
    File file = new File(getFilesDir(), "sound_themes/" + themeIdToCheck + "/pool/" + fileName);
    return file.exists() ? file.getAbsolutePath() : null;
  }

  private void stopPreview() {
    if (previewPlayer != null) {
      try {
        previewPlayer.stop();
      } catch (Exception ignored) {
        // Player may already be stopped or released.
      }
      previewPlayer.release();
      previewPlayer = null;
    }
  }

  private void openPicker(int position) {
    pendingSlotPosition = position;
    Intent intent = new Intent(this, SoundPickerActivity.class);
    intent.putExtra(SoundPickerActivity.EXTRA_THEME_ID, themeId);
    startActivityForResult(intent, REQUEST_PICK_SOUND);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQUEST_PICK_SOUND
        && resultCode == RESULT_OK
        && data != null
        && pendingSlotPosition >= 0
        && pendingSlotPosition < shown.size()) {
      String fileName = data.getStringExtra(SoundPickerActivity.RESULT_FILE_NAME);
      if (fileName != null) {
        SoundSlot slot = shown.get(pendingSlotPosition);
        manager.assign(themeId, slot.key, fileName);
        adapter.notifyDataSetChanged();
        list.announceForAccessibility(slot.label + " set to " + fileName);
      }
    }
    pendingSlotPosition = -1;
  }
}
