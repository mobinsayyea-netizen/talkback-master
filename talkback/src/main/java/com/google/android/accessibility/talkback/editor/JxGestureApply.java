/*
 * Copyright 2026 Advanced Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Android side of {@link JxGestureImport}: finds the restored extensions and tools, makes the small
 * "Open app" extensions, and writes the assignments into the gesture schemes. A gesture that already
 * has an assignment in MS is replaced by the Jieshuo one; every other gesture is left alone.
 */
public final class JxGestureApply {
  private JxGestureApply() {}

  /** Lets the backup code (another package) say which app has which label. */
  public interface Apps {
    /** Package name of the installed app with this label (case ignored), or null. */
    String packageOf(String label);
  }

  /** MS's function id for a Jieshuo function name (any spelling service.execute accepts), or null. */
  public static String commandId(String name) {
    String id = JxCommands.idOf(name);
    return id != null ? id : JxCommands.idOf(JxFunctionAliases.canonical(name));
  }

  public static final class Summary {
    public int global;
    public int perApp;
    public int multiPart;
    public int launchers;
    public final List<String> skipped = new ArrayList<>();
  }

  /**
   * @param global config of the scheme Jieshuo had on (gesture name -> target text)
   * @param perApp app label -> that app's config
   * @param settings Jieshuo settings map (multi-part gestures, which app schemes are on)
   */
  public static Summary restore(
      final Context c,
      Map<String, String> global,
      String globalName,
      Map<String, Map<String, String>> perApp,
      Map<String, Object> settings,
      final Apps apps) {
    final File extDir = LuaPaths.extensions(c);
    final File toolDir = LuaPaths.tools(c);
    JxGestureImport.Resolver resolver =
        new JxGestureImport.Resolver() {
          @Override
          public String command(String name) {
            String id = commandId(name);
            return id == null ? null : id;
          }

          @Override
          public String extension(String name) {
            return folder(extDir, name);
          }

          @Override
          public String tool(String name) {
            return folder(toolDir, name);
          }

          @Override
          public String appPackage(String appName) {
            return apps.packageOf(appName);
          }
        };
    JxGestureImport.Plan plan =
        JxGestureImport.plan(global, globalName, perApp, settings, resolver);
    Summary s = new Summary();
    s.skipped.addAll(plan.skipped);
    for (Map.Entry<String, String> e : plan.launchers.entrySet()) {
      if (makeLauncher(extDir, e.getKey(), e.getValue())) {
        s.launchers++;
      }
    }
    for (Map.Entry<String, Boolean> e : plan.schemes.entrySet()) {
      AppGestureSchemes.create(c, e.getKey());
      AppGestureSchemes.setEnabled(c, e.getKey(), e.getValue());
    }
    for (JxGestureImport.Assign a : plan.assigns) {
      if (a.scheme == null) {
        ExtensionGestures.set(c, a.id, a.kind, a.name);
      } else {
        AppGestureSchemes.set(c, a.scheme, a.id, a.kind, a.name);
      }
    }
    s.global = plan.global;
    s.perApp = plan.perApp;
    s.multiPart = plan.multiPart;
    return s;
  }

  /** Real name of the sub-folder with a main.lua whose name equals {@code name} (trim, case ignored). */
  private static String folder(File base, String name) {
    String want = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    File[] dirs = base.listFiles();
    if (dirs == null) {
      return null;
    }
    java.util.Arrays.sort(dirs);
    for (File d : dirs) {
      if (d.isDirectory()
          && d.getName().trim().toLowerCase(Locale.ROOT).equals(want)
          && new File(d, "main.lua").isFile()) {
        return d.getName();
      }
    }
    return null;
  }

  /** Makes extension "Open WhatsApp" that starts the app. An existing folder is kept. */
  private static boolean makeLauncher(File extDir, String folderName, String appLabel) {
    File dir = new File(extDir, folderName);
    File main = new File(dir, "main.lua");
    if (main.isFile()) {
      return true;
    }
    if (!dir.isDirectory() && !dir.mkdirs()) {
      return false;
    }
    String label = appLabel.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    String code =
        "-- title: " + folderName + "\n"
            + "service.startApp(\"" + label + "\")\n"
            + "return true\n";
    try (FileOutputStream out = new FileOutputStream(main)) {
      out.write(code.getBytes(StandardCharsets.UTF_8));
      return true;
    } catch (IOException e) {
      return false;
    }
  }
}
