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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manager for extensions and tools: run, edit, edit steps (functions made with the automatic click
 * builder), rename, delete, send, and add a voice command. Not compiled yet.
 */
public class ExtensionManagerActivity extends Activity {
  private final List<CustomVoiceCommands.Entry> items = new ArrayList<>();
  private final List<String> titles = new ArrayList<>();
  private ArrayAdapter<String> adapter;

  public static boolean show(Context context) {
    try {
      Intent i = new Intent(context, ExtensionManagerActivity.class);
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
    setTitle("Manage extensions and tools");
    ListView list = new ListView(this);
    setContentView(list);
    adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, titles);
    list.setAdapter(adapter);
    list.setOnItemClickListener(
        (parent, view, position, id) -> {
          if (position < items.size()) {
            actions(items.get(position));
          }
        });
  }

  @Override
  protected void onResume() {
    super.onResume();
    reload(); // after editing or renaming, the list is read again
  }

  private void reload() {
    items.clear();
    items.addAll(CustomVoiceCommands.targets(this));
    titles.clear();
    for (CustomVoiceCommands.Entry e : items) {
      titles.add(("tool".equals(e.kind) ? "Tool: " : "Extension: ") + e.name);
    }
    if (titles.isEmpty()) {
      titles.add("Nothing here yet. Create an extension or a tool first.");
    }
    adapter.notifyDataSetChanged();
  }

  private File dir(CustomVoiceCommands.Entry e) {
    File base = "tool".equals(e.kind) ? LuaPaths.tools(this) : LuaPaths.extensions(this);
    return new File(base, e.name);
  }

  /** True when the extension was made by the automatic click builder. */
  private boolean madeByBuilder(CustomVoiceCommands.Entry e) {
    if (!"ext".equals(e.kind)) {
      return false;
    }
    try (BufferedReader r = new BufferedReader(new FileReader(new File(dir(e), "main.lua")))) {
      String line;
      while ((line = r.readLine()) != null) {
        if (line.trim().equals("-- steps begin")) {
          return true;
        }
      }
    } catch (IOException ex) {
      // not readable: treat as a normal extension
    }
    return false;
  }

  private void actions(final CustomVoiceCommands.Entry e) {
    final boolean builder = madeByBuilder(e);
    final List<String> labels = new ArrayList<>();
    labels.add("Run");
    labels.add("Edit code");
    if (builder) {
      labels.add("Edit steps");
    }
    labels.add("Add voice command");
    labels.add("Rename");
    labels.add("Send");
    labels.add("Delete");
    new AlertDialog.Builder(this)
        .setTitle(e.name)
        .setItems(
            labels.toArray(new String[0]),
            (d, which) -> {
              String l = labels.get(which);
              if (l.equals("Run")) {
                CustomVoiceCommands.run(this, e);
              } else if (l.equals("Edit code")) {
                Intent i = new Intent(this, LuaEditorActivity.class);
                i.putExtra(LuaEditorActivity.EXTRA_FILE_PATH, new File(dir(e), "main.lua").getPath());
                startActivity(i);
              } else if (l.equals("Edit steps")) {
                AutoClickBuilderActivity.show(this, e.name);
              } else if (l.equals("Add voice command")) {
                askPhrase(e);
              } else if (l.equals("Rename")) {
                askRename(e);
              } else if (l.equals("Send")) {
                send(e);
              } else if (l.equals("Delete")) {
                askDelete(e);
              }
            })
        .show();
  }

  private void askPhrase(final CustomVoiceCommands.Entry e) {
    final EditText input = new EditText(this);
    input.setHint("What do you say?");
    new AlertDialog.Builder(this)
        .setTitle("New voice command")
        .setView(input)
        .setPositiveButton(
            android.R.string.ok,
            (d, w) -> {
              String phrase = input.getText().toString().trim();
              if (!phrase.isEmpty() && CustomVoiceCommands.add(this, phrase, e.kind, e.name)) {
                Toast.makeText(this, "Saved: " + phrase, Toast.LENGTH_SHORT).show();
              }
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void askRename(final CustomVoiceCommands.Entry e) {
    final EditText input = new EditText(this);
    input.setText(e.name);
    new AlertDialog.Builder(this)
        .setTitle("Rename")
        .setView(input)
        .setPositiveButton(
            android.R.string.ok,
            (d, w) -> {
              String n = input.getText().toString().trim().replaceAll("[\\\\/:*?\"<>|]", "_");
              if (n.isEmpty() || n.equals(e.name)) {
                return;
              }
              File target = new File(dir(e).getParentFile(), n);
              if (target.exists()) {
                Toast.makeText(this, "That name is already used", Toast.LENGTH_LONG).show();
                return;
              }
              if (dir(e).renameTo(target)) {
                CustomVoiceCommands.renameFor(this, e.kind, e.name, n);
                reload();
              } else {
                Toast.makeText(this, "Rename failed", Toast.LENGTH_LONG).show();
              }
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void askDelete(final CustomVoiceCommands.Entry e) {
    new AlertDialog.Builder(this)
        .setTitle(e.name)
        .setMessage("Delete this and its files? This cannot be undone.")
        .setPositiveButton(
            "Delete",
            (d, w) -> {
              deleteRecursive(dir(e));
              CustomVoiceCommands.removeFor(this, e.kind, e.name);
              reload();
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private static void deleteRecursive(File f) {
    File[] kids = f.listFiles();
    if (kids != null) {
      for (File k : kids) {
        deleteRecursive(k);
      }
    }
    f.delete();
  }

  /** Sends the main.lua text with any app that takes text (messages, mail, notes). */
  private void send(CustomVoiceCommands.Entry e) {
    StringBuilder sb = new StringBuilder();
    try (BufferedReader r = new BufferedReader(new FileReader(new File(dir(e), "main.lua")))) {
      String line;
      while ((line = r.readLine()) != null) {
        sb.append(line).append('\n');
      }
    } catch (IOException ex) {
      Toast.makeText(this, "Could not read the file", Toast.LENGTH_SHORT).show();
      return;
    }
    Intent i = new Intent(Intent.ACTION_SEND);
    i.setType("text/plain");
    i.putExtra(Intent.EXTRA_SUBJECT, e.name);
    i.putExtra(Intent.EXTRA_TEXT, sb.toString());
    startActivity(Intent.createChooser(i, e.name));
  }
}
