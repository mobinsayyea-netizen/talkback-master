/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.util.HashMap;
import java.util.Map;

/**
 * MS Screen Reader: custom node aliases. Each line is "key=alias". The key is either a view id
 * (for example com.whatsapp:id/send) or the exact text / description of the element (ignoring
 * case). When an element matches, the alias is spoken as its name.
 */
public final class MsNodeAlias {
  public static final String K_USE = "ms_rd_alias_use";
  public static final String K_TEXT = "ms_rd_alias_text";

  private static String cachedRaw = null;
  private static Map<String, String> cachedMap = new HashMap<>();

  private MsNodeAlias() {}

  /** Returns the alias for the node, or null when there is none. */
  public static CharSequence lookup(Context context, AccessibilityNodeInfoCompat node) {
    if (context == null || node == null) {
      return null;
    }
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    if (!prefs.getBoolean(K_USE, false)) {
      return null;
    }
    Map<String, String> map = load(prefs.getString(K_TEXT, ""));
    if (map.isEmpty()) {
      return null;
    }
    String id = node.getViewIdResourceName();
    if (!TextUtils.isEmpty(id)) {
      String a = map.get(id.toLowerCase());
      if (a != null) {
        return a;
      }
    }
    CharSequence text = AccessibilityNodeInfoUtils.getText(node);
    if (!TextUtils.isEmpty(text)) {
      String a = map.get(text.toString().trim().toLowerCase());
      if (a != null) {
        return a;
      }
    }
    CharSequence desc = node.getContentDescription();
    if (!TextUtils.isEmpty(desc)) {
      String a = map.get(desc.toString().trim().toLowerCase());
      if (a != null) {
        return a;
      }
    }
    return null;
  }

  private static synchronized Map<String, String> load(String raw) {
    if (raw == null) {
      raw = "";
    }
    if (raw.equals(cachedRaw)) {
      return cachedMap;
    }
    cachedRaw = raw;
    Map<String, String> map = new HashMap<>();
    for (String line : raw.split("\\r?\\n")) {
      int eq = line.indexOf('=');
      if (eq <= 0) {
        continue;
      }
      String key = line.substring(0, eq).trim().toLowerCase();
      String val = line.substring(eq + 1).trim();
      if (!key.isEmpty() && !val.isEmpty()) {
        map.put(key, val);
      }
    }
    cachedMap = map;
    return map;
  }
}
