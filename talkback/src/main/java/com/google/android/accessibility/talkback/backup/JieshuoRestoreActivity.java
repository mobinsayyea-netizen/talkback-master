package com.google.android.accessibility.talkback.backup;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Advanced settings > Backup and restore > Restore from Jieshuo backup. The person picks the zip
 * of their Jieshuo folder; sounds and clipboard are brought into MS Screen Reader. The zip is only
 * read, never copied into the app.
 */
public class JieshuoRestoreActivity extends Activity {

  private static final int REQ_PICK = 201;

  private TextView status;
  private Button pick;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTitle("Restore from Jieshuo backup");
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(24, 24, 24, 24);

    TextView info = new TextView(this);
    info.setText(
        "Zip your Jieshuo folder and pick the zip here. This brings in your sound themes "
            + "(the same sound for the same event), clipboard history and favorites, labels and "
            + "aliases for the apps installed on this phone, and your extensions, tools and "
            + "resources. It also brings in your settings and gestures where MS Screen Reader "
            + "has the same setting; a setting you already changed in MS is replaced by the "
            + "Jieshuo value, and a full list of what was left out is saved as a text file. "
            + "Passwords, API keys and tokens are never read, and favorites that look like keys "
            + "are left out. Extensions, tools, labels and sound themes that already exist in MS "
            + "Screen Reader are kept as they are. Your file stays where it is.");
    root.addView(info);

    pick = new Button(this);
    pick.setText("Choose Jieshuo backup zip");
    pick.setAllCaps(false);
    pick.setOnClickListener(v -> choose());
    root.addView(
        pick,
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

    status = new TextView(this);
    status.setPadding(0, 24, 0, 0);
    status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    root.addView(status);

    ScrollView sv = new ScrollView(this);
    sv.addView(root);
    setContentView(sv);
  }

  private void choose() {
    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    i.addCategory(Intent.CATEGORY_OPENABLE);
    i.setType("*/*");
    startActivityForResult(i, REQ_PICK);
  }

  @Override
  protected void onActivityResult(int req, int result, Intent data) {
    super.onActivityResult(req, result, data);
    if (req != REQ_PICK || result != RESULT_OK || data == null || data.getData() == null) {
      return;
    }
    run(data.getData());
  }

  private void run(Uri uri) {
    pick.setEnabled(false);
    status.setText("Restoring. Please wait.");
    new Thread(
            () -> {
              String text;
              try {
                text = "Done.\n\n" + JieshuoRestore.restore(getApplicationContext(), uri).toText();
              } catch (Exception e) {
                text = "Could not restore: " + e.getMessage();
              }
              final String out = text;
              runOnUiThread(
                  () -> {
                    status.setText(out);
                    pick.setEnabled(true);
                  });
            })
        .start();
  }
}
