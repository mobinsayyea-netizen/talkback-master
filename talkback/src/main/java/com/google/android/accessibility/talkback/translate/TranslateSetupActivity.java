package com.google.android.accessibility.talkback.translate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Asks for permission to download the language packs the first time Translate is used. */
public class TranslateSetupActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final String[] langs = getIntent().getStringArrayExtra("langs");
    final List<String> languages =
        langs == null ? new ArrayList<String>() : new ArrayList<>(Arrays.asList(langs));
    StringBuilder names = new StringBuilder();
    for (String l : languages) {
      if (names.length() > 0) {
        names.append(", ");
      }
      names.append(TranslateEngine.languageName(l));
    }
    final Context app = getApplicationContext();
    new AlertDialog.Builder(this)
        .setTitle("Translate")
        .setMessage(
            "Translate needs language packs: "
                + names
                + " (about "
                + (30 * Math.max(1, languages.size()))
                + " MB). Download now using mobile data or Wi-Fi?")
        .setPositiveButton(
            "Download",
            (dialog, which) -> {
              TranslateEngine.prefs(app).edit().putBoolean(TranslateEngine.KEY_DOWNLOAD_OK, true).apply();
              Toast.makeText(app, "Downloading language pack", Toast.LENGTH_LONG).show();
              TranslateEngine.downloadModels(
                  languages,
                  TranslateEngine::runPending,
                  () ->
                      Toast.makeText(
                              app, "Download failed. Check your internet connection.", Toast.LENGTH_LONG)
                          .show());
              finish();
            })
        .setNegativeButton(
            "Cancel",
            (dialog, which) -> {
              TranslateEngine.clearPending();
              finish();
            })
        .setOnCancelListener(
            dialog -> {
              TranslateEngine.clearPending();
              finish();
            })
        .show();
  }
}
