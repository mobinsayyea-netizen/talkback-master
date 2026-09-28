package com.google.android.accessibility.talkback.backup;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Backs up MS Screen Reader's settings to a file the user chooses (so they survive an uninstall),
 * and restores them from such a file.
 */
public class SettingsBackupActivity extends Activity {

  private static final int REQUEST_BACKUP = 9201;
  private static final int REQUEST_RESTORE = 9202;
  private static final String BACKUP_FILE_NAME = "ms-screen-reader-settings.json";

  private TextView status;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTitle("Backup and restore settings");

    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setBackgroundColor(Color.BLACK);
    int pad = (int) (16 * getResources().getDisplayMetrics().density);
    layout.setPadding(pad, pad, pad, pad);

    TextView heading = new TextView(this);
    heading.setText("Backup and restore settings");
    heading.setTextColor(Color.WHITE);
    heading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f);
    layout.addView(heading, wrap());

    Button backup = new Button(this);
    backup.setText("Back up settings to a file");
    backup.setOnClickListener(v -> startBackup());
    layout.addView(backup, wrap());

    Button restore = new Button(this);
    restore.setText("Restore settings from a file");
    restore.setOnClickListener(v -> startRestore());
    layout.addView(restore, wrap());

    status = new TextView(this);
    status.setTextColor(Color.WHITE);
    status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
    layout.addView(status, wrap());

    setContentView(layout);
  }

  private static LinearLayout.LayoutParams wrap() {
    return new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
  }

  private void say(String message) {
    status.setText(message);
    status.announceForAccessibility(message);
  }

  private void startBackup() {
    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("application/json");
    intent.putExtra(Intent.EXTRA_TITLE, BACKUP_FILE_NAME);
    try {
      startActivityForResult(intent, REQUEST_BACKUP);
    } catch (ActivityNotFoundException e) {
      say("No file picker is available on this device");
    }
  }

  private void startRestore() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("*/*");
    try {
      startActivityForResult(intent, REQUEST_RESTORE);
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
    if (requestCode == REQUEST_BACKUP) {
      writeBackup(uri);
    } else if (requestCode == REQUEST_RESTORE) {
      readBackup(uri);
    }
  }

  private void writeBackup(Uri uri) {
    try {
      SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(this);
      JSONObject values = new JSONObject();
      for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
        Object value = entry.getValue();
        JSONObject item = new JSONObject();
        if (value instanceof Boolean) {
          item.put("t", "b");
          item.put("v", ((Boolean) value).booleanValue());
        } else if (value instanceof Integer) {
          item.put("t", "i");
          item.put("v", ((Integer) value).intValue());
        } else if (value instanceof Long) {
          item.put("t", "l");
          item.put("v", ((Long) value).longValue());
        } else if (value instanceof Float) {
          item.put("t", "f");
          item.put("v", ((Float) value).doubleValue());
        } else if (value instanceof String) {
          item.put("t", "s");
          item.put("v", (String) value);
        } else if (value instanceof Set) {
          JSONArray array = new JSONArray();
          for (Object o : (Set<?>) value) {
            array.put(String.valueOf(o));
          }
          item.put("t", "S");
          item.put("v", array);
        } else {
          continue;
        }
        values.put(entry.getKey(), item);
      }
      JSONObject root = new JSONObject();
      root.put("app", "MS Screen Reader");
      root.put("version", 1);
      root.put("values", values);
      try (OutputStream out = getContentResolver().openOutputStream(uri)) {
        if (out == null) {
          say("Could not write the backup file");
          return;
        }
        out.write(root.toString().getBytes(StandardCharsets.UTF_8));
      }
      say("Settings backed up");
    } catch (Exception e) {
      say("Backup failed");
    }
  }

  private void readBackup(Uri uri) {
    try {
      String text;
      try (InputStream in = getContentResolver().openInputStream(uri)) {
        if (in == null) {
          say("Could not read the backup file");
          return;
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = in.read(chunk)) > 0) {
          buffer.write(chunk, 0, n);
        }
        text = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
      }
      JSONObject root = new JSONObject(text);
      if (!"MS Screen Reader".equals(root.optString("app"))) {
        say("This is not an MS Screen Reader backup file");
        return;
      }
      JSONObject values = root.getJSONObject("values");
      SharedPreferences.Editor editor = SharedPreferencesUtils.getSharedPreferences(this).edit();
      int count = 0;
      java.util.Iterator<String> keys = values.keys();
      while (keys.hasNext()) {
        String key = keys.next();
        JSONObject item = values.getJSONObject(key);
        String type = item.getString("t");
        switch (type) {
          case "b":
            editor.putBoolean(key, item.getBoolean("v"));
            break;
          case "i":
            editor.putInt(key, item.getInt("v"));
            break;
          case "l":
            editor.putLong(key, item.getLong("v"));
            break;
          case "f":
            editor.putFloat(key, (float) item.getDouble("v"));
            break;
          case "s":
            editor.putString(key, item.getString("v"));
            break;
          case "S":
            JSONArray array = item.getJSONArray("v");
            Set<String> set = new HashSet<>();
            for (int i = 0; i < array.length(); i++) {
              set.add(array.getString(i));
            }
            editor.putStringSet(key, set);
            break;
          default:
            continue;
        }
        count++;
      }
      editor.apply();
      say("Settings restored: " + count + " items");
    } catch (JSONException e) {
      say("This file is not a valid backup");
    } catch (Exception e) {
      say("Restore failed");
    }
  }
}
