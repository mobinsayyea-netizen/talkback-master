package com.google.android.accessibility.talkback.editor;

import android.content.Context;
import android.content.pm.PackageInfo;
import com.android.dx.command.dexer.DxContext;
import com.android.dx.command.dexer.Main;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.eclipse.jdt.core.compiler.batch.BatchCompiler;

/**
 * Compiles one Java source file (class Main) on the phone: ECJ makes the .class files, dx turns
 * them into plugin.jar (with classes.dex). The plugin API classes (ms.api) are compiled together
 * but left out of the jar, because the app already has them.
 */
final class PluginCompiler {
  private PluginCompiler() {}

  /** Returns null on success, or the error text (suitable to send back to Gemini). */
  static synchronized String compile(Context ctx, String code, File outJar) {
    File tmp = new File(ctx.getCacheDir(), "msc" + System.nanoTime());
    try {
      File root = prepare(ctx);
      File src = new File(tmp, "src");
      File classes = new File(tmp, "classes");
      src.mkdirs();
      classes.mkdirs();
      File main = new File(src, "Main.java");
      write(main, code);
      StringWriter out = new StringWriter();
      StringWriter err = new StringWriter();
      String[] args = {
        "-1.7",
        "-nowarn",
        "-proc:none",
        "-encoding",
        "UTF-8",
        "-cp",
        new File(root, "android.jar").getAbsolutePath(),
        "-d",
        classes.getAbsolutePath(),
        main.getAbsolutePath(),
        new File(root, "ms/api/MsApi.java").getAbsolutePath(),
        new File(root, "ms/api/MsPlugin.java").getAbsolutePath()
      };
      boolean ok =
          BatchCompiler.compile(args, new PrintWriter(out), new PrintWriter(err), null);
      if (!ok) {
        String text = (err.toString() + "\n" + out.toString()).trim();
        text = text.replace(src.getAbsolutePath() + "/", "").replace(root.getAbsolutePath() + "/", "");
        return text.isEmpty() ? "Compile failed with no message" : text;
      }
      deleteRecursive(new File(classes, "ms"));
      File jar = new File(tmp, "plugin.jar");
      ByteArrayOutputStream dxOut = new ByteArrayOutputStream();
      DxContext dx = new DxContext(dxOut, dxOut);
      Main.Arguments a = new Main.Arguments(dx);
      a.parse(new String[] {"--output=" + jar.getAbsolutePath(), classes.getAbsolutePath()});
      int rc = new Main(dx).runDx(a);
      if (rc != 0 || !jar.isFile()) {
        return "Dex conversion failed (code " + rc + "). " + dxOut.toString().trim();
      }
      if (outJar.exists()) {
        outJar.setWritable(true);
        outJar.delete();
      }
      copy(jar, outJar);
      return null;
    } catch (Throwable t) {
      com.google.android.accessibility.talkback.MsErrorLog.log("PluginCompiler", t);
      return "Compiler problem: " + t;
    } finally {
      deleteRecursive(tmp);
    }
  }

  /** Copies the compiler files from the app assets once per app version. */
  private static File prepare(Context ctx) throws Exception {
    File root = new File(ctx.getFilesDir(), "ms_compiler");
    PackageInfo pi = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
    String version = pi.versionName + "/" + pi.lastUpdateTime;
    File marker = new File(root, "ready.txt");
    boolean ready = false;
    if (marker.isFile()) {
      byte[] b = new byte[(int) marker.length()];
      try (FileInputStream in = new FileInputStream(marker)) {
        in.read(b);
      }
      ready = version.equals(new String(b, "UTF-8")) && new File(root, "android.jar").isFile();
    }
    if (!ready) {
      root.mkdirs();
      new File(root, "ms/api").mkdirs();
      assetTo(ctx, "ms_compiler/android.jar", new File(root, "android.jar"));
      assetTo(ctx, "ms_compiler/MsApi.java", new File(root, "ms/api/MsApi.java"));
      assetTo(ctx, "ms_compiler/MsPlugin.java", new File(root, "ms/api/MsPlugin.java"));
      write(marker, version);
    }
    return root;
  }

  private static void assetTo(Context ctx, String asset, File to) throws Exception {
    try (InputStream in = ctx.getAssets().open(asset);
        OutputStream out = new FileOutputStream(to)) {
      byte[] b = new byte[65536];
      int n;
      while ((n = in.read(b)) > 0) {
        out.write(b, 0, n);
      }
    }
  }

  private static void write(File f, String text) throws Exception {
    try (FileWriter w = new FileWriter(f)) {
      w.write(text);
    }
  }

  private static void copy(File from, File to) throws Exception {
    try (InputStream in = new FileInputStream(from);
        OutputStream out = new FileOutputStream(to)) {
      byte[] b = new byte[65536];
      int n;
      while ((n = in.read(b)) > 0) {
        out.write(b, 0, n);
      }
    }
  }

  private static void deleteRecursive(File f) {
    if (f == null || !f.exists()) return;
    File[] kids = f.listFiles();
    if (kids != null) {
      for (File k : kids) deleteRecursive(k);
    }
    f.delete();
  }
}
