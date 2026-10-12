/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.content.Context;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Lists for the editor: every public method of the Lua "service" object (found by reflection, so
 * it always matches the real code) and the official API help text of Jieshuo.
 */
final class ServiceApiList {
  private ServiceApiList() {}

  /** Entries like "toNext(Object, boolean)", sorted, without Java's own Object methods. */
  static List<String> serviceMethods() {
    TreeSet<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    for (Method m : LuaServiceApi.class.getMethods()) {
      if (Modifier.isStatic(m.getModifiers()) || m.getDeclaringClass() == Object.class) {
        continue;
      }
      StringBuilder sb = new StringBuilder(m.getName()).append('(');
      Class<?>[] p = m.getParameterTypes();
      for (int i = 0; i < p.length; i++) {
        if (i > 0) {
          sb.append(", ");
        }
        sb.append(p[i].getSimpleName());
      }
      set.add(sb.append(')').toString());
    }
    return new ArrayList<>(set);
  }

  /** Text to type for an entry: "service.name(" . */
  static String snippet(String entry) {
    int paren = entry.indexOf('(');
    return "service." + (paren > 0 ? entry.substring(0, paren) : entry) + "(";
  }

  /** One help item: first line = title, the rest = detail. */
  static final class Help {
    final String title;
    final String text;

    Help(String title, String text) {
      this.title = title;
      this.text = text;
    }
  }

  /** Reads assets/ide/JX_API_REFERENCE_zh.txt (lines "n| part / part / part"). */
  static List<Help> apiHelp(Context c) {
    List<Help> out = new ArrayList<>();
    try (BufferedReader r =
        new BufferedReader(
            new InputStreamReader(
                c.getAssets().open("ide/JX_API_REFERENCE_zh.txt"), StandardCharsets.UTF_8))) {
      String line;
      while ((line = r.readLine()) != null) {
        int bar = line.indexOf('|');
        String body = (bar >= 0 ? line.substring(bar + 1) : line).trim();
        if (body.isEmpty()) {
          continue;
        }
        String text = body.replace(" /  ", "\n").replace(" / ", "\n").trim();
        String title = text.split("\n", 2)[0].trim();
        if (title.length() > 70) {
          title = title.substring(0, 70) + "...";
        }
        out.add(new Help(title, text));
      }
    } catch (IOException e) {
      // empty list: the editor says the help file is missing
    }
    return out;
  }
}
