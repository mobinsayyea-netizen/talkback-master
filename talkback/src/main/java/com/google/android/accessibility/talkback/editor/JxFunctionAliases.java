/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Pure Java (no Android): other English spellings that Jieshuo's own screens and backups use for a
 * function, mapped to the name MS knows (the English column of {@link JxCommands#TABLE}). Only names
 * whose meaning is certain are listed.
 */
public final class JxFunctionAliases {
  private JxFunctionAliases() {}

  private static final Map<String, String> MAP = new HashMap<>();

  static {
    MAP.put("recent apps", "Recent applications");
    MAP.put("append copy", "Append");
    MAP.put("notification panel", "Notification bar");
    MAP.put("program settings", "Screen reader settings");
    MAP.put("previous item", "Previous focusable element");
    MAP.put("next item", "Next focusable element");
  }

  /** The name to look up in the function table: the alias target, or the name itself. */
  public static String canonical(String name) {
    if (name == null) {
      return null;
    }
    String t = name.trim();
    if (t.startsWith("[") && t.endsWith("]") && t.length() > 2) {
      t = t.substring(1, t.length() - 1).trim();
    }
    String a = MAP.get(t.toLowerCase(Locale.ROOT));
    return a != null ? a : t;
  }
}
