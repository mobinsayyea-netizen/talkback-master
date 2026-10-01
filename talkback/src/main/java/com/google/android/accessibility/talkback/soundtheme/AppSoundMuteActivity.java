package com.google.android.accessibility.talkback.soundtheme;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * "Apps where additional sound effects are not used" (same idea as Jieshuo's list): a multiple
 * choice list of installed apps. Ticked apps get no extra sound effects while they are in front.
 */
public class AppSoundMuteActivity extends Activity {

  private static final class AppEntry {
    final String label;
    final String packageName;

    AppEntry(String label, String packageName) {
      this.label = label;
      this.packageName = packageName;
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(this);
    final Set<String> muted =
        new HashSet<>(prefs.getStringSet(SoundThemeManager.PREF_NO_SOUND_APPS, new HashSet<>()));

    final PackageManager pm = getPackageManager();
    Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
    List<ResolveInfo> infos = pm.queryIntentActivities(launcher, 0);
    final List<AppEntry> apps = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    for (ResolveInfo info : infos) {
      String pkg = info.activityInfo.packageName;
      if (seen.add(pkg)) {
        apps.add(new AppEntry(String.valueOf(info.loadLabel(pm)), pkg));
      }
    }
    final Collator collator = Collator.getInstance();
    Collections.sort(apps, (a, b) -> collator.compare(a.label, b.label));

    String[] names = new String[apps.size()];
    boolean[] checked = new boolean[apps.size()];
    for (int i = 0; i < apps.size(); i++) {
      names[i] = apps.get(i).label;
      checked[i] = muted.contains(apps.get(i).packageName);
    }

    new AlertDialog.Builder(this)
        .setTitle("Apps where additional sound effects are not used")
        .setMultiChoiceItems(
            names,
            checked,
            (dialog, which, isChecked) -> {
              String pkg = apps.get(which).packageName;
              if (isChecked) {
                muted.add(pkg);
              } else {
                muted.remove(pkg);
              }
            })
        .setPositiveButton(
            android.R.string.ok,
            (dialog, which) ->
                prefs.edit().putStringSet(SoundThemeManager.PREF_NO_SOUND_APPS, muted).apply())
        .setNegativeButton(android.R.string.cancel, null)
        .setOnDismissListener(dialog -> finish())
        .show();
  }
}
