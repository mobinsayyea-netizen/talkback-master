package com.google.android.accessibility.talkback.backup;

import java.io.*;
import java.util.*;

/** Runs the real settings mapper on a real Jieshuo settings.dat and prints what it decided. */
public class Step45SettingsCheck {
  private static Map<String, String> byName;

  /** Same lookup as JxCommands.idOf (id, English or Chinese name, case ignored), read from the source file. */
  public static synchronized String idOf(String name) {
    try {
      if (byName == null) {
        byName = new HashMap<>();
        String src = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(
            System.getProperty("jx.commands.src"))), "UTF-8");
        src = src.substring(src.indexOf("TABLE = {"), src.indexOf("LEGACY = {"));
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("\\{\"([^\"]+)\", \"([^\"]+)\", \"([^\"]+)\"\\}").matcher(src);
        while (m.find()) {
          for (int i = 1; i <= 3; i++) {
            byName.putIfAbsent(m.group(i).trim().toLowerCase(Locale.ROOT), m.group(1));
          }
        }
        byName.putIfAbsent("back", "command_to_back");
        byName.putIfAbsent("home", "command_to_home");
        byName.putIfAbsent("recents", "command_to_recents");
        byName.putIfAbsent("copy", "command_copy");
        byName.putIfAbsent("paste", "command_paste");
      }
      String k = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
      String id = byName.get(k);
      if (id == null) {
        String c = com.google.android.accessibility.talkback.editor.JxFunctionAliases.canonical(name);
        id = byName.get(c == null ? "" : c.trim().toLowerCase(Locale.ROOT));
      }
      return id;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  public static void main(String[] a) throws Exception {
    Map<String, Object> jx;
    try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(a[0]))) {
      jx = new LinkedHashMap<>((Map<String, Object>) in.readObject());
    }
    final Set<String> installed = new HashSet<>(Arrays.asList(a.length > 1 ? a[1].split(",") : new String[0]));
    JxSettingsMapper.Plan p = JxSettingsMapper.plan(jx, installed::contains, Step45SettingsCheck::idOf);
    System.out.println("jieshuo keys: " + jx.size());
    System.out.println("applied: " + p.apply.size() + "  rejected: " + p.rejected.size()
        + "  noMatch: " + p.noMatch.size() + "  private: " + p.privateSkipped);
    System.out.println("--- applied (jieshuo -> ms = value)");
    for (Map.Entry<String, String> e : p.from.entrySet()) {
      System.out.println(e.getKey() + " -> " + e.getValue() + " = " + p.apply.get(e.getValue()));
    }
    System.out.println("--- rejected");
    for (Map.Entry<String, String> e : p.rejected.entrySet()) {
      System.out.println(e.getKey() + ": " + e.getValue());
    }
    System.out.println("--- private names skipped");
    for (String k : jx.keySet()) {
      if (JxSettingsMapper.isPrivate(k)) System.out.println(k);
    }
    // safety: no private key may ever be in the plan
    for (String k : p.from.keySet()) {
      if (JxSettingsMapper.isPrivate(k)) throw new AssertionError("private key mapped: " + k);
    }
    for (Object v : p.apply.values()) {
      String s = String.valueOf(v);
      if (s.matches(".*(AIza|ghp_|github_pat|sk-)[A-Za-z0-9_\\-]{10,}.*")) throw new AssertionError("secret-looking value");
    }
    System.out.println("SAFETY OK");
  }
}
