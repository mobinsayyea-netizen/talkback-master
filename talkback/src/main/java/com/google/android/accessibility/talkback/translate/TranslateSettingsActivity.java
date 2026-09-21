package com.google.android.accessibility.talkback.translate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Basic Translate settings: languages and language packs. */
public class TranslateSettingsActivity extends Activity {

  private final List<String> rows = new ArrayList<>();
  private ArrayAdapter<String> adapter;
  private SharedPreferences prefs;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTitle("Translate");
    prefs = TranslateEngine.prefs(this);

    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setBackgroundColor(Color.BLACK);
    int pad = (int) (16 * getResources().getDisplayMetrics().density);
    layout.setPadding(pad, pad, pad, pad);

    TextView heading = new TextView(this);
    heading.setText("Translate");
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
    rows.add("Translate to: " + TranslateEngine.languageName(TranslateEngine.target(this)) + ". Tap to change");
    String source = TranslateEngine.source(this);
    rows.add(
        "Translate from: "
            + (TranslateEngine.AUTO.equals(source) ? "Auto-detect" : TranslateEngine.languageName(source))
            + ". Tap to change");
    rows.add("Language packs. Tap to see or remove");
    rows.add("Translate works on this phone. Language packs download over mobile data or Wi-Fi.");
    adapter.notifyDataSetChanged();
  }

  private void onRow(int position) {
    if (position == 0) {
      pickLanguage("Translate to", false, TranslateEngine.KEY_TARGET);
    } else if (position == 1) {
      pickLanguage("Translate from", true, TranslateEngine.KEY_SOURCE);
    } else if (position == 2) {
      showPacks();
    }
  }

  private void pickLanguage(String title, boolean withAuto, final String key) {
    List<String> codes = new ArrayList<>(TranslateLanguage.getAllLanguages());
    Collections.sort(
        codes,
        (a, b) -> TranslateEngine.languageName(a).compareToIgnoreCase(TranslateEngine.languageName(b)));
    final List<String> values = new ArrayList<>();
    final List<String> labels = new ArrayList<>();
    if (withAuto) {
      values.add(TranslateEngine.AUTO);
      labels.add("Auto-detect");
    }
    for (String c : codes) {
      values.add(c);
      labels.add(TranslateEngine.languageName(c));
    }
    new AlertDialog.Builder(this)
        .setTitle(title)
        .setItems(
            labels.toArray(new String[0]),
            (dialog, which) -> {
              prefs.edit().putString(key, values.get(which)).apply();
              refresh();
            })
        .setNegativeButton("Cancel", null)
        .show();
  }

  private void showPacks() {
    RemoteModelManager.getInstance()
        .getDownloadedModels(TranslateRemoteModel.class)
        .addOnSuccessListener(
            models -> {
              final List<TranslateRemoteModel> list = new ArrayList<>(models);
              if (list.isEmpty()) {
                Toast.makeText(this, "No language packs downloaded yet", Toast.LENGTH_LONG).show();
                return;
              }
              String[] labels = new String[list.size()];
              for (int i = 0; i < list.size(); i++) {
                labels[i] = TranslateEngine.languageName(list.get(i).getLanguage()) + ". Tap to remove";
              }
              new AlertDialog.Builder(this)
                  .setTitle("Language packs")
                  .setItems(
                      labels,
                      (dialog, which) ->
                          RemoteModelManager.getInstance()
                              .deleteDownloadedModel(list.get(which))
                              .addOnSuccessListener(
                                  unused ->
                                      Toast.makeText(this, "Language pack removed", Toast.LENGTH_SHORT)
                                          .show()))
                  .setNegativeButton("Close", null)
                  .show();
            });
  }
}
