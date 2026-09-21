package com.google.android.accessibility.talkback.actor.gemini;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

/** One-time box that asks for the Gemini key. It never comes back on its own. */
public class GeminiKeyActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    GeminiKeyStore.setPromptDone(this);
    final EditText input = new EditText(this);
    input.setHint("Gemini API key");
    input.setSingleLine(true);
    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    LinearLayout box = new LinearLayout(this);
    int pad = (int) (20 * getResources().getDisplayMetrics().density);
    box.setPadding(pad, pad / 2, pad, 0);
    box.addView(
        input,
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    new AlertDialog.Builder(this)
        .setTitle("Gemini key")
        .setMessage(
            "To describe images or the screen, or to ask questions, enter your Gemini API key."
                + " You can also add it later in settings, Recognition, Gemini.")
        .setView(box)
        .setPositiveButton(
            "Save",
            (dialog, which) -> {
              String k = input.getText().toString().trim();
              if (!k.isEmpty()) {
                GeminiKeyStore.setKey(this, k);
                Toast.makeText(this, "Key saved", Toast.LENGTH_SHORT).show();
              }
              finish();
            })
        .setNegativeButton("Cancel", (dialog, which) -> finish())
        .setOnCancelListener(dialog -> finish())
        .show();
  }
}
