package com.google.android.accessibility.talkback.actor.gemini;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

/** Gemini settings: the key and the model name. */
public class GeminiSettingsActivity extends Activity {

  private final List<String> rows = new ArrayList<>();
  private ArrayAdapter<String> adapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTitle("Gemini");
    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setBackgroundColor(Color.BLACK);
    int pad = (int) (16 * getResources().getDisplayMetrics().density);
    layout.setPadding(pad, pad, pad, pad);

    TextView heading = new TextView(this);
    heading.setText("Gemini");
    heading.setTextColor(Color.WHITE);
    heading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f);
    if (Build.VERSION.SDK_INT >= 28) {
      heading.setAccessibilityHeading(true);
    }
    layout.addView(
        heading,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows);
    ListView list = new ListView(this);
    list.setAdapter(adapter);
    list.setOnItemClickListener((parent, view, position, id) -> onRow(position));
    layout.addView(
        list,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    setContentView(layout);
  }

  @Override
  protected void onResume() {
    super.onResume();
    refresh();
  }

  private void refresh() {
    rows.clear();
    boolean hasKey = !GeminiKeyStore.key(this).isEmpty();
    rows.add("API key: " + (hasKey ? "Set" : "Not set") + ". Tap to change");
    rows.add("Model: " + GeminiKeyStore.model(this) + ". Tap to change");
    rows.add("Gemini is used only to describe images, describe the screen and answer your questions.");
    adapter.notifyDataSetChanged();
  }

  private void onRow(int position) {
    if (position == 0) {
      edit("Gemini API key", GeminiKeyStore.key(this), true);
    } else if (position == 1) {
      edit("Gemini model", GeminiKeyStore.model(this), false);
    }
  }

  private void edit(String title, String current, final boolean isKey) {
    final EditText input = new EditText(this);
    input.setSingleLine(true);
    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    input.setText(current);
    input.setSelectAllOnFocus(true);
    LinearLayout box = new LinearLayout(this);
    int pad = (int) (20 * getResources().getDisplayMetrics().density);
    box.setPadding(pad, pad / 2, pad, 0);
    box.addView(
        input,
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    new AlertDialog.Builder(this)
        .setTitle(title)
        .setView(box)
        .setPositiveButton(
            "Save",
            (dialog, which) -> {
              String value = input.getText().toString().trim();
              if (isKey) {
                GeminiKeyStore.setKey(this, value);
              } else {
                GeminiKeyStore.setModel(this, value);
              }
              Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
              refresh();
            })
        .setNegativeButton("Cancel", null)
        .show();
  }
}
