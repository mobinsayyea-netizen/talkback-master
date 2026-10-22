/*
 * Copyright 2026 Advanced Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Resource manager: import .ppk/.xpk packages, back up and restore the extension files (Extensions,
 * Tools, Custom voice commands) as one zip, and a link to the full backup of every setting.
 */
public class ResourceManagerActivity extends Activity {
  private static final int REQ_IMPORT = 101;
  private static final int REQ_SAVE_ALL = 102;
  private static final int REQ_RESTORE_ALL = 103;

  private TextView summary;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTitle("Resource manager");
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    summary = new TextView(this);
    summary.setPadding(24, 24, 24, 24);
    root.addView(summary);
    add(root, "Import extension or tool (.ppk / .xpk)", v -> pickPackage());
    add(root, "Extensions", v -> ExtensionListActivity.show(this, "ext", false));
    add(root, "Tools", v -> ExtensionListActivity.show(this, "tool", false));
    add(root, "Custom voice commands", v -> startActivity(new Intent(this, VoiceCommandsActivity.class)));
    add(root, "Extension gestures", v -> startActivity(new Intent(this, ExtensionGesturesActivity.class)));
    add(root, "Per-application gesture schemes", v -> startActivity(new Intent(this, AppGestureSchemesActivity.class)));
    add(root, "Save extensions to a zip file", v -> saveAll());
    add(root, "Restore extensions from a zip file", v -> restoreAll());
    add(
        root,
        "Full backup and restore (all settings)",
        v ->
            startActivity(
                new Intent(
                    this,
                    com.google.android.accessibility.talkback.backup.SettingsBackupActivity.class)));
    ScrollView sv = new ScrollView(this);
    sv.addView(root);
    setContentView(sv);
  }

  @Override
  protected void onResume() {
    super.onResume();
    int e = count(LuaPaths.extensions(this));
    int t = count(LuaPaths.tools(this));
    summary.setText(e + " extensions, " + t + " tools\nFolder: " + LuaPaths.base(this).getPath());
  }

  private static int count(File dir) {
    int n = 0;
    File[] k = dir.listFiles();
    if (k != null) {
      for (File f : k) {
        if (f.isDirectory() && new File(f, "main.lua").isFile()) {
          n++;
        }
      }
    }
    return n;
  }

  private void add(LinearLayout root, String label, View.OnClickListener l) {
    Button b = new Button(this);
    b.setText(label);
    b.setAllCaps(false);
    b.setOnClickListener(l);
    root.addView(
        b,
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
  }

  private void pickPackage() {
    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    i.addCategory(Intent.CATEGORY_OPENABLE);
    i.setType("*/*");
    startActivityForResult(i, REQ_IMPORT);
  }

  private void saveAll() {
    Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
    i.addCategory(Intent.CATEGORY_OPENABLE);
    i.setType("application/zip");
    i.putExtra(Intent.EXTRA_TITLE, "MS-extensions-backup.zip");
    startActivityForResult(i, REQ_SAVE_ALL);
  }

  private void restoreAll() {
    new AlertDialog.Builder(this)
        .setTitle("Restore extensions")
        .setMessage("Files with the same name will be replaced. Continue?")
        .setPositiveButton(
            android.R.string.ok,
            (d, w) -> {
              Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
              i.addCategory(Intent.CATEGORY_OPENABLE);
              i.setType("*/*");
              startActivityForResult(i, REQ_RESTORE_ALL);
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  @Override
  protected void onActivityResult(int req, int result, Intent data) {
    super.onActivityResult(req, result, data);
    if (result != RESULT_OK || data == null || data.getData() == null) {
      return;
    }
    Uri uri = data.getData();
    if (req == REQ_IMPORT) {
      PackageImporter.importUri(this, uri, null, this::onResume);
    } else if (req == REQ_SAVE_ALL) {
      try (OutputStream out = getContentResolver().openOutputStream(uri)) {
        ExtensionPackages.packAll(LuaPaths.base(this), out);
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
      } catch (IOException | RuntimeException e) {
        Toast.makeText(this, "Could not save: " + e.getMessage(), Toast.LENGTH_LONG).show();
      }
    } else if (req == REQ_RESTORE_ALL) {
      File tmp = new File(getCacheDir(), "restore_" + System.nanoTime() + ".zip");
      try (InputStream in = getContentResolver().openInputStream(uri);
          OutputStream out = new FileOutputStream(tmp)) {
        byte[] buf = new byte[8192];
        int n;
        while (in != null && (n = in.read(buf)) > 0) {
          out.write(buf, 0, n);
        }
        out.flush();
        int files = ExtensionPackages.restoreAll(tmp, LuaPaths.base(this));
        Toast.makeText(this, files + " files restored", Toast.LENGTH_LONG).show();
      } catch (IOException | RuntimeException e) {
        Toast.makeText(this, "Could not restore: " + e.getMessage(), Toast.LENGTH_LONG).show();
      } finally {
        tmp.delete();
        onResume();
      }
    }
  }

  static boolean show(android.content.Context c) {
    try {
      Intent i = new Intent(c, ResourceManagerActivity.class);
      i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      c.startActivity(i);
      return true;
    } catch (RuntimeException e) {
      return false;
    }
  }
}
