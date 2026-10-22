package com.google.android.accessibility.talkback.editor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** "AI Create": three big choices. Everything is plain text and buttons for the screen reader. */
public class AiCreateActivity extends Activity {

  public static void show(Context c) {
    Intent i = new Intent(c, AiCreateActivity.class);
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    c.startActivity(i);
  }

  @Override
  protected void onCreate(Bundle b) {
    super.onCreate(b);
    setTitle("AI Create");
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    int pad = (int) (16 * getResources().getDisplayMetrics().density);
    root.setPadding(pad, pad, pad, pad);

    TextView head = new TextView(this);
    head.setText("AI Create");
    head.setTextSize(22);
    head.setFocusable(true);
    root.addView(head);

    root.addView(button("Create Extension", new Runnable() {
      @Override
      public void run() {
        AiChatActivity.show(AiCreateActivity.this, "extension");
      }
    }));
    root.addView(button("Create Tool", new Runnable() {
      @Override
      public void run() {
        AiChatActivity.show(AiCreateActivity.this, "tool");
      }
    }));
    root.addView(button("Create Android APK", new Runnable() {
      @Override
      public void run() {
        startActivity(new Intent(AiCreateActivity.this, ApkBuilderActivity.class));
      }
    }));

    ScrollView sv = new ScrollView(this);
    sv.addView(root);
    setContentView(sv);
  }

  private Button button(String text, final Runnable action) {
    Button bt = new Button(this);
    bt.setText(text);
    bt.setTextSize(18);
    LinearLayout.LayoutParams lp =
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    lp.topMargin = (int) (14 * getResources().getDisplayMetrics().density);
    bt.setLayoutParams(lp);
    bt.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            action.run();
          }
        });
    return bt;
  }
}
