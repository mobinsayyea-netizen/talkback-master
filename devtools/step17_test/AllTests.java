import java.io.*; import java.nio.file.*; import java.util.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class AllTests {
  static int fails=0;
  static void check(boolean ok,String m){ if(!ok){fails++;System.out.println("FAIL "+m);} }
  public static void main(String[] a) throws Exception {
    String src="local function f(x)\nif x then\nprint('end')\nelse\nfor i=1,3 do\nprint(i) -- end\nend\nend\nend\nservice.click({\n\"a\",\n{\"b\"}\n})\nwhile true do break end\nrepeat\nx=1\nuntil x\nif a then b() elseif c then d() else e() end\nlocal t = function()\nreturn 1\nend";
    String exp="local function f(x)\n  if x then\n    print('end')\n  else\n    for i=1,3 do\n      print(i) -- end\n    end\n  end\nend\nservice.click({\n  \"a\",\n  {\"b\"}\n})\nwhile true do break end\nrepeat\n  x=1\nuntil x\nif a then b() elseif c then d() else e() end\nlocal t = function()\n  return 1\nend";
    String got=LuaFormatter.format(src);
    check(got.equals(exp),"format\n"+got);
    check(LuaFormatter.format(got).equals(got),"idempotent");
    Path tmp=Files.createTempDirectory("pk"); File base=tmp.toFile();
    File ext=new File(base,"Extensions/Hello"); ext.mkdirs();
    Files.writeString(new File(ext,"main.lua").toPath(),"print('hi')");
    new File(ext,"sub").mkdirs(); Files.writeString(new File(ext,"sub/a.txt").toPath(),"x");
    File z=new File(base,"Hello.ppk");
    try(OutputStream o=new FileOutputStream(z)){ ExtensionPackages.pack(ext,"ext","Hello",o);}
    check(ExtensionPackages.peekName(z,"fb").equals("Hello"),"peek");
    File dest=new File(base,"dest"); dest.mkdirs();
    ExtensionPackages.Result r=ExtensionPackages.unpack(z,dest,"Hello2",false);
    check(!r.exists&&r.files==3,"unpack files="+r.files);
    check(new File(dest,"Hello2/main.lua").isFile()&&new File(dest,"Hello2/sub/a.txt").isFile(),"files there");
    check(ExtensionPackages.unpack(z,dest,"Hello2",false).exists,"exists flag");
    check(!ExtensionPackages.unpack(z,dest,"Hello2",true).exists,"overwrite");
    File w=new File(base,"w.xpk");
    try(java.util.zip.ZipOutputStream zo=new java.util.zip.ZipOutputStream(new FileOutputStream(w))){
      zo.putNextEntry(new java.util.zip.ZipEntry("Tool1/main.lua")); zo.write("x".getBytes()); zo.closeEntry(); }
    ExtensionPackages.unpack(w,dest,"T",false);
    check(new File(dest,"T/main.lua").isFile(),"wrapper stripped");
    File bad=new File(base,"bad.ppk");
    try(java.util.zip.ZipOutputStream zo=new java.util.zip.ZipOutputStream(new FileOutputStream(bad))){
      zo.putNextEntry(new java.util.zip.ZipEntry("main.lua")); zo.write("x".getBytes()); zo.closeEntry();
      zo.putNextEntry(new java.util.zip.ZipEntry("../evil.txt")); zo.write("x".getBytes()); zo.closeEntry(); }
    boolean threw=false; try{ExtensionPackages.unpack(bad,dest,"B",false);}catch(IOException e){threw=true;}
    check(threw&&!new File(dest,"B").exists()&&!new File(dest,"evil.txt").exists(),"zip slip blocked");
    File nm=new File(base,"nm.ppk");
    try(java.util.zip.ZipOutputStream zo=new java.util.zip.ZipOutputStream(new FileOutputStream(nm))){
      zo.putNextEntry(new java.util.zip.ZipEntry("a.lua")); zo.write("x".getBytes()); zo.closeEntry(); }
    threw=false; try{ExtensionPackages.unpack(nm,dest,"N",false);}catch(IOException e){threw=true;}
    check(threw,"no main.lua rejected");
    File vc=new File(base,"Custom voice commands"); vc.mkdirs(); Files.writeString(new File(vc,"commands.txt").toPath(),"a\text\tHello\n");
    File all=new File(base,"all.zip"); try(OutputStream o=new FileOutputStream(all)){ExtensionPackages.packAll(base,o);}
    File b2=new File(base,"b2"); b2.mkdirs();
    int n=ExtensionPackages.restoreAll(all,b2);
    check(n==3&&new File(b2,"Custom voice commands/commands.txt").isFile(),"restoreAll n="+n);
    check(ExtensionPackages.kindForFileName("A.PPK").equals("ext")&&ExtensionPackages.kindForFileName("a.xpk").equals("tool")&&ExtensionPackages.kindForFileName("a.zip")==null,"kinds");
    check("My Tool".equals(ExtensionPackages.readAppName("appname=\"My Tool\"\nappver='1'")),"appname");
    System.out.println(fails==0?"ALL PASS":"FAILS: "+fails);
  }
}
/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */



















/**
 * Packages for extensions (.ppk) and tools (.xpk), and a whole-resources backup zip. Pure Java (no
 * Android classes) so it can be tested on a computer. A package is a normal zip whose root holds
 * main.lua (and init.lua with appname/appver/ext). Jieshuo's exact private format is not
 * documented, so packages made by Jieshuo may or may not open here; ours are plain zips.
 */
final class ExtensionPackages {
  private ExtensionPackages() {}

  public static final String KIND_EXT = "ext";
  public static final String KIND_TOOL = "tool";
  private static final long MAX_TOTAL_BYTES = 60L * 1024 * 1024;
  private static final int MAX_ENTRIES = 3000;
  private static final String[] BACKUP_TOP = {"Extensions", "Tools", "Custom voice commands"};

  /** Outcome of an import. */
  public static final class Result {
    public String name;
    public boolean exists; // true = nothing was written because the name is taken
    public int files;
  }

  /** ".ppk" gives "ext", ".xpk" gives "tool", anything else null. */
  public static String kindForFileName(String fileName) {
    if (fileName == null) {
      return null;
    }
    String n = fileName.toLowerCase(java.util.Locale.ROOT);
    if (n.endsWith(".ppk")) {
      return KIND_EXT;
    }
    if (n.endsWith(".xpk")) {
      return KIND_TOOL;
    }
    return null;
  }

  public static String fileNameFor(String kind, String name) {
    return name + (KIND_TOOL.equals(kind) ? ".xpk" : ".ppk");
  }

  public static String baseNameOf(String fileName) {
    String n = fileName == null ? "" : fileName;
    int slash = Math.max(n.lastIndexOf('/'), n.lastIndexOf('\\'));
    if (slash >= 0) {
      n = n.substring(slash + 1);
    }
    int dot = n.lastIndexOf('.');
    if (dot > 0) {
      n = n.substring(0, dot);
    }
    return cleanName(n);
  }

  public static String cleanName(String s) {
    String r = s == null ? "" : s.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
    return r.equals(".") || r.equals("..") ? "" : r;
  }

  /** Text of init.lua written when the folder has none. */
  public static String defaultInit(String kind, String name) {
    return "appname=\""
        + name.replace("\\", "\\\\").replace("\"", "\\\"")
        + "\"\nappver=\"1.0\"\next=\""
        + (KIND_TOOL.equals(kind) ? "xpk" : "ppk")
        + "\"\n";
  }

  /** Reads appname="..." from init.lua text; null when absent. */
  public static String readAppName(String initLua) {
    if (initLua == null) {
      return null;
    }
    Matcher m = Pattern.compile("appname\\s*=\\s*[\"']([^\"'\\r\\n]+)[\"']").matcher(initLua);
    if (m.find()) {
      String n = cleanName(m.group(1));
      return n.isEmpty() ? null : n;
    }
    return null;
  }

  // ----------------------------------------------------------------------- export

  /** Writes the folder as a package. Adds init.lua when the folder has none. */
  public static void pack(File dir, String kind, String name, OutputStream out)
      throws IOException {
    ZipOutputStream zip = new ZipOutputStream(out);
    boolean hasInit = new File(dir, "init.lua").isFile();
    addTree(zip, dir, "");
    if (!hasInit) {
      zip.putNextEntry(new ZipEntry("init.lua"));
      zip.write(defaultInit(kind, name).getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
    }
    zip.finish();
    zip.flush();
  }

  private static void addTree(ZipOutputStream zip, File f, String prefix) throws IOException {
    File[] kids = f.listFiles();
    if (kids == null) {
      return;
    }
    Arrays.sort(kids);
    for (File k : kids) {
      String path = prefix + k.getName();
      if (k.isDirectory()) {
        addTree(zip, k, path + "/");
      } else {
        zip.putNextEntry(new ZipEntry(path));
        try (InputStream in = new FileInputStream(k)) {
          copy(in, zip);
        }
        zip.closeEntry();
      }
    }
  }

  /** Zip of Extensions, Tools and Custom voice commands under the base folder. */
  public static void packAll(File base, OutputStream out) throws IOException {
    ZipOutputStream zip = new ZipOutputStream(out);
    for (String top : BACKUP_TOP) {
      File f = new File(base, top);
      if (f.isDirectory()) {
        addTree(zip, f, top + "/");
      }
    }
    zip.finish();
    zip.flush();
  }

  // ----------------------------------------------------------------------- import

  /** Reads init.lua appname inside a package; fallback when none. */
  public static String peekName(File zipFile, String fallback) {
    try (ZipInputStream z = new ZipInputStream(new FileInputStream(zipFile))) {
      ZipEntry e;
      while ((e = z.getNextEntry()) != null) {
        String n = e.getName().replace('\\', '/');
        if (!e.isDirectory() && (n.equals("init.lua") || n.endsWith("/init.lua"))) {
          if (n.indexOf('/') == n.lastIndexOf('/') || n.equals("init.lua")) {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            copy(z, b);
            String name = readAppName(new String(b.toByteArray(), StandardCharsets.UTF_8));
            if (name != null) {
              return name;
            }
          }
        }
      }
    } catch (IOException ignored) {
      // fall through
    }
    String f = cleanName(fallback);
    return f.isEmpty() ? "Imported" : f;
  }

  /**
   * Unpacks a package into baseDir/name. If the folder exists and overwrite is false nothing is
   * written and Result.exists is true. Throws IOException for a bad package (no main.lua, unsafe
   * path, too big).
   */
  public static Result unpack(File zipFile, File baseDir, String name, boolean overwrite)
      throws IOException {
    Result r = new Result();
    r.name = cleanName(name);
    if (r.name.isEmpty()) {
      throw new IOException("Empty name");
    }
    File target = new File(baseDir, r.name);
    if (target.exists() && !overwrite) {
      r.exists = true;
      return r;
    }
    // Pass 1: look at the paths to find a single wrapping folder and main.lua.
    List<String> names = new ArrayList<>();
    try (ZipInputStream z = new ZipInputStream(new FileInputStream(zipFile))) {
      ZipEntry e;
      while ((e = z.getNextEntry()) != null) {
        if (names.size() >= MAX_ENTRIES) {
          throw new IOException("Too many files");
        }
        names.add(e.getName().replace('\\', '/'));
      }
    }
    String strip = wrapperFolder(names);
    boolean hasMain = false;
    for (String n : names) {
      if (n.equals(strip + "main.lua")) {
        hasMain = true;
      }
    }
    if (!hasMain) {
      throw new IOException("This package has no main.lua");
    }
    File tmp = new File(baseDir, ".import_" + System.nanoTime());
    boolean ok = false;
    try {
      long total = 0;
      try (ZipInputStream z = new ZipInputStream(new FileInputStream(zipFile))) {
        ZipEntry e;
        while ((e = z.getNextEntry()) != null) {
          String n = e.getName().replace('\\', '/');
          if (e.isDirectory() || !n.startsWith(strip)) {
            continue;
          }
          String rel = n.substring(strip.length());
          if (rel.isEmpty()) {
            continue;
          }
          File out = safeFile(tmp, rel);
          out.getParentFile().mkdirs();
          try (OutputStream o = new FileOutputStream(out)) {
            total += copyLimited(z, o, MAX_TOTAL_BYTES - total);
          }
          r.files++;
        }
      }
      if (target.exists()) {
        deleteRecursive(target);
      }
      if (!tmp.renameTo(target)) {
        throw new IOException("Could not create the folder");
      }
      ok = true;
    } finally {
      if (!ok) {
        deleteRecursive(tmp);
      }
    }
    return r;
  }

  /** Restores a packAll zip. Only the three known top folders are accepted; files are replaced. */
  public static int restoreAll(File zipFile, File base) throws IOException {
    int count = 0;
    long total = 0;
    try (ZipInputStream z = new ZipInputStream(new FileInputStream(zipFile))) {
      ZipEntry e;
      while ((e = z.getNextEntry()) != null) {
        String n = e.getName().replace('\\', '/');
        boolean allowed = false;
        for (String top : BACKUP_TOP) {
          if (n.startsWith(top + "/")) {
            allowed = true;
          }
        }
        if (!allowed || e.isDirectory()) {
          continue;
        }
        if (count >= MAX_ENTRIES) {
          throw new IOException("Too many files");
        }
        File out = safeFile(base, n);
        out.getParentFile().mkdirs();
        try (OutputStream o = new FileOutputStream(out)) {
          total += copyLimited(z, o, MAX_TOTAL_BYTES - total);
        }
        count++;
      }
    }
    return count;
  }

  /** "Name/" when every entry sits inside one folder and main.lua is not at the root, else "". */
  static String wrapperFolder(List<String> names) {
    String first = null;
    for (String n : names) {
      if (n.equals("main.lua")) {
        return "";
      }
      int s = n.indexOf('/');
      if (s <= 0) {
        return "";
      }
      String top = n.substring(0, s + 1);
      if (first == null) {
        first = top;
      } else if (!first.equals(top)) {
        return "";
      }
    }
    return first == null ? "" : first;
  }

  private static File safeFile(File root, String rel) throws IOException {
    File out = new File(root, rel);
    String rootPath = root.getCanonicalPath() + File.separator;
    if (!out.getCanonicalPath().startsWith(rootPath)) {
      throw new IOException("Unsafe path in package");
    }
    return out;
  }

  // ----------------------------------------------------------------------- helpers

  public static void deleteRecursive(File f) {
    File[] kids = f.listFiles();
    if (kids != null) {
      for (File k : kids) {
        deleteRecursive(k);
      }
    }
    f.delete();
  }

  private static void copy(InputStream in, OutputStream out) throws IOException {
    byte[] buf = new byte[8192];
    int n;
    while ((n = in.read(buf)) > 0) {
      out.write(buf, 0, n);
    }
  }

  private static long copyLimited(InputStream in, OutputStream out, long limit)
      throws IOException {
    byte[] buf = new byte[8192];
    long done = 0;
    int n;
    while ((n = in.read(buf)) > 0) {
      done += n;
      if (done > limit) {
        throw new IOException("Package is too big");
      }
      out.write(buf, 0, n);
    }
    return done;
  }
}

/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */





/** "Format source code" for the Lua editor: re-indents by block keywords. Pure Java. */
final class LuaFormatter {
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
