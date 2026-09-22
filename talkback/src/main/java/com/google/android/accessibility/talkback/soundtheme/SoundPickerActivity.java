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

/**
 * Lists every sound file available inside one theme's own pool. Moving focus to an item announces
 * its name and plays it; tapping an item chooses it and returns to the Customize screen.
 */
public class SoundPickerActivity extends Activity {

  public static final String EXTRA_THEME_ID = "theme_id";
  public static final String RESULT_FILE_NAME = "file_name";

  private final List<String> fileNames = new ArrayList<>();
  private String themeId;
  private @android.annotation.SuppressLint("StaticFieldLeak") MediaPlayer previewPlayer;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTitle("Choose a sound");
    themeId = getIntent().getStringExtra(EXTRA_THEME_ID);
    SoundThemeManager manager = new SoundThemeManager(this);
    fileNames.addAll(manager.getPoolFileNames(themeId));

    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setBackgroundColor(Color.BLACK);
    int pad = (int) (16 * getResources().getDisplayMetrics().density);
    layout.setPadding(pad, pad, pad, pad);

    TextView heading = new TextView(this);
    heading.setText("Choose a sound");
    heading.setTextColor(Color.WHITE);
    heading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
    layout.addView(
        heading,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    ListView list = new ListView(this);
    layout.addView(
        list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    setContentView(layout);

    if (fileNames.isEmpty()) {
      TextView empty = new TextView(this);
      empty.setText("This theme has no sounds yet. Add a sound theme with some files first.");
      empty.setTextColor(Color.WHITE);
      layout.addView(
          empty,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    ArrayAdapter<String> adapter =
        new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, fileNames) {
          @Override
          public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            TextView t = v.findViewById(android.R.id.text1);
            t.setTextColor(Color.WHITE);
            return v;
          }
        };
    list.setAdapter(adapter);

    list.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            playPreview(fileNames.get(position));
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });

    list.setOnItemClickListener(
        (parent, view, position, id) -> {
          Intent result = new Intent();
          result.putExtra(RESULT_FILE_NAME, fileNames.get(position));
          setResult(RESULT_OK, result);
          finish();
        });
  }

  @Override
  protected void onPause() {
    super.onPause();
    stopPreview();
  }

  private void playPreview(String fileName) {
    stopPreview();
    File file = new File(getFilesDir(), "sound_themes/" + themeId + "/pool/" + fileName);
    if (!file.exists()) {
      return;
    }
    try {
      previewPlayer = new MediaPlayer();
      previewPlayer.setDataSource(file.getAbsolutePath());
      previewPlayer.prepare();
      previewPlayer.start();
    } catch (Exception e) {
      stopPreview();
    }
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
}
