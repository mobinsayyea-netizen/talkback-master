package com.google.android.accessibility.talkback;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Advanced settings > Backup and restore > Error log: read, copy, save to Downloads, share, clear. */
public class ErrorLogActivity extends Activity {

  private TextView body;
  private TextView status;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MsErrorLog.init(this);
    setTitle("Error log");
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(24, 24, 24, 24);

    status = new TextView(this);
    status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    root.addView(status);

    addButton(root, "Copy to clipboard", () -> {
      ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
      if (cm != null) {
        cm.setPrimaryClip(ClipData.newPlainText("Advanced Screen Reader report", MsErrorLog.readAll(this)));
        status.setText("Copied. Paste it in the chat.");
      }
    });
    addButton(root, "Save to Downloads", () -> status.setText(MsErrorLog.saveToDownloads(this)));
    addButton(root, "Share", () -> {
      String all = MsErrorLog.readAll(this);
      Intent i = new Intent(Intent.ACTION_SEND);
      i.setType("text/plain");
      i.putExtra(Intent.EXTRA_TEXT, all.length() > 100000 ? all.substring(all.length() - 100000) : all);
      startActivity(Intent.createChooser(i, "Share error log"));
    });
    addButton(root, "Clear log", () -> {
      MsErrorLog.clear(this);
      show();
      status.setText("Cleared.");
    });

    body = new TextView(this);
    body.setPadding(0, 24, 0, 0);
    body.setTextIsSelectable(true);
    root.addView(body);

    ScrollView sv = new ScrollView(this);
    sv.addView(root);
    setContentView(sv);
    show();
  }

  private void addButton(LinearLayout root, String label, Runnable r) {
    Button b = new Button(this);
    b.setText(label);
    b.setAllCaps(false);
    b.setOnClickListener(v -> {
      try {
        r.run();
      } catch (Throwable t) {
        status.setText("Failed: " + t);
      }
    });
    root.addView(
        b,
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
  }

  private void show() {
    String all = MsErrorLog.readAll(this);
    body.setText(all.length() > 20000 ? "...(older part cut, Copy has all)\n" + all.substring(all.length() - 20000) : all);
  }
}
