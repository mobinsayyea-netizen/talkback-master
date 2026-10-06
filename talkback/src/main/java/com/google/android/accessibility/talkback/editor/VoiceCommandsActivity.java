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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

/** "Custom voice commands": add a spoken phrase for an extension or tool, or delete one. */
public class VoiceCommandsActivity extends Activity {
  private final List<CustomVoiceCommands.Entry> entries = new ArrayList<>();
  private final List<String> titles = new ArrayList<>();
  private ArrayAdapter<String> adapter;

  public static boolean show(Context context) {
    try {
      Intent i = new Intent(context, VoiceCommandsActivity.class);
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
    setTitle("Custom voice commands");
    ListView list = new ListView(this);
    setContentView(list);
    adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, titles);
    list.setAdapter(adapter);
    list.setOnItemClickListener(
        (parent, view, position, id) -> {
          if (position == 0) {
            askPhrase();
          } else {
            askDelete(entries.get(position - 1));
          }
        });
    reload();
  }

  private void reload() {
    entries.clear();
    entries.addAll(CustomVoiceCommands.load(this));
    titles.clear();
    titles.add("New voice command");
    for (CustomVoiceCommands.Entry e : entries) {
      titles.add(e.phrase + ", " + ("tool".equals(e.kind) ? "tool " : "extension ") + e.name);
    }
    adapter.notifyDataSetChanged();
  }

  private void askPhrase() {
    final EditText input = new EditText(this);
    input.setHint("What do you say?");
    new AlertDialog.Builder(this)
        .setTitle("New voice command")
        .setView(input)
        .setPositiveButton(
            android.R.string.ok,
            (d, w) -> {
              String phrase = input.getText().toString().trim();
              if (phrase.isEmpty()) {
                return;
              }
              askTarget(phrase);
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void askTarget(final String phrase) {
    final List<CustomVoiceCommands.Entry> targets = CustomVoiceCommands.targets(this);
    if (targets.isEmpty()) {
      Toast.makeText(this, "No extension or tool yet. Create one first.", Toast.LENGTH_LONG).show();
      return;
    }
    String[] names = new String[targets.size()];
    for (int i = 0; i < names.length; i++) {
      names[i] = ("tool".equals(targets.get(i).kind) ? "Tool: " : "Extension: ") + targets.get(i).name;
    }
    new AlertDialog.Builder(this)
        .setTitle("What should it start?")
        .setItems(
            names,
            (d, which) -> {
              CustomVoiceCommands.Entry t = targets.get(which);
              List<CustomVoiceCommands.Entry> all = CustomVoiceCommands.load(this);
              // The same phrase can only start one thing: replace the old one.
              String key = CustomVoiceCommands.normalize(phrase);
              for (int i = all.size() - 1; i >= 0; i--) {
                if (CustomVoiceCommands.normalize(all.get(i).phrase).equals(key)) {
                  all.remove(i);
                }
              }
              all.add(new CustomVoiceCommands.Entry(phrase, t.kind, t.name));
              if (!CustomVoiceCommands.save(this, all)) {
                Toast.makeText(this, "Could not save", Toast.LENGTH_SHORT).show();
              }
              reload();
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void askDelete(final CustomVoiceCommands.Entry e) {
    new AlertDialog.Builder(this)
        .setTitle(e.phrase)
        .setMessage("Delete this voice command?")
        .setPositiveButton(
            "Delete",
            (d, w) -> {
              List<CustomVoiceCommands.Entry> all = CustomVoiceCommands.load(this);
              String key = CustomVoiceCommands.normalize(e.phrase);
              for (int i = all.size() - 1; i >= 0; i--) {
                if (CustomVoiceCommands.normalize(all.get(i).phrase).equals(key)) {
                  all.remove(i);
                }
              }
              CustomVoiceCommands.save(this, all);
              reload();
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }
}
