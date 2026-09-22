package com.google.android.accessibility.talkback.clipboard;

import android.content.Context;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Persistent history of copied text. Items stay until the user deletes them. The newest item is
 * first in the list. Favorite items are not removed by "delete all".
 */
public final class ClipboardStore {

  private static final String TAG = "ClipboardStore";
  private static final String FILE_NAME = "ms_clipboard_history.json";
  private static final int MAX_TEXT_LENGTH = 200000;
  private static final Object LOCK = new Object();

  /** One copied text. */
  public static final class Entry {
    public final long id;
    public final String text;
    public final long time;
    public final boolean favorite;

    Entry(long id, String text, long time, boolean favorite) {
      this.id = id;
      this.text = text;
      this.time = time;
      this.favorite = favorite;
    }
  }

  private ClipboardStore() {}

  /** Returns all items, newest first. */
  public static List<Entry> all(Context context) {
    synchronized (LOCK) {
      return load(context);
    }
  }

  /** Adds a copied text at the top. If the same text is already there, it moves to the top. */
  public static void add(Context context, CharSequence text) {
    replace(context, null, text);
  }

  /**
   * Puts {@code newText} at the top. If an item with {@code oldText} exists, it is replaced (its
   * favorite mark is kept), otherwise a new item is added.
   */
  public static void replace(Context context, String oldText, CharSequence newText) {
    if (context == null || newText == null) {
      return;
    }
    String value = newText.toString();
    if (value.trim().isEmpty()) {
      return;
    }
    if (value.length() > MAX_TEXT_LENGTH) {
      value = value.substring(0, MAX_TEXT_LENGTH);
    }
    synchronized (LOCK) {
      List<Entry> items = load(context);
      boolean favorite = false;
      long id = nextId(items);
      if (oldText != null && !oldText.isEmpty()) {
        Iterator<Entry> it = items.iterator();
        while (it.hasNext()) {
          Entry e = it.next();
          if (e.text.equals(oldText)) {
            favorite = favorite || e.favorite;
            id = e.id;
            it.remove();
            break;
          }
        }
      }
      Iterator<Entry> it = items.iterator();
      while (it.hasNext()) {
        Entry e = it.next();
        if (e.text.equals(value)) {
          favorite = favorite || e.favorite;
          it.remove();
        }
      }
      items.add(0, new Entry(id, value, System.currentTimeMillis(), favorite));
      save(context, items);
    }
  }

  /** Marks or unmarks the given items as favorite. */
  public static void setFavorite(Context context, Collection<Long> ids, boolean favorite) {
    synchronized (LOCK) {
      List<Entry> items = load(context);
      List<Entry> out = new ArrayList<>();
      for (Entry e : items) {
        if (ids.contains(e.id)) {
          out.add(new Entry(e.id, e.text, e.time, favorite));
        } else {
          out.add(e);
        }
      }
      save(context, out);
    }
  }

  /** Deletes the given items. */
  public static void delete(Context context, Collection<Long> ids) {
    synchronized (LOCK) {
      List<Entry> items = load(context);
      List<Entry> out = new ArrayList<>();
      for (Entry e : items) {
        if (!ids.contains(e.id)) {
          out.add(e);
        }
      }
      save(context, out);
    }
  }

  /** Deletes everything except favorites. */
  public static void deleteAllKeepFavorites(Context context) {
    synchronized (LOCK) {
      List<Entry> items = load(context);
      List<Entry> out = new ArrayList<>();
      for (Entry e : items) {
        if (e.favorite) {
          out.add(e);
        }
      }
      save(context, out);
    }
  }

  private static long nextId(List<Entry> items) {
    long max = 0;
    for (Entry e : items) {
      if (e.id > max) {
        max = e.id;
      }
    }
    return max + 1;
  }

  private static List<Entry> load(Context context) {
    List<Entry> items = new ArrayList<>();
    File file = new File(context.getFilesDir(), FILE_NAME);
    if (!file.exists()) {
      return items;
    }
    try (InputStream in = new FileInputStream(file)) {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      byte[] chunk = new byte[8192];
      int n;
      while ((n = in.read(chunk)) > 0) {
        buffer.write(chunk, 0, n);
      }
      JSONArray array = new JSONArray(new String(buffer.toByteArray(), StandardCharsets.UTF_8));
      for (int i = 0; i < array.length(); i++) {
        JSONObject o = array.getJSONObject(i);
        items.add(
            new Entry(
                o.getLong("id"), o.getString("t"), o.optLong("ts", 0L), o.optBoolean("f", false)));
      }
    } catch (IOException | JSONException e) {
      Log.w(TAG, "Could not read clipboard history", e);
    }
    return items;
  }

  private static void save(Context context, List<Entry> items) {
    try {
      JSONArray array = new JSONArray();
      for (Entry e : items) {
        JSONObject o = new JSONObject();
        o.put("id", e.id);
        o.put("t", e.text);
        o.put("ts", e.time);
        o.put("f", e.favorite);
        array.put(o);
      }
      File file = new File(context.getFilesDir(), FILE_NAME);
      File tmp = new File(context.getFilesDir(), FILE_NAME + ".tmp");
      try (FileOutputStream out = new FileOutputStream(tmp)) {
        out.write(array.toString().getBytes(StandardCharsets.UTF_8));
        out.getFD().sync();
      }
      if (!tmp.renameTo(file)) {
        Log.w(TAG, "Could not replace clipboard history file");
      }
    } catch (IOException | JSONException e) {
      Log.w(TAG, "Could not save clipboard history", e);
    }
  }
}
