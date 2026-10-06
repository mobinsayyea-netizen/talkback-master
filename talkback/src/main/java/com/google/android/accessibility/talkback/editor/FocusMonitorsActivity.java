/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.List;

/** "Focus monitor manager": the list of watched elements; tap one to stop watching it. */
public class FocusMonitorsActivity extends Activity {
  private final List<MsFocusMonitor.Monitor> items = new ArrayList<>();
  private final List<String> titles = new ArrayList<>();
  private ArrayAdapter<String> adapter;

  static boolean show(Context context) {
    try {
      Intent i = new Intent(context, FocusMonitorsActivity.class);
      i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(i);
      return true;
    } catch (RuntimeException e) {
      return false;
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTitle("Focus monitor manager");
    ListView list = new ListView(this);
    setContentView(list);
    adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, titles);
    list.setAdapter(adapter);
    list.setOnItemClickListener(
        (parent, view, position, id) -> {
          if (position < items.size()) {
            askRemove(items.get(position));
          }
        });
    reload();
  }

  private void reload() {
    items.clear();
    items.addAll(MsFocusMonitor.snapshot(this));
    titles.clear();
    for (MsFocusMonitor.Monitor m : items) {
      titles.add(m.label + ", " + m.pkg);
    }
    if (titles.isEmpty()) {
      titles.add("Nothing is being watched");
    }
    adapter.notifyDataSetChanged();
  }

  private void askRemove(final MsFocusMonitor.Monitor m) {
    new AlertDialog.Builder(this)
        .setTitle(m.label)
        .setMessage("Stop watching this element?")
        .setPositiveButton(
            "Stop watching",
            (d, w) -> {
              MsFocusMonitor.remove(this, m);
              reload();
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }
}
