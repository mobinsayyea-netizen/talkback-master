package com.google.android.accessibility.talkback.backup;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.DocumentsContract;
import com.google.android.accessibility.talkback.editor.LuaPaths;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * MS Screen Reader: full backup and restore. One zip file holds EVERYTHING the app keeps:
 *
 * <ul>
 *   <li>every settings file (main settings, Gemini key and model, translate, sound theme choice,
 *       Lua IDE, and any other settings file the app has),
 *   <li>sound themes (theme list, slot assignments and all sound files),
 *   <li>clipboard history,
 *   <li>custom labels,
 *   <li>Lua files (app lua folder, Extensions and Tools folders).
 * </ul>
 *
 * The zip can be saved to a normal file, or into any folder picked with the system folder picker
 * (this includes a Google Drive folder, because the Drive app shows up in that picker).
 */
public final class MsBackupManager {

  public static final String BACKUP_FILE_NAME = "ms-screen-reader-backup.zip";
  public static final String PREFS_NAME = "ms_backup"; // device-only state, never backed up
  public static final String KEY_FOLDER_URI = "folder_uri";
  public static final String KEY_AUTO = "auto_backup";
  public static final String KEY_LAST_TIME = "last_backup_time";
  public static final String KEY_LAST_RESULT = "last_backup_result";
  public static final long AUTO_INTERVAL_MS = 6L * 60L * 60L * 1000L;

  private static final String ENTRY_MANIFEST = "manifest.json";
  private static final String DIR_PREFS = "prefs/";
  private static final String DIR_FILES = "files/";
  private static final String DIR_EXT = "ext/";
  private static final String ENTRY_LABELS = "labels.json";

  /** Settings files that belong to the device or to libraries, not to the user. */
  private static boolean skipPrefsFile(String name) {
    return name.equals(PREFS_NAME)
        || name.equals("ms_updater")
        || name.startsWith("com.google.android.gms")
        || name.startsWith("com.google.firebase")
        || name.startsWith("androidx.")
        || name.startsWith("WebView")
        || name.startsWith("FirebaseHeartBeat")
        || name.startsWith("Primes");
  }

  /** Folders and files inside the app's private "files" folder that are part of the backup. */
  private static final String[] FILES_TO_SAVE = {
    "sound_themes", "ms_clipboard_history.json", "lua"
  };

  private MsBackupManager() {}

  // ---------------------------------------------------------------------------------------------
  // Small state kept on this device (which folder, auto on/off, last time)

  public static SharedPreferences state(Context c) {
    return c.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
  }

  public static Uri folderUri(Context c) {
    String s = state(c).getString(KEY_FOLDER_URI, "");
    return (s == null || s.isEmpty()) ? null : Uri.parse(s);
  }

  /** Remembers the chosen folder and keeps permission to use it after restarts. */
  public static void setFolder(Context c, Uri tree) {
    try {
      c.getContentResolver()
          .takePersistableUriPermission(
              tree, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    } catch (Exception ignored) {
      // Some providers do not allow persisting; the folder may need picking again later.
    }
    state(c).edit().putString(KEY_FOLDER_URI, tree.toString()).apply();
  }

  // ---------------------------------------------------------------------------------------------
  // Build the backup zip

  public static byte[] buildBackup(Context context) throws Exception {
    Context app = context.getApplicationContext();
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
      JSONObject manifest = new JSONObject();
      manifest.put("app", "MS Screen Reader");
      manifest.put("version", 2);
      manifest.put("time", System.currentTimeMillis());
      putText(zip, ENTRY_MANIFEST, manifest.toString());

      // 1. Every settings file.
      File prefsDir = new File(app.getApplicationInfo().dataDir, "shared_prefs");
      File[] prefFiles = prefsDir.listFiles();
      if (prefFiles != null) {
        for (File f : prefFiles) {
          String fn = f.getName();
          if (!fn.endsWith(".xml")) {
            continue;
          }
          String name = fn.substring(0, fn.length() - 4);
          if (skipPrefsFile(name)) {
            continue;
          }
          SharedPreferences sp = app.getSharedPreferences(name, Context.MODE_PRIVATE);
          putText(zip, DIR_PREFS + name + ".json", prefsToJson(sp.getAll()).toString());
        }
      }

      // 2. Files in the private files folder.
      File filesDir = app.getFilesDir();
      for (String rel : FILES_TO_SAVE) {
        addTree(zip, new File(filesDir, rel), DIR_FILES + rel);
      }

      // 3. Lua extension and tool folders (public TalkBack folder or app folder).
      try {
        File base = LuaPaths.base(app);
        addTree(zip, new File(base, "Extensions"), DIR_EXT + "Extensions");
        addTree(zip, new File(base, "Tools"), DIR_EXT + "Tools");
      } catch (Exception ignored) {
        // Folder not readable: skip, the rest of the backup is still valid.
      }

      // 4. Custom labels.
      String labels = labelsToJson(app);
      if (labels != null) {
        putText(zip, ENTRY_LABELS, labels);
      }
    }
    return bytes.toByteArray();
  }

  private static void putText(ZipOutputStream zip, String name, String text) throws IOException {
    zip.putNextEntry(new ZipEntry(name));
    zip.write(text.getBytes(StandardCharsets.UTF_8));
    zip.closeEntry();
  }

  private static void addTree(ZipOutputStream zip, File file, String entryName)
      throws IOException {
    if (file == null || !file.exists()) {
      return;
    }
    if (file.isDirectory()) {
      File[] children = file.listFiles();
      if (children != null) {
        for (File child : children) {
          addTree(zip, child, entryName + "/" + child.getName());
        }
      }
      return;
    }
    zip.putNextEntry(new ZipEntry(entryName));
    try (InputStream in = new FileInputStream(file)) {
      copy(in, zip);
    }
    zip.closeEntry();
  }

  private static JSONObject prefsToJson(Map<String, ?> all) throws Exception {
    JSONObject values = new JSONObject();
    for (Map.Entry<String, ?> entry : all.entrySet()) {
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
    return values;
  }

  private static String labelsToJson(Context app) {
    File db = app.getDatabasePath("labelsDatabase.db");
    if (db == null || !db.exists()) {
      return null;
    }
    SQLiteDatabase database = null;
    Cursor cursor = null;
    try {
      database = SQLiteDatabase.openDatabase(db.getPath(), null, SQLiteDatabase.OPEN_READONLY);
      cursor = database.query("labels", null, null, null, null, null, null);
      JSONArray rows = new JSONArray();
      while (cursor.moveToNext()) {
        JSONObject row = new JSONObject();
        for (int i = 0; i < cursor.getColumnCount(); i++) {
          String col = cursor.getColumnName(i);
          if ("_id".equals(col)) {
            continue;
          }
          switch (cursor.getType(i)) {
            case Cursor.FIELD_TYPE_INTEGER:
              row.put(col, cursor.getLong(i));
              break;
            case Cursor.FIELD_TYPE_FLOAT:
              row.put(col, cursor.getDouble(i));
              break;
            case Cursor.FIELD_TYPE_STRING:
              row.put(col, cursor.getString(i));
              break;
            default:
              break;
          }
        }
        rows.put(row);
      }
      return rows.toString();
    } catch (Exception e) {
      return null;
    } finally {
      if (cursor != null) {
        cursor.close();
      }
      if (database != null) {
        database.close();
      }
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Restore

  /** Returns a short summary such as "12 settings files, 45 files, 3 labels". */
  public static String restore(Context context, byte[] zipBytes) throws Exception {
    Context app = context.getApplicationContext();
    int prefFilesCount = 0;
    int itemCount = 0;
    int fileCount = 0;
    int labelCount = 0;
    boolean valid = false;
    Set<String> clearedDirs = new HashSet<>();
    File filesDir = app.getFilesDir();
    File extBase = null;
    try {
      extBase = LuaPaths.base(app);
    } catch (Exception ignored) {
      // no extension folder available
    }

    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        String name = entry.getName();
        if (entry.isDirectory() || name.contains("..")) {
          continue;
        }
        byte[] data = readAll(zip);
        if (name.equals(ENTRY_MANIFEST)) {
          JSONObject m = new JSONObject(new String(data, StandardCharsets.UTF_8));
          if (!"MS Screen Reader".equals(m.optString("app"))) {
            throw new IOException("not an MS Screen Reader backup");
          }
          valid = true;
        } else if (name.startsWith(DIR_PREFS) && name.endsWith(".json")) {
          String prefName = name.substring(DIR_PREFS.length(), name.length() - 5);
          if (skipPrefsFile(prefName)) {
            continue;
          }
          itemCount += applyPrefs(app, prefName, new JSONObject(new String(data, StandardCharsets.UTF_8)));
          prefFilesCount++;
        } else if (name.startsWith(DIR_FILES)) {
          String rel = name.substring(DIR_FILES.length());
          String top = rel.contains("/") ? rel.substring(0, rel.indexOf('/')) : rel;
          if (isSavedTop(top)) {
            // Replace each saved folder as a whole the first time we meet it.
            if (rel.contains("/") && clearedDirs.add(top)) {
              deleteTree(new File(filesDir, top));
            }
            writeFile(new File(filesDir, rel), data);
            fileCount++;
          }
        } else if (name.startsWith(DIR_EXT) && extBase != null) {
          String rel = name.substring(DIR_EXT.length());
          String top = rel.contains("/") ? rel.substring(0, rel.indexOf('/')) : rel;
          if (top.equals("Extensions") || top.equals("Tools")) {
            if (clearedDirs.add("ext:" + top)) {
              deleteTree(new File(extBase, top));
            }
            writeFile(new File(extBase, rel), data);
            fileCount++;
          }
        } else if (name.equals(ENTRY_LABELS)) {
          labelCount = applyLabels(app, new JSONArray(new String(data, StandardCharsets.UTF_8)));
        }
      }
    }
    if (!valid) {
      throw new IOException("not an MS Screen Reader backup");
    }
    return prefFilesCount
        + " settings files ("
        + itemCount
        + " items), "
        + fileCount
        + " files, "
        + labelCount
        + " labels";
  }

  private static boolean isSavedTop(String top) {
    for (String s : FILES_TO_SAVE) {
      if (s.equals(top)) {
        return true;
      }
    }
    return false;
  }

  private static int applyPrefs(Context app, String prefName, JSONObject values) throws Exception {
    SharedPreferences.Editor editor =
        app.getSharedPreferences(prefName, Context.MODE_PRIVATE).edit();
    int count = 0;
    Iterator<String> keys = values.keys();
    while (keys.hasNext()) {
      String key = keys.next();
      JSONObject item = values.getJSONObject(key);
      switch (item.getString("t")) {
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
    editor.commit();
    return count;
  }

  private static int applyLabels(Context app, JSONArray rows) {
    SQLiteDatabase database = null;
    try {
      File db = app.getDatabasePath("labelsDatabase.db");
      if (db == null || !db.exists()) {
        return 0; // the label database is created by the app itself on first use
      }
      database = SQLiteDatabase.openDatabase(db.getPath(), null, SQLiteDatabase.OPEN_READWRITE);
      database.beginTransaction();
      database.delete("labels", null, null);
      int n = 0;
      for (int i = 0; i < rows.length(); i++) {
        JSONObject row = rows.getJSONObject(i);
        ContentValues cv = new ContentValues();
        Iterator<String> keys = row.keys();
        while (keys.hasNext()) {
          String k = keys.next();
          Object v = row.get(k);
          if (v instanceof Integer) {
            cv.put(k, (Integer) v);
          } else if (v instanceof Long) {
            cv.put(k, (Long) v);
          } else if (v instanceof Double) {
            cv.put(k, (Double) v);
          } else if (v instanceof String) {
            cv.put(k, (String) v);
          }
        }
        if (database.insert("labels", null, cv) != -1) {
          n++;
        }
      }
      database.setTransactionSuccessful();
      return n;
    } catch (Exception e) {
      return 0;
    } finally {
      if (database != null) {
        try {
          database.endTransaction();
        } catch (Exception ignored) {
          // not in a transaction
        }
        database.close();
      }
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Folder (Google Drive or any other folder) read and write

  /** Writes the backup zip into the chosen folder, replacing the previous backup file. */
  public static void writeToFolder(Context context, Uri tree, byte[] zip) throws Exception {
    ContentResolver cr = context.getContentResolver();
    Uri parent =
        DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree));
    Uri existing = findChild(cr, tree, BACKUP_FILE_NAME);
    Uri target = existing;
    if (target == null) {
      target = DocumentsContract.createDocument(cr, parent, "application/zip", BACKUP_FILE_NAME);
    }
    if (target == null) {
      throw new IOException("could not create the file in that folder");
    }
    try (OutputStream out = cr.openOutputStream(target, "wt")) {
      if (out == null) {
        throw new IOException("could not write to that folder");
      }
      out.write(zip);
      out.flush();
    }
  }

  /** Reads the backup zip from the chosen folder. Returns null if there is none. */
  public static byte[] readFromFolder(Context context, Uri tree) throws Exception {
    ContentResolver cr = context.getContentResolver();
    Uri doc = findChild(cr, tree, BACKUP_FILE_NAME);
    if (doc == null) {
      return null;
    }
    try (InputStream in = cr.openInputStream(doc)) {
      return in == null ? null : readAll(in);
    }
  }

  private static Uri findChild(ContentResolver cr, Uri tree, String displayName) {
    String parentId = DocumentsContract.getTreeDocumentId(tree);
    Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId);
    List<Uri> found = new ArrayList<>();
    try (Cursor c =
        cr.query(
            children,
            new String[] {
              DocumentsContract.Document.COLUMN_DOCUMENT_ID,
              DocumentsContract.Document.COLUMN_DISPLAY_NAME
            },
            null,
            null,
            null)) {
      if (c != null) {
        while (c.moveToNext()) {
          if (displayName.equals(c.getString(1))) {
            found.add(DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0)));
          }
        }
      }
    } catch (Exception e) {
      return null;
    }
    return found.isEmpty() ? null : found.get(0);
  }

  // ---------------------------------------------------------------------------------------------
  // One-call helpers used by the screen and by the automatic backup

  /** Builds a backup and writes it to the saved folder. Returns a short result text. */
  public static String backupToSavedFolder(Context context) {
    Uri tree = folderUri(context);
    if (tree == null) {
      return "No backup folder chosen";
    }
    try {
      byte[] zip = buildBackup(context);
      writeToFolder(context, tree, zip);
      state(context)
          .edit()
          .putLong(KEY_LAST_TIME, System.currentTimeMillis())
          .putString(KEY_LAST_RESULT, "ok")
          .apply();
      return "Backup saved to the folder";
    } catch (Exception e) {
      state(context).edit().putString(KEY_LAST_RESULT, "failed").apply();
      return "Backup failed: " + e.getMessage();
    }
  }

  public static boolean autoBackupDue(Context context) {
    SharedPreferences s = state(context);
    if (!s.getBoolean(KEY_AUTO, false) || folderUri(context) == null) {
      return false;
    }
    return System.currentTimeMillis() - s.getLong(KEY_LAST_TIME, 0L) >= AUTO_INTERVAL_MS;
  }

  // ---------------------------------------------------------------------------------------------
  // Utilities

  private static void writeFile(File f, byte[] data) throws IOException {
    File parent = f.getParentFile();
    if (parent != null) {
      parent.mkdirs();
    }
    try (FileOutputStream out = new FileOutputStream(f)) {
      out.write(data);
    }
  }

  private static void deleteTree(File f) {
    if (f == null || !f.exists()) {
      return;
    }
    if (f.isDirectory()) {
      File[] children = f.listFiles();
      if (children != null) {
        for (File c : children) {
          deleteTree(c);
        }
      }
    }
    f.delete();
  }

  private static void copy(InputStream in, OutputStream out) throws IOException {
    byte[] buf = new byte[16384];
    int n;
    while ((n = in.read(buf)) > 0) {
      out.write(buf, 0, n);
    }
  }

  private static byte[] readAll(InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    copy(in, out);
    return out.toByteArray();
  }
}
