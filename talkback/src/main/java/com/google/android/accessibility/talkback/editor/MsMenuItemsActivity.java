/*
 * Copyright 2026 Advanced Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Customize the main menu, the multi-column menu or the floating menu: Add, Default, and for every
 * entry Move up, Move down, Delete (like Jieshuo). Not compiled yet.
 */
public class MsMenuItemsActivity extends Activity {
  private String key;
  private List<MsMenus.Item> items = new ArrayList<>();
  private ArrayAdapter<String> adapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    key = getIntent().getStringExtra("key");
    String title = getIntent().getStringExtra("title");
    if (key == null) {
      finish();
      return;
    }
    setTitle(title);
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    TextView head = new TextView(this);
    head.setText(title);
    head.setTextSize(20);
    head.setPadding(24, 24, 24, 12);
    head.setAccessibilityHeading(true);
    root.addView(head);
    Button add = new Button(this);
    add.setText("Add");
    add.setOnClickListener(v -> askCategory());
    root.addView(add);
    Button def = new Button(this);
    def.setText("Default");
    def.setOnClickListener(
        v -> {
          MsMenus.resetToDefault(this, key);
          reload();
        });
    root.addView(def);
    ListView list = new ListView(this);
    adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
    list.setAdapter(adapter);
    list.setOnItemClickListener((p, v, pos, id) -> askEntry(pos));
    root.addView(
        list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    setContentView(root);
    reload();
  }

  private void reload() {
    items = new ArrayList<>(MsMenus.load(this, key));
    adapter.clear();
    for (int i = 0; i < items.size(); i++) {
      adapter.add((i + 1) + " " + items.get(i).label);
    }
    adapter.notifyDataSetChanged();
  }

  private void store() {
    MsMenus.save(this, key, items);
    reload();
  }

  private void askEntry(final int pos) {
    new AlertDialog.Builder(this)
        .setTitle(items.get(pos).label)
        .setItems(
            new String[] {"Move up", "Move down", "Delete"},
            (d, which) -> {
              if (which == 0 && pos > 0) {
                Collections.swap(items, pos, pos - 1);
              } else if (which == 1 && pos < items.size() - 1) {
                Collections.swap(items, pos, pos + 1);
              } else if (which == 2) {
                items.remove(pos);
              }
              store();
            })
        .setNegativeButton("Cancel", null)
        .show();
  }

  private void askCategory() {
    new AlertDialog.Builder(this)
        .setTitle("Select a function")
        .setItems(
            new String[] {"Functions", "Extensions and tools", "Applications"},
            (d, which) -> {
              if (which == 0) {
                pickCommand();
              } else if (which == 1) {
                pickExtension();
              } else {
                pickApp();
              }
            })
        .setNegativeButton("Cancel", null)
        .show();
  }

  private void add(MsMenus.Item i) {
    items.add(i);
    store();
  }

  private void pickCommand() {
    final List<MsMenus.Item> all = new ArrayList<>();
    all.add(new MsMenus.Item("Open keyboard", "kbd", "kbd"));
    for (String[] row : JxCommands.TABLE) {
      if (row.length > 1 && row[1] != null && !row[1].isEmpty()) {
        all.add(new MsMenus.Item(row[1], "cmd", row[1]));
      }
    }
    String[] names = new String[all.size()];
    for (int i = 0; i < names.length; i++) {
      names[i] = all.get(i).label;
    }
    new AlertDialog.Builder(this)
        .setTitle("Functions")
        .setItems(names, (d, which) -> add(all.get(which)))
        .setNegativeButton("Cancel", null)
        .show();
  }

  private void pickExtension() {
    final List<CustomVoiceCommands.Entry> targets = CustomVoiceCommands.targets(this);
    if (targets.isEmpty()) {
      new AlertDialog.Builder(this).setMessage("No extensions or tools yet").setPositiveButton("OK", null).show();
      return;
    }
    String[] names = new String[targets.size()];
    for (int i = 0; i < names.length; i++) {
      names[i] =
          ("tool".equals(targets.get(i).kind) ? "Tool: " : "Extension: ") + targets.get(i).name;
    }
    new AlertDialog.Builder(this)
        .setTitle("Extensions and tools")
        .setItems(
            names,
            (d, which) -> {
              CustomVoiceCommands.Entry t = targets.get(which);
              add(new MsMenus.Item(t.name, t.kind, t.name));
            })
        .setNegativeButton("Cancel", null)
        .show();
  }

  private void pickApp() {
    PackageManager pm = getPackageManager();
    Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
    List<ResolveInfo> found = pm.queryIntentActivities(main, 0);
    final List<MsMenus.Item> apps = new ArrayList<>();
    for (ResolveInfo r : found) {
      apps.add(new MsMenus.Item(r.loadLabel(pm).toString(), "app", r.activityInfo.packageName));
    }
    Collections.sort(apps, (a, b) -> a.label.compareToIgnoreCase(b.label));
    String[] names = new String[apps.size()];
    for (int i = 0; i < names.length; i++) {
      names[i] = apps.get(i).label;
    }
    new AlertDialog.Builder(this)
        .setTitle("Applications")
        .setItems(names, (d, which) -> add(apps.get(which)))
        .setNegativeButton("Cancel", null)
        .show();
  }
}
