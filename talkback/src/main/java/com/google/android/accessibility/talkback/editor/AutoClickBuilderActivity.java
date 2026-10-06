/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * "Create automatic click function": builds an extension whose main.lua is one
 * service.click({{ step, step, ... }}) call (the inner braces = steps run one after another).
 * Steps are written with Jieshuo's symbols (see JIESHUO_SYMBOLS_STATUS_hi.md). Opening an existing
 * builder-made extension edits its steps. Not compiled yet.
 */
public class AutoClickBuilderActivity extends Activity {
  public static final String EXTRA_NAME = "name";
  private static final String BEGIN = "-- steps begin";
  private static final String END = "-- steps end";

  private final List<String> steps = new ArrayList<>();
  private final List<String> shown = new ArrayList<>();
  private ArrayAdapter<String> adapter;
  private String name;

  public static boolean show(Context context, String existingName) {
    try {
      Intent i = new Intent(context, AutoClickBuilderActivity.class);
      if (existingName != null) {
        i.putExtra(EXTRA_NAME, existingName);
      }
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
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    ListView list = new ListView(this);
    root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1f));
    Button add = new Button(this);
    add.setText("Add step");
    Button test = new Button(this);
    test.setText("Test");
    Button save = new Button(this);
    save.setText("Save");
    root.addView(add, new LinearLayout.LayoutParams(-1, -2));
    root.addView(test, new LinearLayout.LayoutParams(-1, -2));
    root.addView(save, new LinearLayout.LayoutParams(-1, -2));
    setContentView(root);

    adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, shown);
    list.setAdapter(adapter);
    list.setOnItemClickListener((p, v, position, id) -> stepMenu(position));
    add.setOnClickListener(v -> chooseType(-1));
    test.setOnClickListener(v -> testRun());
    save.setOnClickListener(v -> save());

    name = getIntent().getStringExtra(EXTRA_NAME);
    if (name != null) {
      setTitle(name);
      load(name);
      refresh();
    } else {
      setTitle("Create automatic click function");
      askName();
    }
  }

  // ------------------------------------------------------------------ name

  private void askName() {
    final EditText input = new EditText(this);
    input.setHint("Function name");
    new AlertDialog.Builder(this)
        .setTitle("Create automatic click function")
        .setView(input)
        .setCancelable(false)
        .setPositiveButton(
            android.R.string.ok,
            (d, w) -> {
              String n = input.getText().toString().trim().replaceAll("[\\\\/:*?\"<>|]", "_");
              if (n.isEmpty()) {
                finish();
                return;
              }
              name = n;
              setTitle(name);
            })
        .setNegativeButton(android.R.string.cancel, (d, w) -> finish())
        .show();
  }

  // ------------------------------------------------------------------ list of steps

  private void refresh() {
    shown.clear();
    for (int i = 0; i < steps.size(); i++) {
      shown.add("Step " + (i + 1) + ": " + steps.get(i));
    }
    if (shown.isEmpty()) {
      shown.add("No steps yet. Use Add step.");
    }
    adapter.notifyDataSetChanged();
  }

  private void stepMenu(final int index) {
    if (index < 0 || index >= steps.size()) {
      return;
    }
    String[] items = {"Edit", "Move up", "Move down", "Delete"};
    new AlertDialog.Builder(this)
        .setTitle("Step " + (index + 1))
        .setItems(
            items,
            (d, which) -> {
              if (which == 0) {
                chooseType(index);
              } else if (which == 1 && index > 0) {
                steps.add(index - 1, steps.remove(index));
              } else if (which == 2 && index < steps.size() - 1) {
                steps.add(index + 1, steps.remove(index));
              } else if (which == 3) {
                steps.remove(index);
              }
              refresh();
            })
        .show();
  }

  // ------------------------------------------------------------------ add / edit one step

  private static final String[] TYPES = {
    "Press a button by its name",
    "Press a button by my own label",
    "Open an app",
    "Run a command or function",
    "Tap a point on the screen",
    "Swipe",
    "Find a colour and tap it",
    "Write the line myself"
  };

  /** index = -1 adds a new step, otherwise the step at index is replaced. */
  private void chooseType(final int index) {
    new AlertDialog.Builder(this)
        .setTitle(index < 0 ? "Add step" : "Edit step")
        .setItems(TYPES, (d, which) -> askDetails(which, index))
        .show();
  }

  private void askDetails(final int type, final int index) {
    final String[] hints;
    final boolean pauseField;
    switch (type) {
      case 0:
      case 1:
        hints = new String[] {"Button name (use | for \"or\", * for any characters)", "Only in app (optional)"};
        pauseField = true;
        break;
      case 2:
        hints = new String[] {"App name"};
        pauseField = true;
        break;
      case 3:
        hints = new String[] {"Command or function, for example recent application"};
        pauseField = true;
        break;
      case 4:
        hints = new String[] {"Left (pixels, or 0.5 for half)", "Top (pixels, or 0.5 for half)"};
        pauseField = true;
        break;
      case 5:
        hints = new String[] {"From left", "From top", "To left", "To top", "Time in ms"};
        pauseField = true;
        break;
      case 6:
        hints =
            new String[] {
              "Rectangle left", "Rectangle top", "Rectangle right", "Rectangle bottom",
              "Red 0-255", "Green 0-255", "Blue 0-255", "Allowed difference"
            };
        pauseField = true;
        break;
      default:
        hints = new String[] {"The whole step line"};
        pauseField = false;
        break;
    }
    LinearLayout box = new LinearLayout(this);
    box.setOrientation(LinearLayout.VERTICAL);
    final List<EditText> fields = new ArrayList<>();
    for (String h : hints) {
      EditText e = new EditText(this);
      e.setHint(h);
      if (type >= 4 && type <= 6) {
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
      }
      box.addView(e);
      fields.add(e);
    }
    final EditText pause = new EditText(this);
    if (pauseField) {
      pause.setHint("Pause after this step in ms (optional)");
      pause.setInputType(InputType.TYPE_CLASS_NUMBER);
      box.addView(pause);
    }
    new AlertDialog.Builder(this)
        .setTitle(TYPES[type])
        .setView(box)
        .setPositiveButton(
            android.R.string.ok,
            (d, w) -> {
              List<String> v = new ArrayList<>();
              for (EditText e : fields) {
                v.add(e.getText().toString().trim());
              }
              String line = buildLine(type, v, pause.getText().toString().trim());
              if (line == null) {
                Toast.makeText(this, "Please fill every box", Toast.LENGTH_SHORT).show();
                return;
              }
              String problem = JxClickScript.checkItem(line);
              if (problem != null) {
                Toast.makeText(this, problem, Toast.LENGTH_LONG).show();
                return;
              }
              if (index < 0) {
                steps.add(line);
              } else {
                steps.set(index, line);
              }
              refresh();
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  /** Writes one step in Jieshuo's symbol language. Null when a needed box is empty. */
  static String buildLine(int type, List<String> v, String pauseMs) {
    for (int i = 0; i < v.size(); i++) {
      boolean optional = type <= 1 && i == 1; // "Only in app" may stay empty
      if (v.get(i).isEmpty() && !optional) {
        return null;
      }
    }
    String pause = pauseMs.isEmpty() ? "" : "$" + pauseMs;
    switch (type) {
      case 0:
      case 1:
        {
          String app = v.size() > 1 && !v.get(1).isEmpty() ? "@" + v.get(1) : "";
          return (type == 1 ? "#" : "") + v.get(0) + app + pause;
        }
      case 2:
        return ">" + v.get(0) + pause;
      case 3:
        return "%" + v.get(0) + pause;
      case 4:
        return "[" + String.join(",", v) + "]" + pause;
      case 5:
        return "[" + String.join(",", v) + "]" + pause;
      case 6:
        return "[" + String.join(",", v) + "]" + pause;
      default:
        return v.get(0);
    }
  }

  // ------------------------------------------------------------------ save / load / test

  private static String luaString(String s) {
    return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }

  private String script() {
    StringBuilder sb = new StringBuilder();
    sb.append("-- title: ").append(name).append('\n');
    sb.append("-- menu: on\n");
    sb.append("-- event: \n");
    sb.append("-- Made with Create automatic click function. Steps run one after another.\n");
    sb.append("service.click({{\n").append(BEGIN).append('\n');
    for (int i = 0; i < steps.size(); i++) {
      sb.append("  ").append(luaString(steps.get(i))).append(i < steps.size() - 1 ? "," : "").append('\n');
    }
    sb.append(END).append("\n}})\n");
    return sb.toString();
  }

  private void load(String n) {
    File main = new File(new File(LuaPaths.extensions(this), n), "main.lua");
    boolean inside = false;
    try (BufferedReader r = new BufferedReader(new FileReader(main))) {
      String line;
      while ((line = r.readLine()) != null) {
        String t = line.trim();
        if (t.equals(BEGIN)) {
          inside = true;
        } else if (t.equals(END)) {
          break;
        } else if (inside && t.startsWith("\"")) {
          if (t.endsWith(",")) {
            t = t.substring(0, t.length() - 1);
          }
          if (t.length() >= 2 && t.endsWith("\"")) {
            steps.add(t.substring(1, t.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\"));
          }
        }
      }
    } catch (IOException e) {
      Toast.makeText(this, "Could not read this function", Toast.LENGTH_SHORT).show();
    }
  }

  private void save() {
    if (name == null) {
      askName();
      return;
    }
    if (steps.isEmpty()) {
      Toast.makeText(this, "Add at least one step", Toast.LENGTH_SHORT).show();
      return;
    }
    File dir = new File(LuaPaths.extensions(this), name);
    dir.mkdirs();
    try (FileWriter w = new FileWriter(new File(dir, "main.lua"))) {
      w.write(script());
      Toast.makeText(this, "Saved: " + name, Toast.LENGTH_SHORT).show();
    } catch (IOException e) {
      Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
    }
  }

  /** Runs the steps after 3 seconds, with this screen moved behind, so they act on the app below. */
  private void testRun() {
    if (steps.isEmpty()) {
      Toast.makeText(this, "Add at least one step", Toast.LENGTH_SHORT).show();
      return;
    }
    final Context app = getApplicationContext();
    final File dir = new File(app.getCacheDir(), "ms_autoclick_test");
    dir.mkdirs();
    final File main = new File(dir, "main.lua");
    try (FileWriter w = new FileWriter(main)) {
      w.write(script());
    } catch (IOException e) {
      Toast.makeText(this, "Could not start the test", Toast.LENGTH_SHORT).show();
      return;
    }
    Toast.makeText(this, "Test starts in 3 seconds", Toast.LENGTH_SHORT).show();
    moveTaskToBack(true);
    new Handler(Looper.getMainLooper())
        .postDelayed(
            () ->
                new Thread(
                        () -> {
                          String err = LuaServiceApi.run(app, main, null);
                          LuaServiceApi.speakStatic(err != null ? err : "Test finished");
                        },
                        "ms-autoclick-test")
                    .start(),
            3000);
  }
}
