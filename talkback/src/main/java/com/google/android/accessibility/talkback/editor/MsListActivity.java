/*
 * Copyright 2026 Advanced Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.List;

/**
 * One generic list screen used by many commands (notification box, chat history, show all nodes,
 * virtual screen, virtual navigation, list browsing, screen grid, granular browsing, profiles).
 * The caller builds the items; tapping an item runs its action. Not compiled yet.
 */
public class MsListActivity extends Activity {
  /** One row. */
  static final class Item {
    final String label;
    final Runnable action; // may be null
    final boolean closeOnTap;

    Item(String label, Runnable action, boolean closeOnTap) {
      this.label = label;
      this.action = action;
      this.closeOnTap = closeOnTap;
    }
  }

  private static List<Item> items = new ArrayList<>();
  private static String screenTitle = "";

  static boolean show(Context ctx, String title, List<Item> list) {
    if (list == null || list.isEmpty()) {
      return false;
    }
    items = list;
    screenTitle = title;
    try {
      Intent i = new Intent(ctx, MsListActivity.class);
      i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      ctx.startActivity(i);
      return true;
    } catch (RuntimeException e) {
      return false;
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTitle(screenTitle);
    final List<Item> mine = items;
    ListView list = new ListView(this);
    setContentView(list);
    List<String> labels = new ArrayList<>();
    for (Item it : mine) {
      labels.add(it.label);
    }
    list.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels));
    list.setOnItemClickListener(
        (parent, view, position, id) -> {
          Item it = mine.get(position);
          if (it.closeOnTap) {
            finish();
          }
          if (it.action != null) {
            // give the previous window a moment to come back before acting on it
            android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
            h.postDelayed(it.action, it.closeOnTap ? 350 : 0);
          }
        });
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (isFinishing()) {
      items = new ArrayList<>(); // do not keep node references
    }
  }
}
