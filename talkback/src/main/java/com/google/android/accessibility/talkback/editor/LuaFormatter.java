/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** "Format source code" for the Lua editor: re-indents by block keywords. Pure Java. */
public final class LuaFormatter {
  private LuaFormatter() {}

  private static final Pattern WORD = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

  public static String format(String code) {
    String[] lines = code.replace("\r\n", "\n").split("\n", -1);
    StringBuilder out = new StringBuilder();
    int level = 0;
    boolean inLongString = false;
    for (int i = 0; i < lines.length; i++) {
      String raw = lines[i];
      if (inLongString) {
        out.append(raw);
        if (raw.contains("]]")) {
          inLongString = false;
        }
      } else {
        String t = raw.trim();
        int[] d = delta(t);
        int before = Math.max(0, level - d[0]); // closers at the start dedent this line
        out.append(t.isEmpty() ? "" : indent(before) + t);
        level = Math.max(0, level + d[1]);
        if (t.contains("[[") && !t.contains("]]")) {
          inLongString = true;
        }
      }
      if (i < lines.length - 1) {
        out.append('\n');
      }
    }
    return out.toString();
  }

  private static String indent(int n) {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < n; i++) {
      b.append("  ");
    }
    return b.toString();
  }

  /** Returns {leading dedent for this line, net change for the next lines}. */
  static int[] delta(String line) {
    String code = stripStringsAndComments(line);
    int net = 0;
    int leading = 0;
    int brNet = 0; // ( and { : one indent level per line at most, so "f({" is not double indented
    int brLeading = 0;
    boolean atStart = true;
    Matcher m = WORD.matcher(code);
    int pos = 0;
    int loopDo = 0; // "do" that belongs to while/for does not open a second block
    while (pos < code.length()) {
      char c = code.charAt(pos);
      if (c == '{' || c == '(') {
        brNet++;
        atStart = false;
        pos++;
      } else if (c == '}' || c == ')') {
        brNet--;
        if (atStart) {
          brLeading++;
        }
        pos++;
      } else if (Character.isLetter(c) || c == '_') {
        m.find(pos);
        String w = m.group();
        pos = m.end();
        switch (w) {
          case "function":
          case "if":
          case "repeat":
            net++;
            break;
          case "while":
          case "for":
            net++;
            loopDo++;
            break;
          case "do":
            if (loopDo > 0) {
              loopDo--;
            } else {
              net++;
            }
            break;
          case "end":
          case "until":
            net--;
            if (atStart) {
              leading++;
            }
            break;
          case "else":
          case "elseif":
            if (atStart) {
              leading++; // closes the previous branch; the new branch opens again (net 0)
            }
            break;
          case "then":
            break;
          default:
            break;
        }
        if (!w.equals("else") && !w.equals("elseif")) {
          atStart = atStart && (w.equals("end") || w.equals("until"));
        } else {
          atStart = false;
        }
      } else if (Character.isWhitespace(c)) {
        pos++;
      } else {
        atStart = false;
        pos++;
      }
    }
    brNet = Math.max(-1, Math.min(1, brNet));
    brLeading = Math.min(1, brLeading);
    return new int[] {leading + brLeading, net + brNet};
  }

  /** Removes "..." and '...' contents and -- comments so keywords inside them are ignored. */
  static String stripStringsAndComments(String line) {
    StringBuilder b = new StringBuilder();
    char quote = 0;
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (quote != 0) {
        if (c == '\\') {
          i++;
        } else if (c == quote) {
          quote = 0;
        }
        continue;
      }
      if (c == '"' || c == '\'') {
        quote = c;
        continue;
      }
      if (c == '-' && i + 1 < line.length() && line.charAt(i + 1) == '-') {
        break;
      }
      b.append(c);
    }
    return b.toString();
  }
}

/** Shares the string/comment stripper with other code helpers. */
final class LuaFormatterHelper {
  private LuaFormatterHelper() {}

  static String strip(String code) {
    StringBuilder b = new StringBuilder();
    for (String line : code.split("\n", -1)) {
      b.append(LuaFormatter.stripStringsAndComments(line)).append('\n');
    }
    return b.toString();
  }
}
