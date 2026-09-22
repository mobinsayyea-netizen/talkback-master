package com.google.android.accessibility.talkback.soundtheme;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;
import androidx.annotation.Nullable;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.utils.output.FeedbackController;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Sound themes: a "Default" theme (its sounds are the app's own built-in earcons, and it can never
 * be deleted or exported) plus any themes the user adds. Every theme can have some of its slots
 * individually customized. Whichever theme is marked active is played; a slot the active theme
 * hasn't set falls back to the Default theme's own setting for that slot, and if Default hasn't set
 * it either, the app's built-in sound plays. Implements {@link FeedbackController.SoundOverrideProvider}
 * so {@link FeedbackController} can ask "what should actually play for this resource id".
 */
public final class SoundThemeManager implements FeedbackController.SoundOverrideProvider {

  private static final String TAG = "SoundThemeManager";
  public static final String DEFAULT_THEME_ID = "default";
  private static final String ROOT_DIR = "sound_themes";
  private static final String THEMES_FILE = "themes.json";
  private static final String ASSIGNMENTS_FILE = "assignments.json";
  private static final String POOL_DIR = "pool";
  private static final String PREFS_NAME = "sound_theme_prefs";
  private static final String PREF_ACTIVE_THEME = "active_theme_id";

  /** One sound theme: the built-in Default, or one the user added. */
  public static final class Theme {
    public final String id;
    public final String name;
    public final boolean builtin;

    Theme(String id, String name, boolean builtin) {
      this.id = id;
      this.name = name;
      this.builtin = builtin;
    }
  }

  private final Context appContext;
  private final Map<Integer, SoundSlot> slotsByResId = new LinkedHashMap<>();
  private final Map<String, SoundSlot> slotsByKey = new LinkedHashMap<>();

  public SoundThemeManager(Context context) {
    this.appContext = context.getApplicationContext();
    for (SoundSlot slot : buildSlots()) {
      slotsByResId.put(slot.defaultResId, slot);
      slotsByKey.put(slot.key, slot);
    }
    ensureDefaultThemeExists();
  }

  private static List<SoundSlot> buildSlots() {
    List<SoundSlot> slots = new ArrayList<>();
    slots.add(new SoundSlot("click", "Click", R.raw.tick));
    slots.add(new SoundSlot("long_press", "Long press", R.raw.long_clicked));
    slots.add(new SoundSlot("focus", "Focus", R.raw.focus));
    slots.add(new SoundSlot("focus_actionable", "Focus on a clickable item", R.raw.focus_actionable));
    slots.add(new SoundSlot("scroll", "Scroll", R.raw.scroll_tone));
    slots.add(new SoundSlot("window_changed", "Window changed", R.raw.view_entered));
    slots.add(new SoundSlot("complete", "Action complete", R.raw.complete));
    slots.add(new SoundSlot("gesture_begin", "Gesture started", R.raw.gesture_begin));
    slots.add(new SoundSlot("gesture_end", "Gesture finished", R.raw.gesture_end));
    slots.add(new SoundSlot("chime_up", "Chime up", R.raw.chime_up));
    slots.add(new SoundSlot("chime_down", "Chime down", R.raw.chime_down));
    slots.add(new SoundSlot("screen_on", "Screen on", R.raw.screen_on));
    slots.add(new SoundSlot("screen_off", "Screen off", R.raw.screen_off));
    slots.add(new SoundSlot("typo", "Spelling mistake", R.raw.typo));
    slots.add(new SoundSlot("clipboard", "Clipboard", R.raw.clipboard));
    slots.add(new SoundSlot("keyboard_focus", "Keyboard key", R.raw.keyboard_focus));
    slots.add(new SoundSlot("loading", "Loading", R.raw.loading));
    slots.add(new SoundSlot("volume_beep", "Volume change", R.raw.volume_beep));
    slots.add(new SoundSlot("power_connected", "Charger connected", R.raw.power_connected));
    slots.add(new SoundSlot("power_full", "Battery full", R.raw.power_full));
    slots.add(new SoundSlot("browse_on", "Browse mode on", R.raw.browse_mode_on_v4_2));
    slots.add(new SoundSlot("browse_off", "Browse mode off", R.raw.browse_mode_off_v4_2));
    return slots;
  }

  public List<SoundSlot> getSlots() {
    return new ArrayList<>(slotsByKey.values());
  }

  // ------------------------------------------------------------------------------------------
  // FeedbackController.SoundOverrideProvider

  @Override
  public @Nullable String getOverridePath(int resId) {
    SoundSlot slot = slotsByResId.get(resId);
    if (slot == null) {
      return null;
    }
    String activeId = getActiveThemeId();
    String path = poolPathForAssignment(activeId, slot.key);
    if (path != null) {
      return path;
    }
    if (!DEFAULT_THEME_ID.equals(activeId)) {
      return poolPathForAssignment(DEFAULT_THEME_ID, slot.key);
    }
    return null;
  }

  private @Nullable String poolPathForAssignment(String themeId, String slotKey) {
    Map<String, String> assignments = readAssignments(themeId);
    String fileName = assignments.get(slotKey);
    if (fileName == null) {
      return null;
    }
    File file = new File(poolDir(themeId), fileName);
    return file.exists() ? file.getAbsolutePath() : null;
  }

  // ------------------------------------------------------------------------------------------
  // Themes

  public List<Theme> getThemes() {
    List<Theme> themes = new ArrayList<>();
    try {
      JSONArray array = new JSONArray(readTextFile(themesFile(), "[]"));
      for (int i = 0; i < array.length(); i++) {
        JSONObject o = array.getJSONObject(i);
        themes.add(new Theme(o.getString("id"), o.getString("name"), o.optBoolean("builtin", false)));
      }
    } catch (JSONException e) {
      Log.w(TAG, "Could not read sound themes", e);
    }
    return themes;
  }

  private void ensureDefaultThemeExists() {
    for (Theme theme : getThemes()) {
      if (DEFAULT_THEME_ID.equals(theme.id)) {
        return;
      }
    }
    List<Theme> themes = new ArrayList<>(getThemes());
    themes.add(0, new Theme(DEFAULT_THEME_ID, "Default", /* builtin= */ true));
    saveThemes(themes);
  }

  /** Adds a new theme with all files copied out of {@code folderUri} into its own pool. Returns
   *  the new theme's id, or null if nothing usable was found in the folder. */
  public @Nullable String addThemeFromFolder(Uri folderUri, String themeName) {
    ContentResolver resolver = appContext.getContentResolver();
    Uri childrenUri =
        DocumentsContract.buildChildDocumentsUriUsingTree(
            folderUri, DocumentsContract.getTreeDocumentId(folderUri));
    String themeId = "theme_" + System.currentTimeMillis();
    File pool = poolDir(themeId);
    if (!pool.mkdirs() && !pool.isDirectory()) {
      return null;
    }
    int copied = 0;
    try (android.database.Cursor cursor =
        resolver.query(
            childrenUri,
            new String[] {
              DocumentsContract.Document.COLUMN_DOCUMENT_ID,
              DocumentsContract.Document.COLUMN_DISPLAY_NAME,
              DocumentsContract.Document.COLUMN_MIME_TYPE
            },
            null,
            null,
            null)) {
      if (cursor != null) {
        while (cursor.moveToNext()) {
          String docId = cursor.getString(0);
          String displayName = cursor.getString(1);
          String mime = cursor.getString(2);
          if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime) || displayName == null) {
            continue;
          }
          Uri fileUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, docId);
          File dest = uniqueFile(pool, sanitizeFileName(displayName));
          if (copyUriToFile(resolver, fileUri, dest)) {
            copied++;
          }
        }
      }
    }
    if (copied == 0) {
      deleteRecursive(pool);
      return null;
    }
    List<Theme> themes = new ArrayList<>(getThemes());
    themes.add(new Theme(themeId, themeName, /* builtin= */ false));
    saveThemes(themes);
    return themeId;
  }

  /** Deletes an added (non-Default) theme and all its files. */
  public void deleteTheme(String themeId) {
    if (DEFAULT_THEME_ID.equals(themeId)) {
      return;
    }
    List<Theme> themes = new ArrayList<>();
    for (Theme theme : getThemes()) {
      if (!theme.id.equals(themeId)) {
        themes.add(theme);
      }
    }
    saveThemes(themes);
    deleteRecursive(themeDir(themeId));
    if (themeId.equals(getActiveThemeId())) {
      setActiveThemeId(DEFAULT_THEME_ID);
    }
  }

  public String getActiveThemeId() {
    return prefs().getString(PREF_ACTIVE_THEME, DEFAULT_THEME_ID);
  }

  public void setActiveThemeId(String themeId) {
    prefs().edit().putString(PREF_ACTIVE_THEME, themeId).apply();
  }

  private void saveThemes(List<Theme> themes) {
    try {
      JSONArray array = new JSONArray();
      for (Theme theme : themes) {
        JSONObject o = new JSONObject();
        o.put("id", theme.id);
        o.put("name", theme.name);
        o.put("builtin", theme.builtin);
        array.put(o);
      }
      writeTextFile(themesFile(), array.toString());
    } catch (JSONException e) {
      Log.w(TAG, "Could not save sound themes", e);
    }
  }

  // ------------------------------------------------------------------------------------------
  // Pool files and per-slot assignment within one theme

  /** Files available to assign inside this theme (uploaded files, plus the app's own sounds for
   *  Default). */
  public List<String> getPoolFileNames(String themeId) {
    File dir = poolDir(themeId);
    List<String> names = new ArrayList<>();
    File[] files = dir.listFiles();
    if (files != null) {
      for (File f : files) {
        if (f.isFile()) {
          names.add(f.getName());
        }
      }
    }
    return names;
  }

  public Map<String, String> readAssignments(String themeId) {
    Map<String, String> map = new LinkedHashMap<>();
    try {
      JSONObject o = new JSONObject(readTextFile(assignmentsFile(themeId), "{}"));
      java.util.Iterator<String> keys = o.keys();
      while (keys.hasNext()) {
        String k = keys.next();
        map.put(k, o.getString(k));
      }
    } catch (JSONException e) {
      Log.w(TAG, "Could not read sound-theme assignments", e);
    }
    return map;
  }

  /** Sets which pool file plays for {@code slotKey} in this theme (the file must already be in
   *  that theme's pool). */
  public void assign(String themeId, String slotKey, String poolFileName) {
    Map<String, String> assignments = readAssignments(themeId);
    assignments.put(slotKey, poolFileName);
    saveAssignments(themeId, assignments);
  }

  /** Removes any custom sound for this slot in this theme, so it falls back again. */
  public void clearAssignment(String themeId, String slotKey) {
    Map<String, String> assignments = readAssignments(themeId);
    if (assignments.remove(slotKey) != null) {
      saveAssignments(themeId, assignments);
    }
  }

  private void saveAssignments(String themeId, Map<String, String> assignments) {
    try {
      JSONObject o = new JSONObject();
      for (Map.Entry<String, String> e : assignments.entrySet()) {
        o.put(e.getKey(), e.getValue());
      }
      writeTextFile(assignmentsFile(themeId), o.toString());
    } catch (JSONException e) {
      Log.w(TAG, "Could not save sound-theme assignments", e);
    }
  }

  /** Adds one more file to a theme's pool from an arbitrary content Uri (a single file pick). */
  public @Nullable String addPoolFile(String themeId, Uri fileUri, String suggestedName) {
    File pool = poolDir(themeId);
    if (!pool.mkdirs() && !pool.isDirectory()) {
      return null;
    }
    File dest = uniqueFile(pool, sanitizeFileName(suggestedName));
    boolean ok = copyUriToFile(appContext.getContentResolver(), fileUri, dest);
    return ok ? dest.getName() : null;
  }

  // ------------------------------------------------------------------------------------------
  // Paths and small file helpers

  private File rootDir() {
    return new File(appContext.getFilesDir(), ROOT_DIR);
  }

  private File themeDir(String themeId) {
    return new File(rootDir(), themeId);
  }

  private File poolDir(String themeId) {
    return new File(themeDir(themeId), POOL_DIR);
  }

  private File themesFile() {
    return new File(rootDir(), THEMES_FILE);
  }

  private File assignmentsFile(String themeId) {
    return new File(themeDir(themeId), ASSIGNMENTS_FILE);
  }

  private SharedPreferences prefs() {
    return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
  }

  private static String readTextFile(File file, String defaultValue) {
    if (!file.exists()) {
      return defaultValue;
    }
    try (InputStream in = new FileInputStream(file)) {
      java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
      byte[] chunk = new byte[8192];
      int n;
      while ((n = in.read(chunk)) > 0) {
        buffer.write(chunk, 0, n);
      }
      return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      Log.w(TAG, "Could not read " + file, e);
      return defaultValue;
    }
  }

  private static void writeTextFile(File file, String content) {
    File parent = file.getParentFile();
    if (parent != null) {
      parent.mkdirs();
    }
    File tmp = new File(parent, file.getName() + ".tmp");
    try (OutputStream out = new FileOutputStream(tmp)) {
      out.write(content.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      Log.w(TAG, "Could not write " + file, e);
      return;
    }
    if (!tmp.renameTo(file)) {
      Log.w(TAG, "Could not replace " + file);
    }
  }

  private static boolean copyUriToFile(ContentResolver resolver, Uri uri, File dest) {
    try (InputStream in = resolver.openInputStream(uri);
        OutputStream out = new FileOutputStream(dest)) {
      if (in == null) {
        return false;
      }
      byte[] chunk = new byte[8192];
      int n;
      while ((n = in.read(chunk)) > 0) {
        out.write(chunk, 0, n);
      }
      return true;
    } catch (IOException e) {
      Log.w(TAG, "Could not copy sound file", e);
      return false;
    }
  }

  private static File uniqueFile(File dir, String name) {
    File file = new File(dir, name);
    if (!file.exists()) {
      return file;
    }
    String base = name;
    String ext = "";
    int dot = name.lastIndexOf('.');
    if (dot > 0) {
      base = name.substring(0, dot);
      ext = name.substring(dot);
    }
    int i = 1;
    File candidate;
    do {
      candidate = new File(dir, base + "_" + i + ext);
      i++;
    } while (candidate.exists());
    return candidate;
  }

  private static String sanitizeFileName(String name) {
    String cleaned = name.replaceAll("[/\\\\]", "_").trim();
    return cleaned.isEmpty() ? ("sound_" + System.currentTimeMillis()) : cleaned;
  }

  private static void deleteRecursive(File file) {
    if (file.isDirectory()) {
      File[] children = file.listFiles();
      if (children != null) {
        for (File child : children) {
          deleteRecursive(child);
        }
      }
    }
    file.delete();
  }
}
