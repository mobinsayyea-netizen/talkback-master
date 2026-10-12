/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.content.Context;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Custom voice commands (Jieshuo folder "Custom voice commands"): a spoken phrase starts an
 * extension or a tool. Stored in a plain text file, one command per line: phrase TAB kind TAB name
 * where kind is "ext" or "tool". Not compiled yet.
 */
public final class CustomVoiceCommands {
  private CustomVoiceCommands() {}

  /** One saved command. */
  public static final class Entry {
    public final String phrase;
    public final String kind; // "ext", "tool" or "cmd" (an execute() command, only from gesture schemes)
    public final String name; // folder name of the extension or tool

    public Entry(String phrase, String kind, String name) {
      this.phrase = phrase;
      this.kind = kind;
      this.name = name;
    }
  }

  static File file(Context c) {
    File dir = new File(LuaPaths.base(c), "Custom voice commands");
    dir.mkdirs();
    return new File(dir, "commands.txt");
  }

  public static List<Entry> load(Context c) {
    List<Entry> out = new ArrayList<>();
    File f = file(c);
    if (!f.isFile()) {
      return out;
    }
    try (BufferedReader r = new BufferedReader(new FileReader(f))) {
      String line;
      while ((line = r.readLine()) != null) {
        String[] p = line.split("\t", -1);
        if (p.length == 3 && !p[0].trim().isEmpty() && !p[2].trim().isEmpty()) {
          out.add(new Entry(p[0].trim(), p[1].trim(), p[2].trim()));
        }
      }
    } catch (IOException e) {
      // unreadable file: act as if there are no commands
    }
    return out;
  }

  static boolean save(Context c, List<Entry> entries) {
    try (FileWriter w = new FileWriter(file(c))) {
      for (Entry e : entries) {
        w.write(e.phrase.replace('\t', ' ').replace('\n', ' ') + "\t" + e.kind + "\t" + e.name + "\n");
      }
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  /** Lower case, no punctuation, single spaces: "Open  WhatsApp!" and "open whatsapp" match. */
  static String normalize(String s) {
    if (s == null) {
      return "";
    }
    String t = s.toLowerCase(Locale.ROOT).replaceAll("[\\p{Punct}\\u0964]", " ");
    return t.replaceAll("\\s+", " ").trim();
  }

  /** The saved command whose phrase equals what was said, or null. */
  public static Entry match(Context c, String spoken) {
    String said = normalize(spoken);
    if (said.isEmpty()) {
      return null;
    }
    for (Entry e : load(c)) {
      if (normalize(e.phrase).equals(said)) {
        return MsExtSettings.voiceMayRun(c, e.kind) ? e : null;
      }
    }
    return null;
  }

  /** Saves a phrase for a target. The same phrase can only start one thing, so it replaces. */
  public static boolean add(Context c, String phrase, String kind, String name) {
    List<Entry> all = load(c);
    String key = normalize(phrase);
    for (int i = all.size() - 1; i >= 0; i--) {
      if (normalize(all.get(i).phrase).equals(key)) {
        all.remove(i);
      }
    }
    all.add(new Entry(phrase, kind, name));
    return save(c, all);
  }

  /** Deletes every voice command that starts this extension or tool. */
  static void removeFor(Context c, String kind, String name) {
    List<Entry> all = load(c);
    for (int i = all.size() - 1; i >= 0; i--) {
      if (all.get(i).kind.equals(kind) && all.get(i).name.equals(name)) {
        all.remove(i);
      }
    }
    save(c, all);
  }

  /** After a rename: voice commands keep working with the new folder name. */
  static void renameFor(Context c, String kind, String oldName, String newName) {
    List<Entry> all = load(c);
    List<Entry> out = new ArrayList<>();
    for (Entry e : all) {
      out.add(e.kind.equals(kind) && e.name.equals(oldName) ? new Entry(e.phrase, kind, newName) : e);
    }
    save(c, out);
  }

  /** Runs the extension or tool on its own thread. */
  public static void run(final Context c, final Entry e) {
    final Context app = c.getApplicationContext();
    new Thread(
            () -> {
              if ("cmd".equals(e.kind)) {
                LuaServiceApi.executeStatic(app, e.name);
                return;
              }
              File base = "tool".equals(e.kind) ? LuaPaths.tools(app) : LuaPaths.extensions(app);
              File main = new File(new File(base, e.name), "main.lua");
              if (!main.isFile()) {
                LuaServiceApi.speakStatic("Not found: " + e.name);
                return;
              }
              String err = LuaServiceApi.run(app, main, null);
              if (err != null) {
                LuaServiceApi.speakStatic(err);
              }
            },
            "ms-custom-voice-command")
        .start();
  }

  /** Folder names of all extensions ("ext") and tools ("tool") that have a main.lua. */
  static List<Entry> targets(Context c) {
    List<Entry> out = new ArrayList<>();
    addTargets(out, LuaPaths.extensions(c), "ext");
    addTargets(out, LuaPaths.tools(c), "tool");
    return out;
  }

  private static void addTargets(List<Entry> out, File base, String kind) {
    File[] dirs = base.listFiles();
    if (dirs == null) {
      return;
    }
    java.util.Arrays.sort(dirs);
    for (File d : dirs) {
      if (d.isDirectory() && new File(d, "main.lua").isFile()) {
        out.add(new Entry("", kind, d.getName()));
      }
    }
  }
}
