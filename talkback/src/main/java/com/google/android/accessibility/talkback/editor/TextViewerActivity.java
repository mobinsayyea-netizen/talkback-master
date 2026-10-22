/*
 * Copyright 2026 Advanced Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * One screen for three commands: "Full text editing" (read, select, copy), "edit current text"
 * (change the text and write it back to the field) and "List copying" (the texts of a list, one per
 * line, copy all or a selection). Not compiled yet.
 */
public class TextViewerActivity extends Activity {
  static final String MODE_VIEW = "view";
  static final String MODE_EDIT = "edit";
  static final String MODE_LIST = "list";
  private static final String EXTRA_MODE = "mode";
  private static final String EXTRA_TEXT = "text";
  private static final int MAX_NODES = 400;

  /** The field to write back to (edit mode). Kept only while this screen is open. */
  private static AccessibilityNodeInfo target;

  private EditText box;
  private String mode;

  static boolean show(Context ctx, String mode, AccessibilityNodeInfo node) {
    if (node == null) {
      return false;
    }
    String text;
    if (MODE_LIST.equals(mode)) {
      AccessibilityNodeInfo root = node;
      if (node.getChildCount() == 0 && node.getParent() != null) {
        root = node.getParent(); // a single list item: take its whole list
      }
      StringBuilder sb = new StringBuilder();
      collect(root, sb, new int[1]);
      text = sb.toString().trim();
    } else {
      CharSequence t = node.getText();
      if (t == null || t.length() == 0) {
        t = node.getContentDescription();
      }
      if (t == null || t.length() == 0) {
        StringBuilder sb = new StringBuilder();
        collect(node, sb, new int[1]);
        t = sb.toString().trim();
      }
      text = t.toString();
    }
    if (text.isEmpty()) {
      return false;
    }
    target = MODE_EDIT.equals(mode) && node.isEditable() ? node : null;
    try {
      Intent i = new Intent(ctx, TextViewerActivity.class);
      i.putExtra(EXTRA_MODE, mode);
      i.putExtra(EXTRA_TEXT, text);
      i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      ctx.startActivity(i);
      return true;
    } catch (RuntimeException e) {
      return false;
    }
  }

  /** Visible texts of the node and everything under it, one per line. */
  private static void collect(AccessibilityNodeInfo n, StringBuilder out, int[] count) {
    if (n == null || count[0]++ > MAX_NODES || !n.isVisibleToUser()) {
      return;
    }
    CharSequence t = n.getText();
    if (t == null || t.length() == 0) {
      t = n.getContentDescription();
    }
    if (t != null && t.length() > 0 && n.getChildCount() == 0) {
      out.append(t).append('\n');
    }
    for (int i = 0; i < n.getChildCount(); i++) {
      collect(n.getChild(i), out, count);
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mode = getIntent().getStringExtra(EXTRA_MODE);
    setTitle(
        MODE_EDIT.equals(mode)
            ? "Edit current text"
            : MODE_LIST.equals(mode) ? "List copying" : "Full text");
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    box = new EditText(this);
    box.setText(getIntent().getStringExtra(EXTRA_TEXT));
    box.setGravity(android.view.Gravity.TOP);
    if (!MODE_EDIT.equals(mode)) {
      box.setTextIsSelectable(true); // read only, but selecting and copying work
      box.setKeyListener(null);
    }
    root.addView(box, new LinearLayout.LayoutParams(-1, 0, 1f));
    addButton(root, "Copy selection", v -> copy(true));
    addButton(root, "Copy all", v -> copy(false));
    if (MODE_EDIT.equals(mode) && target != null) {
      addButton(root, "Save to the field", v -> saveBack());
    }
    addButton(root, "Close", v -> finish());
    setContentView(root);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    target = null;
  }

  private void addButton(LinearLayout root, String label, android.view.View.OnClickListener l) {
    Button b = new Button(this);
    b.setText(label);
    b.setOnClickListener(l);
    root.addView(b, new LinearLayout.LayoutParams(-1, -2));
  }

  private void copy(boolean selectionOnly) {
    String all = box.getText().toString();
    int a = Math.min(box.getSelectionStart(), box.getSelectionEnd());
    int b = Math.max(box.getSelectionStart(), box.getSelectionEnd());
    String text = selectionOnly && a >= 0 && b > a ? all.substring(a, b) : all;
    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    if (cm != null) {
      cm.setPrimaryClip(ClipData.newPlainText("text", text));
      Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
    }
  }

  private void saveBack() {
    AccessibilityNodeInfo n = target;
    if (n == null || !n.refresh()) {
      Toast.makeText(this, "The field is no longer there", Toast.LENGTH_LONG).show();
      return;
    }
    Bundle args = new Bundle();
    args.putCharSequence(
        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, box.getText().toString());
    if (n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
      Toast.makeText(this, "Saved to the field", Toast.LENGTH_SHORT).show();
      finish();
    } else {
      Toast.makeText(this, "Could not change the field", Toast.LENGTH_LONG).show();
    }
  }
}
