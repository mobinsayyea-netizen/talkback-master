package com.google.android.accessibility.talkback.backup;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import com.google.android.accessibility.talkback.MsNodeAlias;
import com.google.android.accessibility.talkback.clipboard.ClipboardStore;
import com.google.android.accessibility.talkback.editor.LuaPaths;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.talkback.soundtheme.SoundSlot;
import com.google.android.accessibility.talkback.soundtheme.SoundThemeManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.regex.Pattern;
import org.json.JSONObject;

/**
 * Restores a Jieshuo backup folder (zipped) into Advanced Screen Reader. This step covers sound packages
 * (each becomes a sound theme with the same file for the same occasion) and the clipboard history.
 *
 * <p>The zip is read from the place the person picks; nothing from it is bundled in the app. Keys,
 * tokens and passwords are never read.
 */
public final class JieshuoRestore {

  private static final String TAG = "JieshuoRestore";
  private static final long MAX_ENTRY_BYTES = 200L * 1024 * 1024;
  // "\u5907\u4efd" is the Chinese word for backup; "\u526a\u5207\u677f" is clipboard.
  private static final String CLIPBOARD_ZIP_NAME = "\u526a\u5207\u677f.dat";
  private static final String CLIPBOARD_FILE = "staged_clipboard.dat"; // name used on disk
  // "\u8bbe\u7f6e" is the Chinese word for settings (the file that says which sound package is on).
  private static final String SETTINGS_ZIP_NAME = "\u8bbe\u7f6e.dat";
  private static final String SETTINGS_FILE = "staged_settings.dat";
  private static final String SOUNDS_DIR = "Sounds";
  // "\u6536\u85cf\u5939" is the Chinese word for favorites folder.
  private static final String FAVORITES_ZIP_NAME = "\u6536\u85cf\u5939.dat";
  private static final String FAVORITES_FILE = "staged_favorites.dat";
  /** Top-level Jieshuo folders that are taken out of the zip. */
  private static final Set<String> TOP_FOLDERS =
      new HashSet<>(
          Arrays.asList(
              SOUNDS_DIR, "Labels", "Aliases", "Plugins", "Tools", "Resources", "Notes", "Timer", "Gestures"));
  /** Loose files in the Jieshuo folder that are copied. The Gemini key file is NOT in this list. */
  private static final Set<String> TOP_FILES =
      new HashSet<>(Arrays.asList("GeminiUI_Logic.lua", "GeminiLangConfig.txt"));

  // Things that look like keys or tokens. Such favorites are never brought in.
  private static final Pattern SECRET_PRECISE =
      Pattern.compile(
          "ghp_[A-Za-z0-9]{20,}|gho_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,}"
              + "|AIza[0-9A-Za-z_\\-]{20,}|sk-[A-Za-z0-9_\\-]{20,}|AKIA[0-9A-Z]{12,}"
              + "|-----BEGIN [A-Z ]*PRIVATE KEY");
  private static final Pattern SECRET_WORDS =
      Pattern.compile("(?i)apikey|api_key|authdomain|password|passwd|secret|token|bearer ");

  /** Jieshuo sound names that have a different name in Advanced Screen Reader. */
  private static final Map<String, String> SLOT_RENAMES = new HashMap<>();

  static {
    SLOT_RENAMES.put("focus1", "focus");
    SLOT_RENAMES.put("focus2", "focus_actionable");
    SLOT_RENAMES.put("focus3", "keyboard_focus");
    SLOT_RENAMES.put("long_click", "long_press");
    SLOT_RENAMES.put("scroll_up", "chime_up");
    SLOT_RENAMES.put("scroll_down", "chime_down");
    SLOT_RENAMES.put("gesture_start", "gesture_begin");
    SLOT_RENAMES.put("has_link", "hyperlink");
  }

  /** What happened, in plain words for the result screen. */
  public static final class Result {
    public int themesCreated;
    public int soundsAssigned;
    public int clockFiles;
    public int clipboardAdded;
    public int aliasesAdded;
    public int labelAppsDone;
    public int labelsSilentSkipped;
    public int labelsPathSkipped;
    public int extensionsCopied;
    public int toolsCopied;
    public int filesCopied;
    public int alreadyThere;
    public int favoritesAdded;
    public int favoritesSecretSkipped;
    public int settingsApplied;
    public int settingsRejected;
    public int settingsNoMatch;
    public int settingsPrivate;
    public int gesturesGlobal;
    public int gesturesPerApp;
    public int gesturesMultiPart;
    public int gesturesSkipped;
    public final List<String> settingsRejectedShown = new ArrayList<>();
    public final List<String> gesturesSkippedShown = new ArrayList<>();
    public String activeTheme = "";
    public final List<String> notes = new ArrayList<>();

    public String toText() {
      StringBuilder sb = new StringBuilder();
      sb.append("Sound themes created: ").append(themesCreated).append('\n');
      if (!activeTheme.isEmpty()) {
        sb.append("Active sound theme: ").append(activeTheme).append('\n');
      }
      sb.append("Sounds assigned to events: ").append(soundsAssigned).append('\n');
      sb.append("Spoken clock files saved: ").append(clockFiles).append('\n');
      sb.append("Clipboard items restored: ").append(clipboardAdded).append('\n');
      sb.append("Favorites restored: ").append(favoritesAdded);
      if (favoritesSecretSkipped > 0) {
        sb.append(" (").append(favoritesSecretSkipped).append(" with keys or passwords left out)");
      }
      sb.append('\n');
      sb.append("Label apps restored: ").append(labelAppsDone).append(", labels added: ")
          .append(aliasesAdded).append('\n');
      sb.append("Extensions: ").append(extensionsCopied).append(", tools: ").append(toolsCopied)
          .append(", other files: ").append(filesCopied).append('\n');
      sb.append("Settings restored: ").append(settingsApplied);
      if (settingsRejected > 0) {
        sb.append(", not restored because the value does not fit: ").append(settingsRejected);
      }
      sb.append(", no matching setting in MS: ").append(settingsNoMatch);
      if (settingsPrivate > 0) {
        sb.append(", private (keys, tokens, accounts) never read: ").append(settingsPrivate);
      }
      sb.append('\n');
      for (String r : settingsRejectedShown) {
        sb.append("  - ").append(r).append('\n');
      }
      sb.append("Gestures: ").append(gesturesGlobal).append(" global, ").append(gesturesPerApp)
          .append(" in app schemes, ").append(gesturesMultiPart).append(" multi-part, ")
          .append(gesturesSkipped).append(" left out\n");
      for (String g : gesturesSkippedShown) {
        sb.append("  - ").append(g).append('\n');
      }
      if (alreadyThere > 0) {
        sb.append(alreadyThere).append(" items were already here and were kept as they are.\n");
      }
      for (String n : notes) {
        sb.append('\n').append(n);
      }
      return sb.toString();
    }
  }

  private JieshuoRestore() {}

  /** Runs the whole restore. Call from a background thread. */
  public static Result restore(Context context, Uri zipUri) throws IOException {
    Result result = new Result();
    File work = new File(context.getCacheDir(), "jx_restore_" + System.nanoTime());
    if (!work.mkdirs()) {
      throw new IOException("Could not make a work folder");
    }
    try {
      extract(context.getContentResolver(), zipUri, work);
      restoreSounds(context, new File(work, SOUNDS_DIR), result);
      List<String> favorites = readFavorites(new File(work, FAVORITES_FILE), result);
      markFavorites(context, favorites, result); // first, so clipboard items end up above them
      restoreClipboard(context, new File(work, CLIPBOARD_FILE), result);
      restoreLabels(context, new File(work, "Labels"), new File(work, "Aliases"), result);
      restoreLua(context, work, result);
      JieshuoSettingsRestore.restore(context, work, new File(work, SETTINGS_FILE), result);
      if (result.themesCreated == 0
          && result.clipboardAdded == 0
          && result.favoritesAdded == 0
          && result.aliasesAdded == 0
          && result.extensionsCopied == 0
          && result.toolsCopied == 0
          && result.settingsApplied == 0
          && result.gesturesGlobal == 0) {
        result.notes.add("Nothing usable was found. Is this a Jieshuo backup zip?");
      }
    } finally {
      deleteRecursive(work);
    }
    return result;
  }

  // ------------------------------------------------------------------------------------------
  // Reading the zip: only the Sounds folder and the clipboard file are taken out.

  private static void extract(ContentResolver resolver, Uri zipUri, File work) throws IOException {
    try (InputStream raw = resolver.openInputStream(zipUri)) {
      if (raw == null) {
        throw new IOException("Could not open the file");
      }
      ZipInputStream zin = new ZipInputStream(raw, StandardCharsets.UTF_8);
      ZipEntry entry;
      byte[] chunk = new byte[16384];
      while ((entry = zin.getNextEntry()) != null) {
        if (entry.isDirectory()) {
          continue;
        }
        String rel = relativeTarget(entry.getName());
        if (rel == null) {
          continue;
        }
        File dest = new File(work, rel);
        if (!dest.getCanonicalPath().startsWith(work.getCanonicalPath() + File.separator)) {
          continue; // path trick, skip
        }
        File parent = dest.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
          continue;
        }
        long total = 0;
        try (OutputStream out = new FileOutputStream(dest)) {
          int n;
          while ((n = zin.read(chunk)) > 0) {
            total += n;
            if (total > MAX_ENTRY_BYTES) {
              throw new IOException("A file in the zip is too large");
            }
            out.write(chunk, 0, n);
          }
        }
      }
    }
  }

  /** Maps a zip entry to a path inside the work folder, or null if it is not needed. */
  private static String relativeTarget(String entryName) {
    String[] parts = entryName.replace('\\', '/').split("/");
    for (String p : parts) {
      if (p.equals("..")) {
        return null;
      }
    }
    String last = parts[parts.length - 1];
    if (last.equals("history.json")) {
      return null; // recent-files list of Jieshuo, not needed
    }
    // The Jieshuo folder may or may not be the first folder in the zip.
    for (int i = 0; i < parts.length && i <= 1; i++) {
      if (TOP_FOLDERS.contains(parts[i]) && i + 1 < parts.length) {
        return join(parts, i);
      }
    }
    if (parts.length <= 2 && TOP_FILES.contains(last)) {
      return last;
    }
    // Stored under plain ASCII names, so the phone's file system never has to handle the Chinese names.
    if (last.equals(CLIPBOARD_ZIP_NAME)) {
      return CLIPBOARD_FILE;
    }
    if (last.equals(SETTINGS_ZIP_NAME)) {
      return SETTINGS_FILE;
    }
    if (last.equals(FAVORITES_ZIP_NAME)) {
      return FAVORITES_FILE;
    }
    return null;
  }

  private static String join(String[] parts, int from) {
    StringBuilder sb = new StringBuilder();
    for (int i = from; i < parts.length; i++) {
      if (sb.length() > 0) {
        sb.append('/');
      }
      sb.append(parts[i]);
    }
    return sb.toString();
  }

  // ------------------------------------------------------------------------------------------
  // Sounds

  private static void restoreSounds(Context context, File soundsDir, Result result) {
    File[] packages = soundsDir.listFiles();
    if (packages == null) {
      return;
    }
    Arrays.sort(packages);
    SoundThemeManager manager = new SoundThemeManager(context);
    Set<String> knownSlots = new HashSet<>();
    for (SoundSlot slot : manager.getSlots()) {
      knownSlots.add(slot.key);
    }
    String activeJx = readActivePackage(new File(soundsDir.getParentFile(), SETTINGS_FILE), result);
    for (File pkg : packages) {
      if (!pkg.isDirectory()) {
        continue;
      }
      String themeName = pkg.getName().trim();
      String themeId = manager.createEmptyTheme(themeName);
      if (themeId == null) {
        result.notes.add("Could not create the sound theme \"" + themeName + "\".");
        continue;
      }
      result.themesCreated++;
      Map<String, String> poolNames = new HashMap<>();
      File[] files = pkg.listFiles();
      if (files != null) {
        Arrays.sort(files);
        for (File f : files) {
          if (f.isFile() && !f.getName().equals("config") && isAudio(f.getName())) {
            String pooled = manager.addPoolFileFromFile(themeId, f);
            if (pooled != null) {
              poolNames.put(f.getName(), pooled);
            }
          }
        }
      }
      assignFromConfig(manager, themeId, themeName, new File(pkg, "config"), poolNames,
          knownSlots, result);
      File clock = new File(pkg, "Clock");
      if (clock.isDirectory()) {
        result.clockFiles +=
            copyTree(clock, new File(manager.themeFolder(themeId), "Clock"));
      }
      if (activeJx != null && activeJx.trim().equals(themeName)) {
        manager.setActiveThemeId(themeId);
        result.activeTheme = themeName;
      }
    }
    if (result.activeTheme.isEmpty() && result.themesCreated > 0) {
      result.notes.add(
          "No sound theme was switched on, because the backup does not say which one you used. "
              + "Choose it in the sound theme settings.");
    }
  }

  private static void assignFromConfig(
      SoundThemeManager manager,
      String themeId,
      String themeName,
      File config,
      Map<String, String> poolNames,
      Set<String> knownSlots,
      Result result) {
    if (!config.isFile()) {
      result.notes.add("\"" + themeName + "\" has no config file; its sounds were copied but not assigned.");
      return;
    }
    List<String> noSlot = new ArrayList<>();
    List<String> missing = new ArrayList<>();
    try {
      JSONObject o = new JSONObject(readText(config));
      Iterator<String> keys = o.keys();
      while (keys.hasNext()) {
        String key = keys.next();
        String value = o.optString(key, "");
        if (value.isEmpty() || value.equals("value_default")) {
          continue; // Jieshuo's own built-in sound; MS keeps its default for this one
        }
        String slot = SLOT_RENAMES.containsKey(key) ? SLOT_RENAMES.get(key) : key;
        if (!knownSlots.contains(slot)) {
          noSlot.add(key);
          continue;
        }
        String pooled = poolNames.get(value);
        if (pooled == null) {
          missing.add(key + " (" + value + ")");
          continue;
        }
        manager.assign(themeId, slot, pooled);
        result.soundsAssigned++;
      }
    } catch (Exception e) {
      Log.w(TAG, "Could not read sound config", e);
      com.google.android.accessibility.talkback.MsErrorLog.log("Restore: " + "Could not read sound config", e);
      result.notes.add("Could not read the sound config of \"" + themeName + "\".");
      return;
    }
    if (!noSlot.isEmpty()) {
      result.notes.add(
          "\"" + themeName + "\": no matching event in Advanced Screen Reader for: " + String.join(", ", noSlot));
    }
    if (!missing.isEmpty()) {
      result.notes.add(
          "\"" + themeName + "\": sound file missing in the backup for: " + String.join(", ", missing));
    }
  }

  /** The sound package that was switched on in Jieshuo's settings, or null. Only this one value
   *  is read from the settings file; nothing else in it is used here. */
  private static String readActivePackage(File settingsDat, Result result) {
    if (!settingsDat.isFile()) {
      return null;
    }
    try (ObjectInputStream in = new SafeObjectInputStream(new FileInputStream(settingsDat))) {
      Object o = in.readObject();
      if (o instanceof Map) {
        Object v = ((Map<?, ?>) o).get("sound_package");
        if (v instanceof String) {
          return (String) v;
        }
      }
    } catch (Exception e) {
      Log.w(TAG, "Could not read settings file", e);
      com.google.android.accessibility.talkback.MsErrorLog.log("Restore: " + "Could not read settings file", e);
      result.notes.add("Could not read which sound theme you had on.");
    }
    return null;
  }

  private static boolean isAudio(String name) {
    String n = name.toLowerCase(java.util.Locale.ROOT);
    return n.endsWith(".mp3") || n.endsWith(".ogg") || n.endsWith(".wav") || n.endsWith(".m4a")
        || n.endsWith(".mid") || n.endsWith(".aac") || n.endsWith(".flac");
  }

  private static int copyTree(File from, File to) {
    return copyTree(from, to, true);
  }

  /** Copies a folder. With overwrite false, files that already exist are left alone. */
  private static int copyTree(File from, File to, boolean overwrite) {
    int copied = 0;
    File[] kids = from.listFiles();
    if (kids == null) {
      return 0;
    }
    if (!to.isDirectory() && !to.mkdirs()) {
      return 0;
    }
    for (File k : kids) {
      File dest = new File(to, k.getName());
      if (k.isDirectory()) {
        copied += copyTree(k, dest, overwrite);
      } else if (!overwrite && dest.exists()) {
        continue;
      } else {
        try (InputStream in = new FileInputStream(k);
            OutputStream out = new FileOutputStream(dest)) {
          byte[] chunk = new byte[16384];
          int n;
          while ((n = in.read(chunk)) > 0) {
            out.write(chunk, 0, n);
          }
          copied++;
        } catch (IOException e) {
          Log.w(TAG, "Could not copy " + k.getName(), e);
          com.google.android.accessibility.talkback.MsErrorLog.log("Restore: " + "Could not copy " + k.getName(), e);
        }
      }
    }
    return copied;
  }

  // ------------------------------------------------------------------------------------------
  // Clipboard

  private static void restoreClipboard(Context context, File dat, Result result) {
    if (!dat.isFile()) {
      return;
    }
    try (ObjectInputStream in = new SafeObjectInputStream(new FileInputStream(dat))) {
      Object o = in.readObject();
      if (!(o instanceof List)) {
        result.notes.add("The clipboard file has an unexpected format; skipped.");
        return;
      }
      List<?> items = (List<?>) o;
      // The first item of Jieshuo's list stays on top: add from the end to the start.
      for (int i = items.size() - 1; i >= 0; i--) {
        Object item = items.get(i);
        if (item instanceof String && !((String) item).trim().isEmpty()) {
          ClipboardStore.add(context, (String) item);
          result.clipboardAdded++;
        }
      }
    } catch (Exception e) {
      Log.w(TAG, "Could not read clipboard", e);
      com.google.android.accessibility.talkback.MsErrorLog.log("Restore: " + "Could not read clipboard", e);
      result.notes.add("Could not read the clipboard file; skipped.");
    }
  }

  /** Reads only plain lists, maps and numbers, so a bad file cannot run anything. */
  static final class SafeObjectInputStream extends ObjectInputStream {
    private static final Set<String> ALLOWED =
        new HashSet<>(
            Arrays.asList(
                "java.util.ArrayList",
                "java.util.HashMap",
                "java.util.HashSet",
                "java.util.LinkedHashMap",
                "java.util.LinkedHashSet",
                "java.lang.Boolean",
                "java.lang.Integer",
                "java.lang.Long",
                "java.lang.Float",
                "java.lang.Double",
                "java.lang.Number"));

    SafeObjectInputStream(InputStream in) throws IOException {
      super(in);
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc)
        throws IOException, ClassNotFoundException {
      if (!ALLOWED.contains(desc.getName())) {
        throw new InvalidClassException("Not allowed", desc.getName());
      }
      return super.resolveClass(desc);
    }
  }

  // ------------------------------------------------------------------------------------------
  // Favorites (clipboard items marked as favorite). Anything that looks like a key, token or
  // password is left out.

  private static List<String> readFavorites(File dat, Result result) {
    List<String> out = new ArrayList<>();
    if (!dat.isFile()) {
      return out;
    }
    try (ObjectInputStream in = new SafeObjectInputStream(new FileInputStream(dat))) {
      Object o = in.readObject();
      if (!(o instanceof List)) {
        return out;
      }
      for (Object item : (List<?>) o) {
        if (!(item instanceof String) || ((String) item).trim().isEmpty()) {
          continue;
        }
        String text = (String) item;
        if (looksSecret(text)) {
          result.favoritesSecretSkipped++;
        } else {
          out.add(text);
        }
      }
    } catch (Exception e) {
      Log.w(TAG, "Could not read favorites", e);
      com.google.android.accessibility.talkback.MsErrorLog.log("Restore: " + "Could not read favorites", e);
      result.notes.add("Could not read the favorites file; skipped.");
    }
    return out;
  }

  private static boolean looksSecret(String text) {
    if (SECRET_PRECISE.matcher(text).find()) {
      return true;
    }
    boolean noSpace = true;
    for (int i = 0; i < text.length(); i++) {
      if (Character.isWhitespace(text.charAt(i))) {
        noSpace = false;
        break;
      }
    }
    if (noSpace && text.length() >= 20) {
      boolean letter = false;
      boolean digit = false;
      for (int i = 0; i < text.length(); i++) {
        char c = text.charAt(i);
        letter |= Character.isLetter(c);
        digit |= Character.isDigit(c);
      }
      if (letter && digit) {
        return true; // one long word with letters and numbers: probably a key
      }
    }
    return text.length() <= 600 && SECRET_WORDS.matcher(text).find();
  }

  private static void markFavorites(Context context, List<String> favorites, Result result) {
    if (favorites.isEmpty()) {
      return;
    }
    for (int i = favorites.size() - 1; i >= 0; i--) {
      ClipboardStore.add(context, favorites.get(i));
    }
    Set<String> texts = new HashSet<>(favorites);
    List<Long> ids = new ArrayList<>();
    for (ClipboardStore.Entry e : ClipboardStore.all(context)) {
      if (texts.contains(e.text)) {
        ids.add(e.id);
      }
    }
    ClipboardStore.setFavorite(context, ids, true);
    result.favoritesAdded = ids.size();
  }

  // ------------------------------------------------------------------------------------------
  // Labels: Jieshuo keeps one file per app (named by the app's name) with "view id -> spoken
  // name". Advanced Screen Reader has one list of "full view id = spoken name", so the app name is
  // turned into the package name of the installed app with that name.

  private static void restoreLabels(Context context, File labelsDir, File aliasesDir, Result result) {
    Map<String, String> found = new LinkedHashMap<>();
    File[] files = labelsDir.listFiles();
    if (files != null) {
      Arrays.sort(files);
      Map<String, List<String>> packagesByName = installedAppsByName(context);
      Map<String, File> baseFiles = new LinkedHashMap<>();
      Map<String, File> versionedOnly = new LinkedHashMap<>();
      Set<String> baseNames = new HashSet<>();
      for (File f : files) {
        if (f.isFile() && f.getName().endsWith(".json")) {
          String name = f.getName().substring(0, f.getName().length() - 5);
          if (appNameOfVersioned(name) == null) {
            baseFiles.put(name, f);
            baseNames.add(name);
          }
        }
      }
      for (File f : files) {
        if (f.isFile() && f.getName().endsWith(".json")) {
          String name = f.getName().substring(0, f.getName().length() - 5);
          String app = appNameOfVersioned(name);
          if (app != null && !baseNames.contains(app) && !versionedOnly.containsKey(app)) {
            versionedOnly.put(app, f); // an app that has only a version-named file
          }
        }
      }
      baseFiles.putAll(versionedOnly);
      List<String> notInstalled = new ArrayList<>();
      for (Map.Entry<String, File> e : baseFiles.entrySet()) {
        String app = e.getKey().trim();
        List<String> packages = packagesByName.get(app.toLowerCase(Locale.ROOT));
        if (packages == null) {
          notInstalled.add(app);
          continue;
        }
        JSONObject o;
        try {
          o = new JSONObject(readText(e.getValue()));
        } catch (Exception ex) {
          Log.w(TAG, "Bad label file " + app, ex);
          com.google.android.accessibility.talkback.MsErrorLog.log("Restore: " + "Bad label file " + app, ex);
          continue;
        }
        int before = found.size();
        Iterator<String> keys = o.keys();
        while (keys.hasNext()) {
          String key = keys.next();
          String value = o.optString(key, "").replace('\n', ' ').trim();
          if (value.isEmpty()) {
            result.labelsSilentSkipped++; // Jieshuo's "say nothing" marker
          } else if (key.indexOf(':') >= 0 || key.indexOf(';') >= 0 || key.indexOf('=') >= 0
              || key.trim().isEmpty()) {
            result.labelsPathSkipped++; // position-based key, MS cannot match it
          } else {
            for (String pkg : packages) {
              found.put((pkg + ":id/" + key.trim()).toLowerCase(Locale.ROOT), value);
            }
          }
        }
        if (found.size() > before) {
          result.labelAppsDone++;
        }
      }
      if (!notInstalled.isEmpty()) {
        result.notes.add(
            notInstalled.size()
                + " apps in the label folder are not installed on this phone, so their labels were"
                + " left out: "
                + joinFirst(notInstalled, 12));
      }
    }
    // Aliases: "spoken text -> new name" per app. MS matches these on the element's text in every app.
    File[] aliasFiles = aliasesDir.listFiles();
    if (aliasFiles != null) {
      Arrays.sort(aliasFiles);
      for (File f : aliasFiles) {
        if (!f.isFile() || !f.getName().endsWith(".json")) {
          continue;
        }
        try {
          JSONObject o = new JSONObject(readText(f));
          Iterator<String> keys = o.keys();
          while (keys.hasNext()) {
            String key = keys.next();
            String value = o.optString(key, "").replace('\n', ' ').trim();
            String k = key.replace('\n', ' ').trim().toLowerCase(Locale.ROOT);
            if (!k.isEmpty() && k.indexOf('=') < 0 && !value.isEmpty()) {
              found.put(k, value);
            }
          }
        } catch (Exception ex) {
          Log.w(TAG, "Bad alias file " + f.getName(), ex);
          com.google.android.accessibility.talkback.MsErrorLog.log("Restore: " + "Bad alias file " + f.getName(), ex);
        }
      }
    }
    if (found.isEmpty()) {
      return;
    }
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    Map<String, String> merged = new LinkedHashMap<>();
    for (String line : prefs.getString(MsNodeAlias.K_TEXT, "").split("\\r?\\n")) {
      int eq = line.indexOf('=');
      if (eq > 0) {
        merged.put(line.substring(0, eq).trim().toLowerCase(Locale.ROOT), line.substring(eq + 1).trim());
      }
    }
    int added = 0;
    for (Map.Entry<String, String> e : found.entrySet()) {
      if (!merged.containsKey(e.getKey())) { // what is already in MS stays as it is
        merged.put(e.getKey(), e.getValue());
        added++;
      }
    }
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String> e : merged.entrySet()) {
      sb.append(e.getKey()).append('=').append(e.getValue()).append('\n');
    }
    prefs.edit().putString(MsNodeAlias.K_TEXT, sb.toString()).putBoolean(MsNodeAlias.K_USE, true).apply();
    result.aliasesAdded = added;
    result.notes.add("\"Use custom node aliases\" was switched on, so the restored labels are spoken.");
    if (result.labelsPathSkipped > 0 || result.labelsSilentSkipped > 0) {
      result.notes.add(
          "Left out: " + result.labelsPathSkipped + " labels that Jieshuo matched by screen position, and "
              + result.labelsSilentSkipped + " that told Jieshuo to say nothing.");
    }
  }

  /** "WhatsApp_2.24.8.85" -> "WhatsApp"; null if the name has no version part. */
  private static String appNameOfVersioned(String fileName) {
    int idx = fileName.lastIndexOf('_');
    if (idx > 0 && idx + 1 < fileName.length() && Character.isDigit(fileName.charAt(idx + 1))) {
      return fileName.substring(0, idx);
    }
    return null;
  }

  private static Map<String, List<String>> installedAppsByName(Context context) {
    Map<String, List<String>> map = new HashMap<>();
    PackageManager pm = context.getPackageManager();
    try {
      for (ApplicationInfo info : pm.getInstalledApplications(0)) {
        CharSequence label = pm.getApplicationLabel(info);
        if (label == null) {
          continue;
        }
        String key = label.toString().trim().toLowerCase(Locale.ROOT);
        List<String> list = map.get(key);
        if (list == null) {
          list = new ArrayList<>();
          map.put(key, list);
        }
        list.add(info.packageName);
      }
    } catch (Exception e) {
      Log.w(TAG, "Could not list installed apps", e);
      com.google.android.accessibility.talkback.MsErrorLog.log("Restore: " + "Could not list installed apps", e);
    }
    return map;
  }

  private static String joinFirst(List<String> names, int max) {
    List<String> shown = names.size() > max ? names.subList(0, max) : names;
    String text = String.join(", ", shown);
    return names.size() > max ? text + " and " + (names.size() - max) + " more" : text;
  }

  // ------------------------------------------------------------------------------------------
  // Lua extensions, tools and other files

  private static void restoreLua(Context context, File work, Result result) {
    File plugins = new File(work, "Plugins");
    if (plugins.isDirectory()) {
      result.extensionsCopied += copyFolders(plugins, LuaPaths.extensions(context), result);
    }
    File tools = new File(work, "Tools");
    if (tools.isDirectory()) {
      result.toolsCopied += copyFolders(tools, LuaPaths.tools(context), result);
    }
    File base = LuaPaths.base(context);
    for (String name : TOP_FILES) {
      File src = new File(work, name);
      if (src.isFile()) {
        result.filesCopied += copyFileIfMissing(src, base);
      }
    }
    File resources = new File(work, "Resources");
    if (resources.isDirectory()) {
      result.filesCopied += copyTree(resources, new File(base, "Resources"), false);
    }
    File notes = new File(work, "Notes");
    if (notes.isDirectory()) {
      result.filesCopied += copyTree(notes, new File(base, "Notes"), false);
    }
    File timer = new File(work, "Timer");
    if (timer.isDirectory()) {
      result.filesCopied += copyTree(timer, LuaPaths.alias(context, "timer"), false);
    }
    if (result.extensionsCopied > 0 || result.toolsCopied > 0) {
      result.notes.add(
          "Extensions and tools were copied to: " + base.getPath()
              + ". Open the Extensions menu to run them. Your Gemini API key was not restored; enter it again in the Gemini extension.");
    }
  }

  /** Copies each sub-folder of {@code from} into {@code to}. A folder that already exists in
   *  Advanced Screen Reader is kept as it is. Folder names lose spaces at the start and end. */
  private static int copyFolders(File from, File to, Result result) {
    File[] kids = from.listFiles();
    if (kids == null) {
      return 0;
    }
    Arrays.sort(kids);
    int copied = 0;
    int n = 0;
    Set<String> madeNow = new HashSet<>();
    for (File k : kids) {
      if (!k.isDirectory()) {
        continue;
      }
      n++;
      String name = k.getName().trim();
      if (name.isEmpty()) {
        name = "extension_" + n;
      }
      File dest = new File(to, name);
      if (dest.exists() && !madeNow.contains(name)) {
        result.alreadyThere++; // was in Advanced Screen Reader before this restore: keep it
        continue;
      }
      // Two Jieshuo folders can have the same name once spaces are removed: keep both.
      int suffix = 2;
      while (dest.exists()) {
        name = k.getName().trim() + " " + suffix++;
        dest = new File(to, name);
      }
      if (copyTree(k, dest, false) > 0) {
        madeNow.add(name);
        copied++;
      }
    }
    return copied;
  }

  /** Copies one file into a folder, unless a file with that name is already there. */
  private static int copyFileIfMissing(File src, File destDir) {
    if (!destDir.isDirectory() && !destDir.mkdirs()) {
      return 0;
    }
    File dest = new File(destDir, src.getName());
    if (dest.exists()) {
      return 0;
    }
    try (InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dest)) {
      byte[] chunk = new byte[16384];
      int n;
      while ((n = in.read(chunk)) > 0) {
        out.write(chunk, 0, n);
      }
      return 1;
    } catch (IOException e) {
      Log.w(TAG, "Could not copy " + src.getName(), e);
      com.google.android.accessibility.talkback.MsErrorLog.log("Restore: " + "Could not copy " + src.getName(), e);
      return 0;
    }
  }

  // ------------------------------------------------------------------------------------------

  private static String readText(File f) throws IOException {
    try (InputStream in = new FileInputStream(f)) {
      java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
      byte[] chunk = new byte[8192];
      int n;
      while ((n = in.read(chunk)) > 0) {
        bos.write(chunk, 0, n);
      }
      return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }
  }

  private static void deleteRecursive(File f) {
    File[] kids = f.listFiles();
    if (kids != null) {
      for (File k : kids) {
        deleteRecursive(k);
      }
    }
    f.delete();
  }
}
