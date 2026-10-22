/*
 * Copyright 2026 Advanced Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

/**
 * Lists the 16 one-finger swipe gestures with the extension or tool on each. Tap = choose. A
 * gesture with an extension no longer does its normal TalkBack action. Without EXTRA_PKG this is
 * the global scheme; with EXTRA_PKG it is the gesture scheme of that one app (gestures left "not
 * set" there use the global scheme). Not compiled yet.
 */
public class ExtensionGesturesActivity extends Activity {
  public static final String EXTRA_KIND = "pick_kind";
  public static final String EXTRA_NAME = "pick_name";
  public static final String EXTRA_PKG = "scheme_pkg";

  private final List<String> labels = new ArrayList<>();
  private ArrayAdapter<String> adapter;
  private String pickKind;
  private String pickName;
  private String pkg;

  /** Opens the list to put one given extension or tool on a gesture. */
  static boolean showFor(Context c, String kind, String name) {
    try {
      Intent i = new Intent(c, ExtensionGesturesActivity.class);
      i.putExtra(EXTRA_KIND, kind);
      i.putExtra(EXTRA_NAME, name);
      i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      c.startActivity(i);
      return true;
    } catch (RuntimeException e) {
      return false;
    }
  }

  /** Opens the gesture scheme of one app. */
  static boolean showForApp(Context c, String packageName) {
    try {
      Intent i = new Intent(c, ExtensionGesturesActivity.class);
      i.putExtra(EXTRA_PKG, packageName);
      if (!(c instanceof Activity)) {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      }
      c.startActivity(i);
      return true;
    } catch (RuntimeException e) {
      return false;
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    pickKind = getIntent().getStringExtra(EXTRA_KIND);
    pickName = getIntent().getStringExtra(EXTRA_NAME);
    pkg = getIntent().getStringExtra(EXTRA_PKG);
    if (pkg != null) {
      setTitle("Gesture scheme of " + AppGestureSchemes.label(this, pkg));
    } else {
      setTitle(pickName == null ? "Extension gestures" : "Choose a gesture for " + pickName);
    }
    ListView list = new ListView(this);
    setContentView(list);
    adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels);
    list.setAdapter(adapter);
    list.setOnItemClickListener(
        (parent, view, position, id) -> {
          if (pickName != null) {
            assign(position, pickKind, pickName);
          } else {
            choose(position);
          }
        });
    refresh();
  }

  private ExtensionGestures.Target current(int gestureId) {
    return pkg == null
        ? ExtensionGestures.get(this, gestureId)
        : AppGestureSchemes.get(this, pkg, gestureId);
  }

  private void store(int gestureId, String kind, String name) {
    if (pkg == null) {
      ExtensionGestures.set(this, gestureId, kind, name);
    } else {
      AppGestureSchemes.set(this, pkg, gestureId, kind, name);
    }
  }

  private void remove(int gestureId) {
    if (pkg == null) {
      ExtensionGestures.clear(this, gestureId);
    } else {
      AppGestureSchemes.clear(this, pkg, gestureId);
    }
  }

  private void refresh() {
    labels.clear();
    for (int i = 0; i < ExtensionGestures.IDS.length; i++) {
      ExtensionGestures.Target t = current(ExtensionGestures.IDS[i]);
      String notSet = pkg == null ? "not set" : "not set, uses the global scheme";
      labels.add(
          ExtensionGestures.NAMES[i]
              + ": "
              + (t == null ? notSet : describe(t)));
    }
    adapter.notifyDataSetChanged();
  }

  private static String describe(ExtensionGestures.Target t) {
    if ("tool".equals(t.kind)) {
      return "tool " + t.name;
    }
    return "cmd".equals(t.kind) ? "command " + t.name : t.name;
  }

  private void assign(final int index, final String kind, final String name) {
    final int gesture = ExtensionGestures.IDS[index];
    ExtensionGestures.Target old = current(gesture);
    String where = pkg == null ? "" : " in " + AppGestureSchemes.label(this, pkg);
    String msg =
        "The gesture \"" + ExtensionGestures.NAMES[index] + "\" will run " + name + where
            + " instead of its normal screen reader action."
            + (old == null ? "" : "\nIt now runs " + old.name + ", which will be replaced.");
    new AlertDialog.Builder(this)
        .setTitle(ExtensionGestures.NAMES[index])
        .setMessage(msg)
        .setPositiveButton(
            android.R.string.ok,
            (d, w) -> {
              store(gesture, kind, name);
              Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
              if (pickName != null) {
                finish();
              } else {
                refresh();
              }
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void choose(final int index) {
    final List<CustomVoiceCommands.Entry> targets = CustomVoiceCommands.targets(this);
    final List<String> items = new ArrayList<>();
    items.add(
        pkg == null ? "No extension (normal action)" : "No extension here (use the global scheme)");
    items.add("Command (a screen reader function)...");
    for (CustomVoiceCommands.Entry t : targets) {
      items.add(("tool".equals(t.kind) ? "Tool: " : "Extension: ") + t.name);
    }
    new AlertDialog.Builder(this)
        .setTitle(ExtensionGestures.NAMES[index])
        .setItems(
            items.toArray(new String[0]),
            (d, which) -> {
              if (which == 0) {
                remove(ExtensionGestures.IDS[index]);
                refresh();
              } else if (which == 1) {
                chooseCommand(index);
              } else {
                CustomVoiceCommands.Entry t = targets.get(which - 2);
                assign(index, t.kind, t.name);
              }
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  /** Lists every execute() command (English name); the chosen one runs on the gesture. */
  private void chooseCommand(final int index) {
    final List<String> names = new ArrayList<>();
    for (String[] row : JxCommands.TABLE) {
      if (!"command_nothing".equals(row[0])) {
        names.add(row[1]);
      }
    }
    new AlertDialog.Builder(this)
        .setTitle(ExtensionGestures.NAMES[index] + ": command")
        .setItems(
            names.toArray(new String[0]), (d, which) -> assign(index, "cmd", names.get(which)))
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }
}
