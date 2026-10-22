package com.google.android.accessibility.talkback.permission;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.talkback.TalkBackPreferencesActivity;
import com.google.android.accessibility.talkback.R;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Permissions screen. Opened from the launcher icon until every permission is granted (then it
 * never blocks again and the icon opens the settings directly). Also reachable any time from
 * Settings > Tools > Check permissions (extra "from_settings"), where it does not block or exit.
 */
public class MsPermissionsActivity extends Activity {
  private static final String PREFS = "ms_permissions";
  private static final String KEY_DONE = "onboarding_done";
  private static final String EXTRA_FROM_SETTINGS = "from_settings";
  private static final int REQ_ALL = 4711;
  private static final int REQ_ONE = 4712;
  private static final String SERVICE_CLASS = "com.google.android.marvin.talkback.TalkBackService";

  private static final String K_FILES = "files";
  private static final String K_BATTERY = "battery";
  private static final String K_ACC = "accessibility";

  /** One row on screen: either a runtime permission (perm != null) or a special step (kind). */
  private static final class Row {
    final int label;
    final int why;
    final String perm;
    final String kind;

    Row(int label, int why, String perm, String kind) {
      this.label = label;
      this.why = why;
      this.perm = perm;
      this.kind = kind;
    }
  }

  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Set<String> attempted = new HashSet<>();
  private boolean autoRunning = false;
  private boolean waitingReturn = false;
  private boolean fromSettings = false;

  private LinearLayout rows;
  private TextView summary;
  private Button next;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    fromSettings = getIntent() != null && getIntent().hasExtra(EXTRA_FROM_SETTINGS);
    if (!fromSettings && isDone() && allSatisfied()) {
      openSettingsAndFinish();
      return;
    }
    buildUi();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (rows == null) {
      return;
    }
    refresh();
    if (waitingReturn) {
      waitingReturn = false;
      if (autoRunning) {
        handler.postDelayed(this::advance, 300);
      }
    }
  }

  @Override
  protected void onPause() {
    handler.removeCallbacksAndMessages(null);
    super.onPause();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
    super.onRequestPermissionsResult(requestCode, permissions, results);
    if (requestCode == REQ_ALL && autoRunning) {
      handler.postDelayed(this::advance, 300);
    } else if (requestCode == REQ_ONE && permissions.length > 0) {
      String p = permissions[0];
      boolean denied = checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED;
      if (denied && !shouldShowRequestPermissionRationale(p)) {
        // Android will not show the dialog any more: send the user to the app's permission page.
        Toast.makeText(this, R.string.ms_perm_after_app_info, Toast.LENGTH_LONG).show();
        launchSettings(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri()));
        waitingReturn = false;
      }
    }
  }

  // ---------------------------------------------------------------- UI

  private void buildUi() {
    ScrollView scroll = new ScrollView(this);
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    int pad = dp(16);
    root.setPadding(pad, pad, pad, pad);
    scroll.addView(root, new ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    setTitle(R.string.ms_perm_title);
    TextView title = text(getString(R.string.ms_perm_title), 24);
    if (Build.VERSION.SDK_INT >= 28) {
      title.setAccessibilityHeading(true);
    }
    root.addView(title);
    root.addView(text(getString(R.string.ms_perm_intro), 16));

    Button grantAll = button(getString(R.string.ms_perm_grant_all));
    grantAll.setOnClickListener(v -> startAutoFlow());
    root.addView(grantAll);

    Button check = button(getString(R.string.ms_perm_check));
    check.setOnClickListener(v -> {
      refresh();
      summary.announceForAccessibility(summary.getText());
    });
    root.addView(check);

    summary = text("", 16);
    summary.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    root.addView(summary);

    rows = new LinearLayout(this);
    rows.setOrientation(LinearLayout.VERTICAL);
    root.addView(rows);

    next = button(getString(R.string.ms_perm_continue));
    next.setVisibility(View.GONE);
    next.setOnClickListener(v -> {
      markDone();
      if (fromSettings) {
        finish();
      } else {
        openSettingsAndFinish();
      }
    });
    root.addView(next);

    root.addView(text(getString(R.string.ms_perm_restricted_help), 14));
    Button restricted = button(getString(R.string.ms_perm_restricted));
    restricted.setOnClickListener(v -> openAppInfo());
    root.addView(restricted);

    if (!fromSettings) {
      Button exit = button(getString(R.string.ms_perm_exit));
      exit.setOnClickListener(v -> finishAffinity());
      root.addView(exit);
    }

    setContentView(scroll);
    refresh();
  }

  private TextView text(String s, int sp) {
    TextView t = new TextView(this);
    t.setText(s);
    t.setTextSize(sp);
    t.setPadding(0, dp(8), 0, dp(8));
    return t;
  }

  private Button button(String s) {
    Button b = new Button(this);
    b.setText(s);
    b.setAllCaps(false);
    return b;
  }

  private int dp(int v) {
    return (int) (v * getResources().getDisplayMetrics().density);
  }

  private void refresh() {
    rows.removeAllViews();
    int missing = 0;
    for (Row r : allRows()) {
      boolean ok = isGranted(r);
      addRow(r, ok);
      if (!ok) {
        missing++;
      }
    }
    if (next != null) {
      next.setVisibility(missing == 0 ? View.VISIBLE : View.GONE);
    }
    if (missing == 0) {
      summary.setText(R.string.ms_perm_summary_all);
    } else {
      summary.setText(getString(R.string.ms_perm_summary_missing, missing, ""));
    }
  }

  private void addRow(Row r, boolean ok) {
    LinearLayout box = new LinearLayout(this);
    box.setOrientation(LinearLayout.VERTICAL);
    box.setPadding(0, dp(6), 0, dp(6));
    String label = getString(r.label);
    String state = getString(ok ? R.string.ms_perm_granted : R.string.ms_perm_missing);
    box.addView(text(getString(R.string.ms_perm_row, label, state), 16));
    box.addView(text(getString(r.why), 14));
    if (!ok) {
      Button give = button(getString(R.string.ms_perm_give));
      give.setContentDescription(getString(R.string.ms_perm_give_desc, label));
      give.setOnClickListener(v -> grantOne(r));
      box.addView(give);
    }
    rows.addView(box);
  }

  // ---------------------------------------------------------------- rows and checks

  private List<Row> allRows() {
    List<Row> l = new ArrayList<>();
    l.add(new Row(R.string.ms_perm_phone_state, R.string.ms_perm_why_phone_state,
        Manifest.permission.READ_PHONE_STATE, null));
    l.add(new Row(R.string.ms_perm_answer_calls, R.string.ms_perm_why_answer_calls,
        Manifest.permission.ANSWER_PHONE_CALLS, null));
    l.add(new Row(R.string.ms_perm_call_log, R.string.ms_perm_why_call_log,
        Manifest.permission.READ_CALL_LOG, null));
    l.add(new Row(R.string.ms_perm_contacts, R.string.ms_perm_why_contacts,
        Manifest.permission.READ_CONTACTS, null));
    l.add(new Row(R.string.ms_perm_mic, R.string.ms_perm_why_mic,
        Manifest.permission.RECORD_AUDIO, null));
    if (Build.VERSION.SDK_INT >= 33) {
      l.add(new Row(R.string.ms_perm_notifications, R.string.ms_perm_why_notifications,
          "android.permission.POST_NOTIFICATIONS", null));
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
      l.add(new Row(R.string.ms_perm_storage, R.string.ms_perm_why_storage,
          Manifest.permission.READ_EXTERNAL_STORAGE, null));
      l.add(new Row(R.string.ms_perm_storage, R.string.ms_perm_why_storage,
          Manifest.permission.WRITE_EXTERNAL_STORAGE, null));
    } else {
      l.add(new Row(R.string.ms_perm_all_files, R.string.ms_perm_why_storage, null, K_FILES));
    }
    l.add(new Row(R.string.ms_perm_battery, R.string.ms_perm_why_battery, null, K_BATTERY));
    l.add(new Row(R.string.ms_perm_accessibility, R.string.ms_perm_why_accessibility, null, K_ACC));
    return l;
  }

  private boolean isGranted(Row r) {
    if (r.perm != null) {
      return checkSelfPermission(r.perm) == PackageManager.PERMISSION_GRANTED;
    }
    switch (r.kind) {
      case K_FILES:
        return allFilesOk();
      case K_BATTERY:
        return batteryOk();
      default:
        return accessibilityOn();
    }
  }

  private List<String> missingRuntime() {
    List<String> l = new ArrayList<>();
    for (Row r : allRows()) {
      if (r.perm != null && !isGranted(r)) {
        l.add(r.perm);
      }
    }
    return l;
  }

  private boolean allFilesOk() {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager();
  }

  private boolean batteryOk() {
    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    return pm == null || pm.isIgnoringBatteryOptimizations(getPackageName());
  }

  private boolean accessibilityOn() {
    String enabled =
        Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
    if (TextUtils.isEmpty(enabled)) {
      return false;
    }
    ComponentName mine = new ComponentName(getPackageName(), SERVICE_CLASS);
    for (String s : enabled.split(":")) {
      ComponentName c = ComponentName.unflattenFromString(s);
      if (c != null && c.equals(mine)) {
        return true;
      }
    }
    return false;
  }

  private boolean allSatisfied() {
    for (Row r : allRows()) {
      if (!isGranted(r)) {
        return false;
      }
    }
    return true;
  }

  // ---------------------------------------------------------------- flow

  /** Per-row "दें" button. Manual: no auto-advance afterwards. */
  private void grantOne(Row r) {
    autoRunning = false;
    if (r.perm != null) {
      requestPermissions(new String[] {r.perm}, REQ_ONE);
    } else if (K_FILES.equals(r.kind)) {
      launchFiles();
    } else if (K_BATTERY.equals(r.kind)) {
      launchBattery();
    } else {
      launchSettings(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }
    waitingReturn = false;
  }

  private void launchFiles() {
    Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, packageUri());
    if (!launchSettings(i)) {
      launchSettings(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
    }
  }

  private void launchBattery() {
    Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri());
    if (!launchSettings(i)) {
      launchSettings(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
    }
  }

  private void startAutoFlow() {
    attempted.clear();
    autoRunning = true;
    advance();
  }

  /** Does the next missing step. Each step is tried once per run, so it can never loop. */
  private void advance() {
    if (isFinishing()) {
      return;
    }
    if (allSatisfied()) {
      autoRunning = false;
      refresh();
      return;
    }
    List<String> miss = missingRuntime();
    if (!miss.isEmpty()) {
      if (attempted.add("runtime")) {
        requestPermissions(miss.toArray(new String[0]), REQ_ALL);
        return;
      }
      if (attempted.add("appinfo")) {
        Toast.makeText(this, R.string.ms_perm_after_app_info, Toast.LENGTH_LONG).show();
        launchSettings(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri()));
        return;
      }
    }
    if (!allFilesOk() && attempted.add(K_FILES)) {
      launchFiles();
      return;
    }
    if (!batteryOk() && attempted.add(K_BATTERY)) {
      launchBattery();
      return;
    }
    if (!accessibilityOn() && attempted.add(K_ACC)) {
      launchSettings(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
      return;
    }
    // Nothing more can be done automatically; leave the list on screen.
    autoRunning = false;
    refresh();
    summary.announceForAccessibility(summary.getText());
  }

  private boolean launchSettings(Intent i) {
    try {
      waitingReturn = true;
      startActivity(i);
      return true;
    } catch (ActivityNotFoundException | SecurityException e) {
      waitingReturn = false;
      return false;
    }
  }

  private void openAppInfo() {
    if (!launchSettings(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri()))) {
      Toast.makeText(this, R.string.ms_perm_open_failed, Toast.LENGTH_SHORT).show();
    }
    waitingReturn = false; // manual button: do not auto-advance on return
  }

  private Uri packageUri() {
    return Uri.parse("package:" + getPackageName());
  }

  // ---------------------------------------------------------------- done

  private SharedPreferences prefs() {
    return getSharedPreferences(PREFS, MODE_PRIVATE);
  }

  private boolean isDone() {
    return prefs().getBoolean(KEY_DONE, false);
  }

  private void markDone() {
    prefs().edit().putBoolean(KEY_DONE, true).apply();
  }

  private void openSettingsAndFinish() {
    startActivity(new Intent(this, TalkBackPreferencesActivity.class));
    finish();
  }
}
