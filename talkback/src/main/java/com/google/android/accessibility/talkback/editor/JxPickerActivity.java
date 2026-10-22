/*
 * Copyright 2026 Advanced Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Plain list screen used by execute("Extensions") and execute("Application"). Not compiled yet. */
public class JxPickerActivity extends Activity {
  public static final String EXTRA_KIND = "kind";
  public static final String KIND_EXTENSIONS = "extensions";
  public static final String KIND_APPS = "apps";
  public static final String KIND_TOOLS = "tools";
  public static final String KIND_NAVTYPE = "navtype";

  static boolean show(Context context, String kind) {
    if (KIND_EXTENSIONS.equals(kind)) {
      return ExtensionDialogActivity.show(context);
    }
    if (KIND_TOOLS.equals(kind)) {
      return ExtensionListActivity.show(context, "tool", false);
    }
    try {
      Intent i = new Intent(context, JxPickerActivity.class);
      i.putExtra(EXTRA_KIND, kind);
      i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(i);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    String kind = getIntent().getStringExtra(EXTRA_KIND);
    final ListView list = new ListView(this);
    setContentView(list);
    final List<String> titles = new ArrayList<>();
    if (KIND_APPS.equals(kind)) {
      setTitle("Application");
      PackageManager pm = getPackageManager();
      Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
      final List<ResolveInfo> apps = pm.queryIntentActivities(main, 0);
      final PackageManager fpm = pm;
      Collections.sort(
          apps,
          new Comparator<ResolveInfo>() {
            @Override
            public int compare(ResolveInfo a, ResolveInfo b) {
              return a.loadLabel(fpm).toString().compareToIgnoreCase(b.loadLabel(fpm).toString());
            }
          });
      for (ResolveInfo r : apps) {
        titles.add(r.loadLabel(pm).toString());
      }
      list.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, titles));
      list.setOnItemClickListener(
          (parent, view, position, id) -> {
            Intent launch = fpm.getLaunchIntentForPackage(apps.get(position).activityInfo.packageName);
            if (launch != null) {
              launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              startActivity(launch);
            }
            finish();
          });
    } else if (KIND_NAVTYPE.equals(kind)) {
      setTitle("Navigation type");
      for (String[] t : MsNavModes.TYPES) {
        titles.add(t[1] + (t[0].equals(MsNavModes.current(this)) ? " (selected)" : ""));
      }
      list.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, titles));
      list.setOnItemClickListener(
          (parent, view, position, id) -> {
            MsNavModes.choose(this, MsNavModes.TYPES[position][0]);
            finish();
          });
    } else if (KIND_TOOLS.equals(kind)) {
      setTitle("Tools");
      final List<CustomVoiceCommands.Entry> tools = new ArrayList<>();
      for (CustomVoiceCommands.Entry t : CustomVoiceCommands.targets(this)) {
        if ("tool".equals(t.kind)) {
          tools.add(t);
          titles.add(t.name);
        }
      }
      if (titles.isEmpty()) {
        titles.add("No tools yet");
      }
      list.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, titles));
      list.setOnItemClickListener(
          (parent, view, position, id) -> {
            if (position < tools.size()) {
              CustomVoiceCommands.run(this, tools.get(position));
            }
            finish();
          });
    } else {
      setTitle("Extensions");
      List<LuaExtensionManager.Extension> exts = LuaExtensionManager.prepareMenuList(this);
      for (LuaExtensionManager.Extension e : exts) {
        titles.add(e.title);
      }
      list.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, titles));
      list.setOnItemClickListener(
          (parent, view, position, id) -> {
            LuaExtensionManager.runFromMenu(this, LuaExtensionManager.MENU_ID_BASE + position, null);
            finish();
          });
    }
  }
}
