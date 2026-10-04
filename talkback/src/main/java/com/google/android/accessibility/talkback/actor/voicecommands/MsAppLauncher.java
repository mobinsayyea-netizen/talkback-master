/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.actor.voicecommands;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * MS Screen Reader: "open <app name>" voice command. Understands English ("open play store") and
 * Hindi ("प्ले स्टोर खोलो", "ओपन व्हाट्सएप") and starts the best matching launcher app.
 */
public final class MsAppLauncher {

  /** Result of {@link #parse}: the spoken app name, or null when this is not an open command. */
  private static final String[] PREFIXES = {
    "open ", "launch ", "start ", "run ", "ओपन ", "खोलो ", "खोल ", "चालू करो ", "शुरू करो "
  };

  private static final String[] SUFFIXES = {
    " खोलो", " खोलिए", " खोल दो", " खोल दीजिए", " ओपन करो", " ओपन कर", " चालू करो", " शुरू करो",
    " open", " kholo"
  };

  /** Spoken Hindi / alternate names mapped to the name shown on the phone. */
  private static final String[][] ALIASES = {
    {"प्ले स्टोर", "play store"},
    {"प्लेस्टोर", "play store"},
    {"प्ले स्टोअर", "play store"},
    {"गूगल प्ले", "play store"},
    {"google play", "play store"},
    {"व्हाट्सएप", "whatsapp"},
    {"वाट्सएप", "whatsapp"},
    {"व्हाट्सअप", "whatsapp"},
    {"व्हॉट्सऍप", "whatsapp"},
    {"यूट्यूब", "youtube"},
    {"यू ट्यूब", "youtube"},
    {"कैमरा", "camera"},
    {"सेटिंग", "settings"},
    {"सेटिंग्स", "settings"},
    {"क्रोम", "chrome"},
    {"जीमेल", "gmail"},
    {"गूगल मैप", "maps"},
    {"मैप्स", "maps"},
    {"गूगल", "google"},
    {"फोन", "phone"},
    {"फ़ोन", "phone"},
    {"संदेश", "messages"},
    {"मैसेज", "messages"},
    {"कैलकुलेटर", "calculator"},
    {"घड़ी", "clock"},
    {"गैलरी", "gallery"},
    {"फोटो", "photos"},
    {"कॉन्टैक्ट", "contacts"},
    {"संपर्क", "contacts"},
    {"फोनपे", "phonepe"},
    {"फोन पे", "phonepe"},
    {"टेलीग्राम", "telegram"},
    {"इंस्टाग्राम", "instagram"},
    {"फेसबुक", "facebook"},
  };

  private MsAppLauncher() {}

  /** Returns the app name from an "open ..." command, or null if the command is not one. */
  public static @Nullable String parse(String command) {
    if (command == null) {
      return null;
    }
    String c = command.trim().toLowerCase(Locale.ROOT).replaceAll("[\\p{Punct}।]", " ");
    c = c.replaceAll("\\s+", " ").trim() + " ";
    for (String p : PREFIXES) {
      if (c.startsWith(p)) {
        String name = c.substring(p.length()).trim();
        name = stripSuffix(name);
        return name.isEmpty() ? null : name;
      }
    }
    String t = c.trim();
    for (String s : SUFFIXES) {
      if (t.endsWith(s)) {
        String name = t.substring(0, t.length() - s.length()).trim();
        return name.isEmpty() ? null : name;
      }
    }
    return null;
  }

  private static String stripSuffix(String name) {
    String t = name;
    for (String s : SUFFIXES) {
      if (t.endsWith(s)) {
        t = t.substring(0, t.length() - s.length()).trim();
      }
    }
    return t;
  }

  /** Result of a launch attempt. */
  public static final class Result {
    public final boolean launched;
    public final String label;

    Result(boolean launched, String label) {
      this.launched = launched;
      this.label = label;
    }
  }

  /** Finds the best matching launcher app for the spoken name and starts it. */
  public static Result open(Context context, String spoken) {
    String wanted = normalize(applyAlias(spoken));
    PackageManager pm = context.getPackageManager();
    Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
    List<ResolveInfo> apps = pm.queryIntentActivities(main, 0);
    ResolveInfo best = null;
    int bestScore = 0;
    String bestLabel = "";
    for (ResolveInfo ri : apps) {
      CharSequence cs = ri.loadLabel(pm);
      if (cs == null) {
        continue;
      }
      String label = cs.toString();
      int score = score(wanted, normalize(label));
      // Prefer shorter labels when scores tie (so "Play Store" beats "Play Store Beta Helper").
      if (score > bestScore
          || (score == bestScore && score > 0 && label.length() < bestLabel.length())) {
        best = ri;
        bestScore = score;
        bestLabel = label;
      }
    }
    if (best == null || bestScore == 0) {
      return new Result(false, spoken);
    }
    Intent launch = pm.getLaunchIntentForPackage(best.activityInfo.packageName);
    if (launch == null) {
      launch =
          new Intent(Intent.ACTION_MAIN)
              .addCategory(Intent.CATEGORY_LAUNCHER)
              .setClassName(best.activityInfo.packageName, best.activityInfo.name);
    }
    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
    try {
      context.startActivity(launch);
      return new Result(true, bestLabel);
    } catch (RuntimeException e) {
      return new Result(false, bestLabel);
    }
  }

  private static String applyAlias(String spoken) {
    String s = spoken.trim().toLowerCase(Locale.ROOT);
    for (String[] a : ALIASES) {
      if (s.equals(a[0])) {
        return a[1];
      }
    }
    for (String[] a : ALIASES) {
      if (s.contains(a[0])) {
        s = s.replace(a[0], a[1]);
      }
    }
    return s;
  }

  private static String normalize(String s) {
    return s.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
  }

  /** 100 exact, 80 same without spaces, 60 starts with, 40 contains, 20 every word is in label. */
  private static int score(String wanted, String label) {
    if (wanted.isEmpty() || label.isEmpty()) {
      return 0;
    }
    if (label.equals(wanted)) {
      return 100;
    }
    if (label.replace(" ", "").equals(wanted.replace(" ", ""))) {
      return 80;
    }
    if (label.startsWith(wanted)) {
      return 60;
    }
    if (label.contains(wanted)) {
      return 40;
    }
    List<String> words = new ArrayList<>();
    for (String w : wanted.split(" ")) {
      if (!w.isEmpty()) {
        words.add(w);
      }
    }
    for (String w : words) {
      if (!label.contains(w)) {
        return 0;
      }
    }
    return words.isEmpty() ? 0 : 20;
  }
}
