/*
 * Copyright 2026 Advanced Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Imports a .ppk (extension) or .xpk (tool) from a Uri (file manager, WhatsApp, Telegram...). */
final class PackageImporter {
  private PackageImporter() {}

  static String displayName(Activity a, Uri uri) {
    String name = null;
    if ("content".equals(uri.getScheme())) {
      try (Cursor c = a.getContentResolver().query(uri, null, null, null, null)) {
        if (c != null && c.moveToFirst()) {
          int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
          if (i >= 0) {
            name = c.getString(i);
          }
        }
      } catch (RuntimeException ignored) {
        // use the path below
      }
    }
    if (name == null) {
      name = uri.getLastPathSegment();
    }
    return name == null ? "package" : name;
  }

  /**
   * Copies the file to the cache, reads it and asks if the name is taken. forcedKind may be null
   * (then the file name decides; a name that is neither .ppk nor .xpk asks the person).
   */
  static void importUri(final Activity a, final Uri uri, final String forcedKind, final Runnable done) {
    final String fileName = displayName(a, uri);
    String k = forcedKind != null ? forcedKind : ExtensionPackages.kindForFileName(fileName);
    if (k == null) {
      new AlertDialog.Builder(a)
          .setTitle(fileName)
          .setMessage("Import this file as:")
          .setPositiveButton("Extension", (d, w) -> importUri(a, uri, ExtensionPackages.KIND_EXT, done))
          .setNeutralButton("Tool", (d, w) -> importUri(a, uri, ExtensionPackages.KIND_TOOL, done))
          .setNegativeButton(android.R.string.cancel, (d, w) -> finish(done))
          .setOnCancelListener(d -> finish(done))
          .show();
      return;
    }
    final String kind = k;
    final File tmp = new File(a.getCacheDir(), "import_" + System.nanoTime() + ".zip");
    try (InputStream in = a.getContentResolver().openInputStream(uri);
        OutputStream out = new FileOutputStream(tmp)) {
      if (in == null) {
        throw new IOException("Cannot open the file");
      }
      byte[] buf = new byte[8192];
      int n;
      long total = 0;
      while ((n = in.read(buf)) > 0) {
        total += n;
        if (total > 60L * 1024 * 1024) {
          throw new IOException("File is too big");
        }
        out.write(buf, 0, n);
      }
    } catch (IOException | RuntimeException e) {
      tmp.delete();
      Toast.makeText(a, "Could not read the file: " + e.getMessage(), Toast.LENGTH_LONG).show();
      finish(done);
      return;
    }
    final String name = ExtensionPackages.peekName(tmp, ExtensionPackages.baseNameOf(fileName));
    final File base = ExtensionPackages.KIND_TOOL.equals(kind) ? LuaPaths.tools(a) : LuaPaths.extensions(a);
    if (!new File(base, name).exists()) {
      doUnpack(a, tmp, base, name, false, kind, done);
      return;
    }
    new AlertDialog.Builder(a)
        .setTitle(name)
        .setMessage("Something with this name already exists.")
        .setPositiveButton("Replace", (d, w) -> doUnpack(a, tmp, base, name, true, kind, done))
        .setNeutralButton(
            "Keep both",
            (d, w) -> {
              String n = name + " 2";
              int i = 2;
              while (new File(base, n).exists()) {
                i++;
                n = name + " " + i;
              }
              doUnpack(a, tmp, base, n, false, kind, done);
            })
        .setNegativeButton(
            android.R.string.cancel,
            (d, w) -> {
              tmp.delete();
              finish(done);
            })
        .setOnCancelListener(
            d -> {
              tmp.delete();
              finish(done);
            })
        .show();
  }

  private static void doUnpack(
      Activity a, File tmp, File base, String name, boolean overwrite, String kind, Runnable done) {
    try {
      ExtensionPackages.Result r = ExtensionPackages.unpack(tmp, base, name, overwrite);
      Toast.makeText(
              a,
              (ExtensionPackages.KIND_TOOL.equals(kind) ? "Tool imported: " : "Extension imported: ")
                  + r.name,
              Toast.LENGTH_LONG)
          .show();
    } catch (IOException e) {
      Toast.makeText(a, "Import failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
    } finally {
      tmp.delete();
      finish(done);
    }
  }

  private static void finish(Runnable done) {
    if (done != null) {
      done.run();
    }
  }

  /** Share sheet for an exported package file (FileProvider path: cache/export/). */
  static void share(Activity a, File file, String title) {
    try {
      Uri uri =
          androidx.core.content.FileProvider.getUriForFile(
              a, a.getPackageName() + ".providers.FileProvider", file);
      Intent i = new Intent(Intent.ACTION_SEND);
      i.setType("application/octet-stream");
      i.putExtra(Intent.EXTRA_STREAM, uri);
      i.putExtra(Intent.EXTRA_SUBJECT, title);
      i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      a.startActivity(Intent.createChooser(i, title));
    } catch (RuntimeException e) {
      Toast.makeText(a, "Could not share the file", Toast.LENGTH_LONG).show();
    }
  }
}
