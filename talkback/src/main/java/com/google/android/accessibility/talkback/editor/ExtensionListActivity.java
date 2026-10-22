/*
 * Copyright 2026 Advanced Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

/**
 * One screen for Extensions and one for Tools, laid out the way Jieshuo does it: a search box, a New
 * button and the list. Tap = run. Touch and hold (double tap and hold) = Add to home, Delete, Edit
 * and a few more. Not compiled yet.
 */
public class ExtensionListActivity extends Activity {
  public static final String EXTRA_KIND = "kind"; // "ext" or "tool"
  public static final String EXTRA_AUTO_NEW = "auto_new";

  private static final int REQ_IMPORT = 201;
  private String kind = "ext";
  private final List<String> all = new ArrayList<>();
  private final List<String> shown = new ArrayList<>();
  private final List<String> labels = new ArrayList<>();
  private ArrayAdapter<String> adapter;
  private EditText search;

  public static boolean show(Context context, String kind, boolean autoNew) {
    try {
      Intent i = new Intent(context, ExtensionListActivity.class);
      i.putExtra(EXTRA_KIND, kind);
      i.putExtra(EXTRA_AUTO_NEW, autoNew);
      i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(i);
      return true;
    } catch (RuntimeException e) {
      return false;
    }
  }

  private boolean isTool() {
    return "tool".equals(kind);
  }

  private File base() {
    return isTool() ? LuaPaths.tools(this) : LuaPaths.extensions(this);
  }

  private File dir(String name) {
    return new File(base(), name);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    String k = getIntent().getStringExtra(EXTRA_KIND);
    kind = "tool".equals(k) ? "tool" : "ext";
    setTitle(isTool() ? "Tools" : "Extensions");

    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    search = new EditText(this);
    search.setHint("Enter keyword");
    search.setSingleLine(true);
    root.addView(
        search,
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    Button add = new Button(this);
    add.setText("New");
    add.setOnClickListener(v -> askNewName());
    root.addView(
        add,
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    Button imp = new Button(this);
    imp.setText("Import (.ppk / .xpk)");
    imp.setOnClickListener(v -> {
      Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
      pick.addCategory(Intent.CATEGORY_OPENABLE);
      pick.setType("*/*");
      startActivityForResult(pick, REQ_IMPORT);
    });
    root.addView(
        imp,
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    ListView list = new ListView(this);
    root.addView(
        list,
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
    setContentView(root);

    adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels);
    list.setAdapter(adapter);
    list.setOnItemClickListener(
        (parent, view, position, id) -> {
          if (position < shown.size()) {
            run(shown.get(position));
          }
        });
    list.setOnItemLongClickListener(
        (parent, view, position, id) -> {
          if (position < shown.size()) {
            actions(shown.get(position));
            return true;
          }
          return false;
        });
    search.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int a, int b, int c) {}

          @Override
          public void onTextChanged(CharSequence s, int a, int b, int c) {}

          @Override
          public void afterTextChanged(Editable s) {
            refilter();
          }
        });

    if (savedInstanceState == null && getIntent().getBooleanExtra(EXTRA_AUTO_NEW, false)) {
      getIntent().removeExtra(EXTRA_AUTO_NEW);
      askNewName();
    }
  }

  @Override
  protected void onActivityResult(int req, int result, Intent data) {
    super.onActivityResult(req, result, data);
    if (req == REQ_IMPORT && result == RESULT_OK && data != null && data.getData() != null) {
      PackageImporter.importUri(this, data.getData(), kind, this::reload);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    reload();
  }

  private void reload() {
    all.clear();
    File[] dirs = base().listFiles();
    if (dirs != null) {
      java.util.Arrays.sort(dirs);
      for (File d : dirs) {
        if (d.isDirectory() && new File(d, "main.lua").isFile()) {
          all.add(d.getName());
        }
      }
    }
    refilter();
  }

  private void refilter() {
    String key = search.getText().toString().trim().toLowerCase(Locale.ROOT);
    shown.clear();
    labels.clear();
    for (String n : all) {
      if (key.isEmpty() || n.toLowerCase(Locale.ROOT).contains(key)) {
        shown.add(n);
        labels.add(n);
      }
    }
    if (shown.isEmpty()) {
      labels.add(isTool() ? "There are no tools" : "There are no extensions");
    }
    adapter.notifyDataSetChanged();
  }

  private void run(String name) {
    CustomVoiceCommands.run(this, new CustomVoiceCommands.Entry("", kind, name));
  }

  // ---------------------------------------------------------------- new

  private static String clean(String s) {
    return s.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
  }

  private void askNewName() {
    final EditText input = new EditText(this);
    input.setHint("Name");
    new AlertDialog.Builder(this)
        .setTitle(isTool() ? "New tool" : "New extension")
        .setView(input)
        .setPositiveButton(
            android.R.string.ok,
            (d, w) -> {
              String n = clean(input.getText().toString());
              if (n.isEmpty()) {
                return;
              }
              File dir = dir(n);
              if (dir.exists() && new File(dir, "main.lua").isFile()) {
                Toast.makeText(this, "That name is already used", Toast.LENGTH_LONG).show();
                return;
              }
              dir.mkdirs();
              edit(n);
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void edit(String name) {
    Intent i = new Intent(this, LuaEditorActivity.class);
    i.putExtra(LuaEditorActivity.EXTRA_FILE_PATH, new File(dir(name), "main.lua").getPath());
    startActivity(i);
  }

  // ---------------------------------------------------------------- long press menu

  private boolean madeByBuilder(String name) {
    if (isTool()) {
      return false;
    }
    try (BufferedReader r = new BufferedReader(new FileReader(new File(dir(name), "main.lua")))) {
      String line;
      while ((line = r.readLine()) != null) {
        if (line.trim().equals("-- steps begin")) {
          return true;
        }
      }
    } catch (IOException ex) {
      // not readable: treat as a normal one
    }
    return false;
  }

  private void actions(final String name) {
    final List<String> items = new ArrayList<>();
    items.add("Add to home");
    items.add("Delete");
    items.add("Edit");
    if (madeByBuilder(name)) {
      items.add("Edit steps");
    }
    items.add("Duplicate");
    items.add("Rename");
    items.add("Add voice command");
    items.add("Send");
    items.add("Export package");
    items.add("Assign to gesture");
    new AlertDialog.Builder(this)
        .setTitle(name)
        .setItems(
            items.toArray(new String[0]),
            (d, which) -> {
              String l = items.get(which);
              if (l.equals("Add to home")) {
                addToHome(name);
              } else if (l.equals("Delete")) {
                askDelete(name);
              } else if (l.equals("Edit")) {
                edit(name);
              } else if (l.equals("Edit steps")) {
                AutoClickBuilderActivity.show(this, name);
              } else if (l.equals("Duplicate")) {
                duplicate(name);
              } else if (l.equals("Rename")) {
                askRename(name);
              } else if (l.equals("Add voice command")) {
                askPhrase(name);
              } else if (l.equals("Send")) {
                send(name);
              } else if (l.equals("Export package")) {
                exportPackage(name);
              } else if (l.equals("Assign to gesture")) {
                ExtensionGesturesActivity.showFor(this, kind, name);
              }
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void addToHome(String name) {
    try {
      ShortcutManager sm = getSystemService(ShortcutManager.class);
      if (sm == null || !sm.isRequestPinShortcutSupported()) {
        Toast.makeText(this, "This launcher cannot add shortcuts", Toast.LENGTH_LONG).show();
        return;
      }
      Intent run = new Intent(this, RunShortcutActivity.class);
      run.setAction(RunShortcutActivity.ACTION_RUN);
      run.putExtra(RunShortcutActivity.EXTRA_KIND, kind);
      run.putExtra(RunShortcutActivity.EXTRA_NAME, name);
      ShortcutInfo info =
          new ShortcutInfo.Builder(this, kind + ":" + name)
              .setShortLabel(name)
              .setLongLabel(name)
              .setIcon(Icon.createWithResource(this, getApplicationInfo().icon))
              .setIntent(run)
              .build();
      sm.requestPinShortcut(info, null);
    } catch (RuntimeException e) {
      Toast.makeText(this, "Could not add the shortcut", Toast.LENGTH_LONG).show();
    }
  }

  private void askDelete(final String name) {
    new AlertDialog.Builder(this)
        .setTitle(name)
        .setMessage("Delete this and its files? This cannot be undone.")
        .setPositiveButton(
            "Delete",
            (d, w) -> {
              deleteRecursive(dir(name));
              CustomVoiceCommands.removeFor(this, kind, name);
              ExtensionGestures.removeFor(this, kind, name);
              reload();
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void duplicate(String name) {
    String n = name + " 2";
    int i = 2;
    while (dir(n).exists()) {
      i++;
      n = name + " " + i;
    }
    try {
      copyRecursive(dir(name), dir(n));
      reload();
      Toast.makeText(this, "Created: " + n, Toast.LENGTH_SHORT).show();
    } catch (IOException e) {
      Toast.makeText(this, "Duplicate failed", Toast.LENGTH_LONG).show();
    }
  }

  private void askRename(final String name) {
    final EditText input = new EditText(this);
    input.setText(name);
    new AlertDialog.Builder(this)
        .setTitle("Rename")
        .setView(input)
        .setPositiveButton(
            android.R.string.ok,
            (d, w) -> {
              String n = clean(input.getText().toString());
              if (n.isEmpty() || n.equals(name)) {
                return;
              }
              File target = dir(n);
              if (target.exists()) {
                Toast.makeText(this, "That name is already used", Toast.LENGTH_LONG).show();
                return;
              }
              if (dir(name).renameTo(target)) {
                CustomVoiceCommands.renameFor(this, kind, name, n);
                ExtensionGestures.renameFor(this, kind, name, n);
                reload();
              } else {
                Toast.makeText(this, "Rename failed", Toast.LENGTH_LONG).show();
              }
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void askPhrase(final String name) {
    final EditText input = new EditText(this);
    input.setHint("What do you say?");
    new AlertDialog.Builder(this)
        .setTitle("New voice command")
        .setView(input)
        .setPositiveButton(
            android.R.string.ok,
            (d, w) -> {
              String phrase = input.getText().toString().trim();
              if (!phrase.isEmpty() && CustomVoiceCommands.add(this, phrase, kind, name)) {
                Toast.makeText(this, "Saved: " + phrase, Toast.LENGTH_SHORT).show();
              }
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void exportPackage(String name) {
    try {
      File dir = new File(getCacheDir(), "export");
      dir.mkdirs();
      File out = new File(dir, ExtensionPackages.fileNameFor(kind, name));
      try (OutputStream o = new FileOutputStream(out)) {
        ExtensionPackages.pack(dir(name), kind, name, o);
      }
      PackageImporter.share(this, out, name);
    } catch (IOException | RuntimeException e) {
      Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
    }
  }

  private void send(String name) {
    StringBuilder sb = new StringBuilder();
    try (BufferedReader r = new BufferedReader(new FileReader(new File(dir(name), "main.lua")))) {
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
    i.putExtra(Intent.EXTRA_SUBJECT, name);
    i.putExtra(Intent.EXTRA_TEXT, sb.toString());
    startActivity(Intent.createChooser(i, name));
  }

  // ---------------------------------------------------------------- files

  private static void deleteRecursive(File f) {
    File[] kids = f.listFiles();
    if (kids != null) {
      for (File k : kids) {
        deleteRecursive(k);
      }
    }
    f.delete();
  }

  private static void copyRecursive(File from, File to) throws IOException {
    if (from.isDirectory()) {
      to.mkdirs();
      File[] kids = from.listFiles();
      if (kids != null) {
        for (File k : kids) {
          copyRecursive(k, new File(to, k.getName()));
        }
      }
      return;
    }
    try (InputStream in = new FileInputStream(from);
        OutputStream out = new FileOutputStream(to)) {
      byte[] buf = new byte[8192];
      int n;
      while ((n = in.read(buf)) > 0) {
        out.write(buf, 0, n);
      }
    }
  }
}
