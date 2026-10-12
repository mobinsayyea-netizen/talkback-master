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
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

/**
 * Multi-part gestures screen (Jieshuo class 52). Level 1: the 24 part 1 gestures, each "on"/"off"
 * with the number of part 2 actions. Tap one: level 2 with "Use ... multi-part gesture" (on/off)
 * and every part 2 gesture with its action. Tap a part 2 gesture to choose its action. Without
 * EXTRA_PKG the global scheme is edited, with EXTRA_PKG the scheme of that app. Not compiled yet.
 */
public class MultiPartGesturesActivity extends Activity {
  public static final String EXTRA_PKG = "scheme_pkg";
  private static final String EXTRA_FIRST = "first_part";

  private final List<String> labels = new ArrayList<>();
  private ArrayAdapter<String> adapter;
  private String pkg;
  private int first; // 0 = level 1

  static boolean show(Context c, String packageName) {
    try {
      Intent i = new Intent(c, MultiPartGesturesActivity.class);
      if (packageName != null) {
        i.putExtra(EXTRA_PKG, packageName);
      }
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
    pkg = getIntent().getStringExtra(EXTRA_PKG);
    first = getIntent().getIntExtra(EXTRA_FIRST, 0);
    String where = pkg == null ? "" : " of " + AppGestureSchemes.label(this, pkg);
    setTitle(
        first == 0
            ? "Multi-part gestures" + where
            : MsMultiPartGestures.firstName(first) + " multi-part gesture" + where);
    ListView list = new ListView(this);
    setContentView(list);
    adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels);
    list.setAdapter(adapter);
    list.setOnItemClickListener((parent, view, position, id) -> onTap(position));
    refresh();
  }

  @Override
  protected void onResume() {
    super.onResume();
    refresh();
  }

  private ExtensionGestures.Target current(int key) {
    return pkg == null
        ? ExtensionGestures.get(this, key)
        : AppGestureSchemes.get(this, pkg, key);
  }

  private void store(int key, String kind, String name) {
    if (pkg == null) {
      ExtensionGestures.set(this, key, kind, name);
    } else {
      AppGestureSchemes.set(this, pkg, key, kind, name);
    }
  }

  private void remove(int key) {
    if (pkg == null) {
      ExtensionGestures.clear(this, key);
    } else {
      AppGestureSchemes.clear(this, pkg, key);
    }
  }

  private boolean isOn(int firstId) {
    ExtensionGestures.Target t = current(MsMultiPartGestures.key(firstId, 0));
    return t != null && MsMultiPartGestures.KIND_SWITCH.equals(t.kind);
  }

  private int countSecond(int firstId) {
    int n = 0;
    for (int second : MsMultiPartGestures.SECOND_IDS) {
      if (current(MsMultiPartGestures.key(firstId, second)) != null) {
        n++;
      }
    }
    return n;
  }

  private void refresh() {
    if (adapter == null) {
      return;
    }
    labels.clear();
    if (first == 0) {
      if (!MsMultiPartGestures.globalEnabled(this)) {
        Toast.makeText(
                this,
                "Multi-part gestures are off. Turn on Use multi-part gestures in Gestures default"
                    + " settings.",
                Toast.LENGTH_LONG)
            .show();
      }
      for (int i = 0; i < MsMultiPartGestures.FIRST_IDS.length; i++) {
        int id = MsMultiPartGestures.FIRST_IDS[i];
        labels.add(
            MsMultiPartGestures.FIRST_NAMES[i]
                + " multi-part gesture, "
                + (isOn(id) ? "on" : "off")
                + ", "
                + countSecond(id)
                + " set");
      }
    } else {
      labels.add(
          "Use "
              + MsMultiPartGestures.firstName(first)
              + " multi-part gesture: "
              + (isOn(first) ? "on" : "off"));
      for (int i = 0; i < MsMultiPartGestures.SECOND_IDS.length; i++) {
        ExtensionGestures.Target t =
            current(MsMultiPartGestures.key(first, MsMultiPartGestures.SECOND_IDS[i]));
        labels.add(
            MsMultiPartGestures.SECOND_NAMES[i]
                + ": "
                + (t == null ? "no command" : describe(t)));
      }
    }
    adapter.notifyDataSetChanged();
  }

  private String describe(ExtensionGestures.Target t) {
    if ("tool".equals(t.kind)) {
      return "tool " + t.name;
    }
    if ("cmd".equals(t.kind)) {
      return "command " + t.name;
    }
    if (MsMultiPartGestures.KIND_TALKBACK.equals(t.kind)) {
      String n = talkbackActionName(t.name);
      return n == null ? "screen reader action" : n;
    }
    return t.name;
  }

  private void onTap(int position) {
    if (first == 0) {
      Intent i = new Intent(this, MultiPartGesturesActivity.class);
      i.putExtra(EXTRA_FIRST, MsMultiPartGestures.FIRST_IDS[position]);
      if (pkg != null) {
        i.putExtra(EXTRA_PKG, pkg);
      }
      startActivity(i);
    } else if (position == 0) {
      int k = MsMultiPartGestures.key(first, 0);
      if (isOn(first)) {
        remove(k);
      } else {
        store(k, MsMultiPartGestures.KIND_SWITCH, "on");
      }
      refresh();
    } else {
      chooseAction(MsMultiPartGestures.SECOND_IDS[position - 1],
          MsMultiPartGestures.SECOND_NAMES[position - 1]);
    }
  }

  private void chooseAction(final int second, final String secondName) {
    final List<CustomVoiceCommands.Entry> targets = CustomVoiceCommands.targets(this);
    final List<String> items = new ArrayList<>();
    items.add(pkg == null ? "No command (clear)" : "No command here (use the global scheme)");
    items.add("Screen reader action (TalkBack)...");
    items.add("Command (a screen reader function)...");
    for (CustomVoiceCommands.Entry t : targets) {
      items.add(("tool".equals(t.kind) ? "Tool: " : "Extension: ") + t.name);
    }
    new AlertDialog.Builder(this)
        .setTitle(secondName)
        .setItems(
            items.toArray(new String[0]),
            (d, which) -> {
              int k = MsMultiPartGestures.key(first, second);
              if (which == 0) {
                remove(k);
                refresh();
              } else if (which == 1) {
                chooseTalkbackAction(k, secondName);
              } else if (which == 2) {
                chooseCommand(k, secondName);
              } else {
                CustomVoiceCommands.Entry t = targets.get(which - 3);
                save(k, t.kind, t.name);
              }
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void save(int key, String kind, String name) {
    store(key, kind, name);
    // Putting an action on a pair also switches part 1 on, so it really works.
    int sw = MsMultiPartGestures.key(first, 0);
    if (!isOn(first)) {
      store(sw, MsMultiPartGestures.KIND_SWITCH, "on");
    }
    Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
    refresh();
  }

  private void chooseCommand(final int key, final String secondName) {
    final List<String> names = new ArrayList<>();
    for (String[] row : JxCommands.TABLE) {
      if (!"command_nothing".equals(row[0])) {
        names.add(row[1]);
      }
    }
    new AlertDialog.Builder(this)
        .setTitle(secondName + ": command")
        .setItems(names.toArray(new String[0]), (d, which) -> save(key, "cmd", names.get(which)))
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private static final int[] TB_NAME_ARRAYS = {
    com.google.android.accessibility.talkback.R.array.shortcut_basic_navigation,
    com.google.android.accessibility.talkback.R.array.shortcut_system_actions,
    com.google.android.accessibility.talkback.R.array.shortcut_reading_control,
    com.google.android.accessibility.talkback.R.array.shortcut_menu_control,
    com.google.android.accessibility.talkback.R.array.shortcut_text_editing
  };
  private static final int[] TB_VALUE_ARRAYS = {
    com.google.android.accessibility.talkback.R.array.shortcut_value_basic_navigation,
    com.google.android.accessibility.talkback.R.array.shortcut_value_system_actions,
    com.google.android.accessibility.talkback.R.array.shortcut_value_reading_control,
    com.google.android.accessibility.talkback.R.array.shortcut_value_menu_control,
    com.google.android.accessibility.talkback.R.array.shortcut_value_text_editing
  };

  /** All TalkBack shortcut actions as {name, value} pairs. */
  private List<String[]> talkbackActions() {
    List<String[]> out = new ArrayList<>();
    for (int i = 0; i < TB_NAME_ARRAYS.length; i++) {
      String[] names = getResources().getStringArray(TB_NAME_ARRAYS[i]);
      String[] values = getResources().getStringArray(TB_VALUE_ARRAYS[i]);
      for (int j = 0; j < Math.min(names.length, values.length); j++) {
        out.add(new String[] {names[j], values[j]});
      }
    }
    return out;
  }

  private String talkbackActionName(String value) {
    for (String[] p : talkbackActions()) {
      if (p[1].equals(value)) {
        return p[0];
      }
    }
    return null;
  }

  private void chooseTalkbackAction(final int key, final String secondName) {
    final List<String[]> all = talkbackActions();
    String[] names = new String[all.size()];
    for (int i = 0; i < names.length; i++) {
      names[i] = all.get(i)[0];
    }
    new AlertDialog.Builder(this)
        .setTitle(secondName + ": screen reader action")
        .setItems(
            names,
            (d, which) -> save(key, MsMultiPartGestures.KIND_TALKBACK, all.get(which)[1]))
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }
}
