package com.google.android.accessibility.talkback.backup;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

/**
 * Backup and restore of ALL MS Screen Reader data: settings (including the Gemini key), sound
 * themes, clipboard history, labels and Lua files. Works with a folder (Google Drive or any other
 * folder picked with the system picker), automatically or by hand, and with a single local file.
 */
public class SettingsBackupActivity extends Activity {

  private static final int REQUEST_FOLDER = 9203;
  private static final int REQUEST_FILE_BACKUP = 9201;
  private static final int REQUEST_FILE_RESTORE = 9202;

  private TextView status;
  private TextView folderInfo;
  private final Handler main = new Handler(Looper.getMainLooper());

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTitle("Backup and restore");

    ScrollView scroll = new ScrollView(this);
    scroll.setBackgroundColor(Color.BLACK);
    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    int pad = (int) (16 * getResources().getDisplayMetrics().density);
    layout.setPadding(pad, pad, pad, pad);
    scroll.addView(layout);

    layout.addView(text("Backup and restore", 22f, true), wrap());
    layout.addView(
        text(
            "Saves everything: all settings, Gemini key and model, sound themes, clipboard"
                + " history, labels and Lua files.",
            15f,
            false),
        wrap());

    layout.addView(text("Folder backup (Google Drive or any folder)", 18f, true), wrap());
    folderInfo = text("", 15f, false);
    layout.addView(folderInfo, wrap());

    addButton(layout, "Choose backup folder", v -> chooseFolder());
    addButton(layout, "Back up now to the folder", v -> backupToFolderNow());
    addButton(layout, "Restore from the folder", v -> confirmRestoreFromFolder());

    Switch auto = new Switch(this);
    auto.setText("Automatic backup to the folder (about every 6 hours)");
    auto.setTextColor(Color.WHITE);
    auto.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
    auto.setChecked(MsBackupManager.state(this).getBoolean(MsBackupManager.KEY_AUTO, false));
    auto.setOnCheckedChangeListener(
        (CompoundButton b, boolean on) -> {
          MsBackupManager.state(this).edit().putBoolean(MsBackupManager.KEY_AUTO, on).apply();
          if (on && MsBackupManager.folderUri(this) == null) {
            say("Automatic backup is on, but choose a backup folder first");
          } else {
            say(on ? "Automatic backup on" : "Automatic backup off");
          }
        });
    layout.addView(auto, wrap());

    layout.addView(text("Single file backup", 18f, true), wrap());
    addButton(layout, "Back up to a file", v -> startFileBackup());
    addButton(layout, "Restore from a file", v -> startFileRestore());

    status = text("", 16f, false);
    layout.addView(status, wrap());

    setContentView(scroll);
    refreshFolderInfo();
  }

  private TextView text(String s, float sp, boolean heading) {
    TextView t = new TextView(this);
    t.setText(s);
    t.setTextColor(Color.WHITE);
    t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
    if (heading) {
      t.setAccessibilityHeading(true);
    }
    int p = (int) (8 * getResources().getDisplayMetrics().density);
    t.setPadding(0, p, 0, p);
    return t;
  }

  private void addButton(LinearLayout layout, String label, android.view.View.OnClickListener l) {
    Button b = new Button(this);
    b.setText(label);
    b.setOnClickListener(l);
    layout.addView(b, wrap());
  }

  private static LinearLayout.LayoutParams wrap() {
    return new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
  }

  private void say(String message) {
    status.setText(message);
    status.announceForAccessibility(message);
  }

  private void refreshFolderInfo() {
    Uri folder = MsBackupManager.folderUri(this);
    long last = MsBackupManager.state(this).getLong(MsBackupManager.KEY_LAST_TIME, 0L);
    String s = folder == null ? "No folder chosen yet." : "Folder is chosen.";
    if (last > 0) {
      s += " Last backup: " + DateFormat.format("d MMM yyyy, h:mm a", new Date(last));
    }
    folderInfo.setText(s);
  }

  // --- Folder -------------------------------------------------------------------------------

  private void chooseFolder() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
    intent.addFlags(
        Intent.FLAG_GRANT_READ_URI_PERMISSION
            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
    try {
      startActivityForResult(intent, REQUEST_FOLDER);
      say("Pick a folder in Google Drive, then press Use this folder");
    } catch (ActivityNotFoundException e) {
      say("No folder picker is available on this device");
    }
  }

  private void backupToFolderNow() {
    if (MsBackupManager.folderUri(this) == null) {
      say("Choose a backup folder first");
      return;
    }
    say("Backing up, please wait");
    new Thread(
            () -> {
              String result = MsBackupManager.backupToSavedFolder(getApplicationContext());
              main.post(
                  () -> {
                    say(result);
                    refreshFolderInfo();
                  });
            })
        .start();
  }

  private void confirmRestoreFromFolder() {
    Uri folder = MsBackupManager.folderUri(this);
    if (folder == null) {
      say("Choose the backup folder first");
      return;
    }
    new AlertDialog.Builder(this)
        .setTitle("Restore from the folder?")
        .setMessage("This replaces your current settings, sound themes, labels and Lua files.")
        .setPositiveButton("Restore", (d, w) -> restoreFromFolder(folder))
        .setNegativeButton("Cancel", null)
        .show();
  }

  private void restoreFromFolder(Uri folder) {
    say("Restoring, please wait");
    new Thread(
            () -> {
              String result;
              try {
                byte[] zip = MsBackupManager.readFromFolder(getApplicationContext(), folder);
                if (zip == null) {
                  result = "No backup file found in that folder";
                } else {
                  result = restoredMessage(MsBackupManager.restore(getApplicationContext(), zip));
                }
              } catch (Exception e) {
                result = "Restore failed: " + e.getMessage();
              }
              final String r = result;
              main.post(() -> say(r));
            })
        .start();
  }

  private static String restoredMessage(String summary) {
    return "Restored: " + summary + ". Turn the screen reader off and on once to apply everything.";
  }

  // --- Single file --------------------------------------------------------------------------

  private void startFileBackup() {
    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("application/zip");
    intent.putExtra(Intent.EXTRA_TITLE, MsBackupManager.BACKUP_FILE_NAME);
    try {
      startActivityForResult(intent, REQUEST_FILE_BACKUP);
    } catch (ActivityNotFoundException e) {
      say("No file picker is available on this device");
    }
  }

  private void startFileRestore() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("*/*");
    try {
      startActivityForResult(intent, REQUEST_FILE_RESTORE);
    } catch (ActivityNotFoundException e) {
      say("No file picker is available on this device");
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode != RESULT_OK || data == null || data.getData() == null) {
      return;
    }
    Uri uri = data.getData();
    if (requestCode == REQUEST_FOLDER) {
      MsBackupManager.setFolder(this, uri);
      refreshFolderInfo();
      say("Folder saved. Press Back up now, or turn on automatic backup");
    } else if (requestCode == REQUEST_FILE_BACKUP) {
      new Thread(
              () -> {
                String r;
                try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
                  if (out == null) {
                    r = "Could not write the backup file";
                  } else {
                    out.write(MsBackupManager.buildBackup(getApplicationContext()));
                    r = "Backed up to the file";
                  }
                } catch (Exception e) {
                  r = "Backup failed: " + e.getMessage();
                }
                final String rr = r;
                main.post(() -> say(rr));
              })
          .start();
    } else if (requestCode == REQUEST_FILE_RESTORE) {
      new AlertDialog.Builder(this)
          .setTitle("Restore from this file?")
          .setMessage("This replaces your current settings, sound themes, labels and Lua files.")
          .setPositiveButton("Restore", (d, w) -> restoreFromFile(uri))
          .setNegativeButton("Cancel", null)
          .show();
    }
  }

  private void restoreFromFile(Uri uri) {
    say("Restoring, please wait");
    new Thread(
            () -> {
              String r;
              try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) {
                  r = "Could not read the file";
                } else {
                  ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                  byte[] chunk = new byte[16384];
                  int n;
                  while ((n = in.read(chunk)) > 0) {
                    buffer.write(chunk, 0, n);
                  }
                  r = restoredMessage(MsBackupManager.restore(getApplicationContext(), buffer.toByteArray()));
                }
              } catch (Exception e) {
                r = "Restore failed: " + e.getMessage();
              }
              final String rr = r;
              main.post(() -> say(rr));
            })
        .start();
  }
}
