package com.google.android.accessibility.talkback.permission;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.accessibility.talkback.R;

/**
 * Welcome screen shown every time the app icon is opened: app name, developer, and one Close button.
 * Close continues into the app (permissions if something is missing, otherwise the settings).
 */
public class MsWelcomeActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTitle(R.string.ms_welcome_title);
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setGravity(Gravity.CENTER);
    int pad = (int) (24 * getResources().getDisplayMetrics().density);
    root.setPadding(pad, pad, pad, pad);

    TextView title = new TextView(this);
    title.setText(R.string.ms_welcome_title);
    title.setTextSize(30);
    title.setGravity(Gravity.CENTER);
    if (Build.VERSION.SDK_INT >= 28) {
      title.setAccessibilityHeading(true);
    }
    root.addView(title);

    TextView by = new TextView(this);
    by.setText(R.string.ms_welcome_by);
    by.setTextSize(20);
    by.setGravity(Gravity.CENTER);
    by.setPadding(0, pad / 2, 0, pad);
    root.addView(by);

    Button close = new Button(this);
    close.setText(R.string.ms_welcome_close);
    close.setTextSize(18);
    close.setAllCaps(false);
    close.setOnClickListener(
        (View v) -> {
          startActivity(new Intent(this, MsPermissionsActivity.class));
          finish();
        });
    root.addView(close);
    setContentView(root);
  }
}
