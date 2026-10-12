package com.google.android.accessibility.talkback.editor;

import com.google.android.accessibility.talkback.backup.Step45SettingsCheck;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Runs the real gesture planner on the real Jieshuo gesture files and settings. args: settings.dat gestures.tsv Plugins-dir Tools-dir installed-apps(comma) */
public class Step45GestureCheck {
  @SuppressWarnings("unchecked")
  public static void main(String[] a) throws Exception {
    Map<String, Object> jx;
    try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(a[0]))) {
      jx = new LinkedHashMap<>((Map<String, Object>) in.readObject());
    }
    Map<String, Map<String, String>> schemes = new LinkedHashMap<>();
    for (String line : Files.readAllLines(Paths.get(a[1]))) {
      String[] p = line.split("\t", 3);
      if (p.length == 3) schemes.computeIfAbsent(p[0], k -> new LinkedHashMap<>()).put(p[1], p[2]);
    }
    final Set<String> exts = names(a[2]);
    final Set<String> tools = names(a[3]);
    final Set<String> apps = new HashSet<>(Arrays.asList(a[4].split(",")));
    String globalName = String.valueOf(jx.get("gesture_package"));
    Map<String, String> global = schemes.getOrDefault(globalName, new LinkedHashMap<>());
    Map<String, Map<String, String>> perApp = new LinkedHashMap<>();
    for (Map.Entry<String, Map<String, String>> e : schemes.entrySet()) {
      if (!e.getKey().equals(globalName)) perApp.put(e.getKey(), e.getValue());
    }
    JxGestureImport.Resolver r = new JxGestureImport.Resolver() {
      public String command(String n) { return Step45SettingsCheck.idOf(n); }
      public String extension(String n) { return find(exts, n); }
      public String tool(String n) { return find(tools, n); }
      public String appPackage(String n) { return apps.contains(n) ? "pkg." + n.toLowerCase() : null; }
    };
    JxGestureImport.Plan plan = JxGestureImport.plan(global, globalName, perApp, jx, r);
    System.out.println("global=" + plan.global + " perApp=" + plan.perApp + " multiPart=" + plan.multiPart
        + " launchers=" + plan.launchers + " schemes=" + plan.schemes + " skipped=" + plan.skipped.size());
    for (JxGestureImport.Assign x : plan.assigns) {
      String where = x.scheme == null ? "global" : x.scheme;
      String id = x.id >= 100000 ? "multi(" + ((x.id - 100000) / 1000) + "," + ((x.id - 100000) % 1000) + ")" : String.valueOf(x.id);
      System.out.println(where + "  " + id + "  " + x.kind + "  " + x.name);
    }
    System.out.println("--- skipped");
    for (String s : plan.skipped) System.out.println(s);
  }

  static Set<String> names(String dir) {
    Set<String> out = new LinkedHashSet<>();
    File[] fs = new File(dir).listFiles();
    if (fs != null) for (File f : fs) if (f.isDirectory() && new File(f, "main.lua").isFile()) out.add(f.getName().trim());
    return out;
  }

  static String find(Set<String> set, String n) {
    for (String s : set) if (s.equalsIgnoreCase(n.trim())) return s;
    return null;
  }
}
