package com.google.android.accessibility.talkback.soundtheme;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/**
 * Lists the Default sound theme and any themes the user has added. Each row can be customized
 * (change individual sounds); added themes can also be deleted. "Add Sound Theme" copies every
 * file from a chosen folder into a new theme.
 */
public class SoundThemeListActivity extends Activity {

  private static final int REQUEST_PICK_FOLDER = 9101;

  private SoundThemeManager manager;
  private ListView list;
  private final List<SoundThemeManager.Theme> shown = new ArrayList<>();
  private ArrayAdapter<SoundThemeManager.Theme> adapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTitle("Sound Theme");
    manager = new SoundThemeManager(this);

    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setBackgroundColor(Color.BLACK);
    int pad = (int) (16 * getResources().getDisplayMetrics().density);
    layout.setPadding(pad, pad, pad, pad);

    TextView heading = new TextView(this);
    heading.setText("Sound Theme");
    heading.setTextColor(Color.WHITE);
    heading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f);
    layout.addView(
        heading,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    list = new ListView(this);
    layout.addView(
        list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

    Button addButton = new Button(this);
    addButton.setText("Add Sound Theme");
    addButton.setOnClickListener(v -> launchFolderPicker());
    layout.addView(
        addButton,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    setContentView(layout);

    adapter =
        new ArrayAdapter<SoundThemeManager.Theme>(this, 0, shown) {
          @Override
          public View getView(int position, View convertView, ViewGroup parent) {
            SoundThemeManager.Theme theme = getItem(position);
            LinearLayout row = new LinearLayout(SoundThemeListActivity.this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            int rowPad = (int) (8 * getResources().getDisplayMetrics().density);
            row.setPadding(0, rowPad, 0, rowPad);

            String activeId = manager.getActiveThemeId();
            boolean active = theme.id.equals(activeId);

            TextView label = new TextView(SoundThemeListActivity.this);
            label.setTextColor(Color.WHITE);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
            label.setText(active ? theme.name + ". Active" : theme.name);
            row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            if (!active) {
              Button setActive = smallButton("Set active");
              setActive.setOnClickListener(
                  v -> {
                    manager.setActiveThemeId(theme.id);
                    refresh();
                    list.announceForAccessibility(theme.name + " set active");
                  });
              row.addView(setActive);
            }

            Button customize = smallButton("Customize");
            customize.setOnClickListener(
                v -> {
                  Intent intent =
                      new Intent(SoundThemeListActivity.this, SoundThemeCustomizeActivity.class);
                  intent.putExtra(SoundThemeCustomizeActivity.EXTRA_THEME_ID, theme.id);
                  intent.putExtra(SoundThemeCustomizeActivity.EXTRA_THEME_NAME, theme.name);
                  startActivity(intent);
                });
            row.addView(customize);

            if (!theme.builtin) {
              Button delete = smallButton("Delete");
              delete.setOnClickListener(v -> confirmDelete(theme));
              row.addView(delete);
            }
            return row;
          }
        };
    list.setAdapter(adapter);
  }

  @Override
  protected void onResume() {
    super.onResume();
    refresh();
  }

  private void launchFolderPicker() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
    try {
      startActivityForResult(intent, REQUEST_PICK_FOLDER);
    } catch (android.content.ActivityNotFoundException e) {
      list.announceForAccessibility("No folder picker is available on this device");
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQUEST_PICK_FOLDER && resultCode == RESULT_OK && data != null) {
      Uri folderUri = data.getData();
      if (folderUri != null) {
        try {
          getContentResolver()
              .takePersistableUriPermission(
                  folderUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException e) {
          // Some providers don't support persisting; a one-time read still works below.
        }
        promptThemeName(folderUri);
      }
    }
  }

  private Button smallButton(String label) {
    Button b = new Button(this);
    b.setText(label);
    b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
    return b;
  }

  private void refresh() {
    shown.clear();
    shown.addAll(manager.getThemes());
    adapter.notifyDataSetChanged();
  }

  private void promptThemeName(Uri folderUri) {
    final EditText input = new EditText(this);
    input.setHint("Theme name");
    input.setTextColor(Color.WHITE);
    new AlertDialog.Builder(this)
        .setTitle("New sound theme")
        .setView(input)
        .setPositiveButton(
            "Add",
            (dialog, which) -> {
              String name = input.getText().toString().trim();
              if (name.isEmpty()) {
                name = "My Sound Theme";
              }
              String themeId = manager.addThemeFromFolder(folderUri, name);
              if (themeId == null) {
                list.announceForAccessibility("No sound files found in that folder");
              } else {
                refresh();
                list.announceForAccessibility(name + " added");
              }
            })
        .setNegativeButton("Cancel", null)
        .show();
  }

  private void confirmDelete(SoundThemeManager.Theme theme) {
    new AlertDialog.Builder(this)
        .setTitle("Delete " + theme.name)
        .setMessage("Delete this sound theme and its sounds?")
        .setPositiveButton(
            "Delete",
            (dialog, which) -> {
              manager.deleteTheme(theme.id);
              refresh();
              list.announceForAccessibility(theme.name + " deleted");
            })
        .setNegativeButton("Cancel", null)
        .show();
  }
}
