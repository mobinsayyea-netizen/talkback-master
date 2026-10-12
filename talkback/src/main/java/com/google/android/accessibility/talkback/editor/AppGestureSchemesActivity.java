/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manager of the per-application gesture schemes (Jieshuo: "gesture scheme manager"). First item
 * creates a scheme for an app you choose; below it every existing scheme. Tap = edit its gestures,
 * long press = turn on or off, or delete. A scheme that is off keeps its gestures but is not used.
 * Not compiled yet.
 */
public class AppGestureSchemesActivity extends Activity {
  private final List<String> labels = new ArrayList<>();
  private final List<String> pkgs = new ArrayList<>(); // pkgs.get(i) belongs to labels.get(i + 1)
  private ArrayAdapter<String> adapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTitle("Per-application gesture schemes");
    ListView list = new ListView(this);
    setContentView(list);
    adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels);
    list.setAdapter(adapter);
    list.setOnItemClickListener(
        (parent, view, position, id) -> {
          if (position == 0) {
            chooseApp();
          } else {
            ExtensionGesturesActivity.showForApp(this, pkgs.get(position - 1));
          }
        });
    list.setOnItemLongClickListener(
        (parent, view, position, id) -> {
          if (position == 0) {
            return false;
          }
          options(pkgs.get(position - 1));
          return true;
        });
  }

  @Override
  protected void onResume() {
    super.onResume();
    refresh();
  }

  private void refresh() {
    labels.clear();
    pkgs.clear();
    labels.add("Create a new per-application gesture scheme");
    for (String p : AppGestureSchemes.packages(this)) {
      AppGestureSchemeData.Scheme s = AppGestureSchemes.scheme(this, p);
      if (s == null) {
        continue;
      }
      pkgs.add(p);
      labels.add(
          AppGestureSchemes.label(this, p)
              + ", "
              + (s.enabled ? "on" : "off")
              + ", "
              + s.gestures.size()
              + " gestures");
    }
    adapter.notifyDataSetChanged();
  }

  private void chooseApp() {
    final PackageManager pm = getPackageManager();
    Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
    List<ResolveInfo> found = pm.queryIntentActivities(main, 0);
    final List<String> names = new ArrayList<>();
    final List<String> ids = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    List<String[]> rows = new ArrayList<>();
    for (ResolveInfo r : found) {
      String p = r.activityInfo.packageName;
      if (seen.add(p)) {
        rows.add(new String[] {r.loadLabel(pm).toString(), p});
      }
    }
    Collections.sort(rows, (a, b) -> a[0].compareToIgnoreCase(b[0]));
    Set<String> have = new HashSet<>(AppGestureSchemes.packages(this));
    for (String[] row : rows) {
      names.add(row[0] + (have.contains(row[1]) ? " (has a scheme)" : ""));
      ids.add(row[1]);
    }
    new AlertDialog.Builder(this)
        .setTitle("Choose an application")
        .setItems(
            names.toArray(new String[0]),
            (d, which) -> {
              String p = ids.get(which);
              AppGestureSchemes.create(this, p);
              ExtensionGesturesActivity.showForApp(this, p);
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void options(final String p) {
    AppGestureSchemeData.Scheme s = AppGestureSchemes.scheme(this, p);
    if (s == null) {
      return;
    }
    final boolean on = s.enabled;
    new AlertDialog.Builder(this)
        .setTitle(AppGestureSchemes.label(this, p))
        .setItems(
            new String[] {on ? "Turn off" : "Turn on", "Delete this scheme"},
            (d, which) -> {
              if (which == 0) {
                AppGestureSchemes.setEnabled(this, p, !on);
                refresh();
              } else {
                confirmDelete(p);
              }
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void confirmDelete(final String p) {
    new AlertDialog.Builder(this)
        .setTitle("Delete the scheme of " + AppGestureSchemes.label(this, p) + "?")
        .setMessage("Its gestures go back to the global scheme.")
        .setPositiveButton(
            android.R.string.ok,
            (d, w) -> {
              AppGestureSchemes.delete(this, p);
              refresh();
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }
}
