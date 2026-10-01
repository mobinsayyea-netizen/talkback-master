package com.google.android.accessibility.talkback.editor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import java.io.File;

/** Folders for Lua extensions and tools. Uses /sdcard/TalkBack when allowed, else the app folder. */
public final class LuaPaths {
  private LuaPaths() {}

  public static boolean hasAllFilesAccess(Context c) {
    if (Build.VERSION.SDK_INT >= 30) {
      return Environment.isExternalStorageManager();
    }
    return c.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        == android.content.pm.PackageManager.PERMISSION_GRANTED;
  }

  public static File base(Context c) {
    if (hasAllFilesAccess(c)) {
      File pub = new File(Environment.getExternalStorageDirectory(), "TalkBack");
      pub.mkdirs();
      if (pub.isDirectory() && pub.canWrite()) {
        return pub;
      }
    }
    File app = c.getExternalFilesDir(null);
    if (app == null) {
      app = c.getFilesDir();
    }
    File f = new File(app, "TalkBack");
    f.mkdirs();
    return f;
  }

  public static File extensions(Context c) {
    File f = new File(base(c), "Extensions");
    f.mkdirs();
    return f;
  }

  public static File tools(Context c) {
    File f = new File(base(c), "Tools");
    f.mkdirs();
    return f;
  }

  public static void requestAllFilesAccess(Activity a) {
    try {
      if (Build.VERSION.SDK_INT >= 30) {
        Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        i.setData(Uri.parse("package:" + a.getPackageName()));
        a.startActivity(i);
      } else {
        a.requestPermissions(
            new String[] {
              android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
              android.Manifest.permission.READ_EXTERNAL_STORAGE
            },
            1);
      }
    } catch (Exception e) {
      try {
        a.startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
      } catch (Exception ignored) {
      }
    }
  }
}
