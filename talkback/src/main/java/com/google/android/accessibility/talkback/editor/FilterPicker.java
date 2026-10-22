/*
 * Copyright 2026 Advanced Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** A dialog with a search box on top and a list below that narrows while you type. */
final class FilterPicker {
  interface OnPick {
    void picked(String item);
  }

  private FilterPicker() {}

  static void show(Activity a, String title, final List<String> all, final OnPick cb) {
    LinearLayout box = new LinearLayout(a);
    box.setOrientation(LinearLayout.VERTICAL);
    final EditText search = new EditText(a);
    search.setHint("Enter keyword");
    search.setSingleLine(true);
    box.addView(search);
    final ListView list = new ListView(a);
    box.addView(
        list,
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    final List<String> shown = new ArrayList<>(all);
    final ArrayAdapter<String> adapter =
        new ArrayAdapter<>(a, android.R.layout.simple_list_item_1, shown);
    list.setAdapter(adapter);
    final AlertDialog dialog =
        new AlertDialog.Builder(a)
            .setTitle(title)
            .setView(box)
            .setNegativeButton(android.R.string.cancel, null)
            .create();
    list.setOnItemClickListener(
        (parent, view, position, id) -> {
          if (position < shown.size()) {
            String item = shown.get(position);
            dialog.dismiss();
            cb.picked(item);
          }
        });
    search.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int x, int y, int z) {}

          @Override
          public void onTextChanged(CharSequence s, int x, int y, int z) {}

          @Override
          public void afterTextChanged(Editable s) {
            String k = s.toString().trim().toLowerCase(Locale.ROOT);
            shown.clear();
            for (String it : all) {
              if (k.isEmpty() || it.toLowerCase(Locale.ROOT).contains(k)) {
                shown.add(it);
              }
            }
            adapter.notifyDataSetChanged();
          }
        });
    dialog.show();
  }
}
