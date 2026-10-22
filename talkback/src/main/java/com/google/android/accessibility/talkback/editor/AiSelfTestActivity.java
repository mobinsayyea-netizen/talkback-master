package com.google.android.accessibility.talkback.editor;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Self test for AI Create, started by the build workflow on an emulator (adb shell am start).
 * It compiles a sample Java extension and a broken one on the device, loads the good one and runs
 * its event entry. Result lines go to logcat with the tag MsSelfTest ("RESULT: PASS" or FAIL).
 */
public class AiSelfTestActivity extends Activity {
  private static final String TAG = "MsSelfTest";

  private static final String GOOD =
      "import ms.api.MsApi;\n"
          + "import ms.api.MsPlugin;\n"
          + "import java.util.ArrayList;\n"
          + "import java.util.List;\n"
          + "public class Main extends MsPlugin {\n"
          + "  public static String seen = \"\";\n"
          + "  @Override\n"
          + "  public void onEvent(MsApi api, String type, String pkg, String text) {\n"
          + "    List<String> l = new ArrayList<>();\n"
          + "    l.add(type);\n"
          + "    Runnable r = new Runnable() { public void run() { seen = \"ran\"; } };\n"
          + "    r.run();\n"
          + "    seen = seen + \":\" + l.get(0) + \":\" + pkg + \":\" + text;\n"
          + "  }\n"
          + "}\n";

  private static final String BAD =
      "import ms.api.MsPlugin;\n"
          + "public class Main extends MsPlugin {\n"
          + "  void x() { int a = \"text\"; undefinedCall(); }\n"
          + "}\n";

  @Override
  protected void onCreate(Bundle b) {
    super.onCreate(b);
    new Thread(
            new Runnable() {
              @Override
              public void run() {
                String result;
                try {
                  result = test();
                } catch (Throwable t) {
                  result = "FAIL exception " + Log.getStackTraceString(t);
                }
                Log.i(TAG, "RESULT: " + (result == null ? "PASS" : result));
                finish();
              }
            })
        .start();
  }

  private String test() throws Exception {
    long t0 = System.currentTimeMillis();
    File dir = new File(getCacheDir(), "selftest/Sample");
    dir.mkdirs();
    File jar = new File(dir, "plugin.jar");
    String err = PluginCompiler.compile(this, GOOD, jar);
    Log.i(TAG, "good compile: " + (err == null ? "ok" : err) + " (" + (System.currentTimeMillis() - t0) + " ms)");
    if (err != null) return "FAIL good source did not compile: " + err;
    if (!jar.isFile() || jar.length() < 100) return "FAIL plugin.jar missing or empty";

    String bad = PluginCompiler.compile(this, BAD, new File(dir, "bad.jar"));
    Log.i(TAG, "bad compile: " + (bad == null ? "NO ERROR" : bad.replace('\n', ' ')));
    if (bad == null) return "FAIL broken source compiled without error";
    if (!bad.contains("incompatible") && !bad.contains("undefined") && !bad.contains("ERROR")) {
      return "FAIL broken source error text looks wrong: " + bad;
    }

    try (FileWriter w = new FileWriter(new File(dir, "main.lua"))) {
      w.write("-- title: Sample\n-- menu: on\n-- java: Main\n");
    }
    Map<String, String> extras = new HashMap<>();
    extras.put("event_type", "app_opened");
    extras.put("event_package", "pkg.test");
    extras.put("event_text", "hello");
    String runErr = JavaPluginRunner.run(this, new File(dir, "main.lua"), null, extras);
    Log.i(TAG, "run: " + (runErr == null ? "ok" : runErr));
    if (runErr != null) return "FAIL run: " + runErr;

    // Second compile of the same text must also work (dx state is reused inside one process).
    File jar2 = new File(dir, "second.jar");
    String err2 = PluginCompiler.compile(this, GOOD, jar2);
    Log.i(TAG, "second compile: " + (err2 == null ? "ok" : err2));
    if (err2 != null) return "FAIL second compile: " + err2;
    return null;
  }
}
