package com.google.android.accessibility.talkback.editor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.android.accessibility.talkback.actor.gemini.GeminiKeyStore;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chat with Gemini that designs and then builds a Java extension or tool. Talk first; when the
 * user says to build ("बनाओ", "build"), Gemini writes Main.java, the phone compiles it, and compile
 * errors go back to Gemini automatically (up to 4 tries). "ठीक करो" / "fix" repairs the last one,
 * using the last run error. The result is the folder NAME with main.lua (header only), Main.java
 * and plugin.jar inside Extensions or Tools.
 */
public class AiChatActivity extends Activity {
  private static final String KIND = "kind";
  private static final int MAX_TRIES = 4;

  public static void show(Context c, String kind) {
    Intent i = new Intent(c, AiChatActivity.class);
    i.putExtra(KIND, kind);
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    c.startActivity(i);
  }

  private final Handler ui = new Handler(Looper.getMainLooper());
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private final List<String[]> history = new ArrayList<>();
  private LinearLayout messages;
  private ScrollView scroll;
  private EditText input;
  private Button send;
  private boolean tool;
  private boolean busy;
  private String system;
  private String lastCode;
  private String lastName;
  private File lastFolder;

  @Override
  protected void onCreate(Bundle b) {
    super.onCreate(b);
    tool = "tool".equals(getIntent().getStringExtra(KIND));
    setTitle(tool ? "Create Tool" : "Create Extension");
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    int pad = (int) (10 * getResources().getDisplayMetrics().density);
    root.setPadding(pad, pad, pad, pad);

    TextView head = new TextView(this);
    head.setText(tool ? "Create Tool" : "Create Extension");
    head.setTextSize(20);
    head.setFocusable(true);
    root.addView(head);

    scroll = new ScrollView(this);
    messages = new LinearLayout(this);
    messages.setOrientation(LinearLayout.VERTICAL);
    scroll.addView(messages);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

    input = new EditText(this);
    input.setHint("Type or dictate here");
    input.setMinLines(2);
    input.setMaxLines(6);
    input.setImeOptions(EditorInfo.IME_ACTION_SEND);
    input.setOnEditorActionListener(
        (v, actionId, ev) -> {
          if (actionId == EditorInfo.IME_ACTION_SEND) {
            onSend();
            return true;
          }
          return false;
        });
    root.addView(input, new LinearLayout.LayoutParams(-1, -2));

    send = new Button(this);
    send.setText("Send");
    send.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            onSend();
          }
        });
    root.addView(send, new LinearLayout.LayoutParams(-1, -2));
    setContentView(root);

    system = buildSystemPrompt();
    String kindWord = tool ? "tool" : "extension";
    if (GeminiKeyStore.key(this).isEmpty()) {
      addMsg(
          "Gemini key is not set. Paste your Gemini API key here and press Send, I will save it for the screen reader.",
          false);
    } else {
      addMsg(
          "Tell me what the "
              + kindWord
              + " should do. We will talk first. When everything is clear, say \"बनाओ\" (build) and I will make it. Later say \"ठीक करो\" (fix) if it gives an error.",
          false);
    }
  }

  private String buildSystemPrompt() {
    String api = readAsset("ms_compiler/API.txt");
    return "You are AI Create inside MS Screen Reader, an Android screen reader used by a blind person who works only on a phone. "
        + "You help design and write a Java "
        + (tool ? "tool" : "extension")
        + " for it. Talk warmly and briefly in the language the user writes (Hindi by default). "
        + "Discuss first: ask at most one short question at a time, and never print full code until you are told to build.\n\n"
        + "ABOUT THE PLUGIN\n"
        + "- One file Main.java, no package line, `public class Main extends MsPlugin`, with `import ms.api.MsApi; import ms.api.MsPlugin;`.\n"
        + "- Java 7 syntax ONLY: no lambdas, no method references, no streams, no default or static interface methods, no var. Use anonymous classes. Diamond <> and try-with-resources are fine.\n"
        + "- All Android classes can be imported normally. Network and file work are allowed. Catch exceptions yourself.\n"
        + "- Methods you may override: run(MsApi api, String text) for the menu; onEvent(MsApi api, String type, String pkg, String text) for events (window_changed, app_opened, notification, service_started); hooks onWindowStateChanged/onNotification/onScrolled/onViewLongClicked(api, pkg, text), boolean onKeyEvent(api, KeyEvent), boolean onGestureDetected(api, int gestureId), onVolumeChanged(api, int streamType, int value, int previous, int percent). Each runs on a background thread. Never block for long.\n"
        + "- Talk to the user with api.speak(text). Ask Gemini with api.gemini(prompt). Click with api.click(\"text\") or api.toClick(node). Run any screen reader command with api.execute(\"name\").\n"
        + "- Use ONLY the methods listed below on `api`; do not invent others.\n\n"
        + "THE api OBJECT (type MsApi)\n"
        + api;
  }

  private String readAsset(String name) {
    try (InputStream in = getAssets().open(name)) {
      java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
      byte[] buf = new byte[8192];
      int n;
      while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
      return bo.toString("UTF-8");
    } catch (Exception e) {
      return "";
    }
  }

  private void addMsg(final String text, final boolean mine) {
    Runnable r =
        new Runnable() {
          @Override
          public void run() {
            TextView t = new TextView(AiChatActivity.this);
            t.setText((mine ? "You: " : "AI: ") + text);
            t.setTextSize(16);
            t.setTextIsSelectable(true);
            t.setFocusable(true);
            t.setPadding(0, 10, 0, 10);
            if (!mine) {
              t.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            }
            messages.addView(t);
            scroll.post(
                new Runnable() {
                  @Override
                  public void run() {
                    scroll.fullScroll(View.FOCUS_DOWN);
                  }
                });
          }
        };
    if (Looper.myLooper() == Looper.getMainLooper()) r.run();
    else ui.post(r);
  }

  private void setBusy(final boolean on) {
    busy = on;
    ui.post(
        new Runnable() {
          @Override
          public void run() {
            send.setEnabled(!on);
          }
        });
  }

  private void onSend() {
    final String text = input.getText().toString().trim();
    if (text.isEmpty() || busy) return;
    input.setText("");
    if (GeminiKeyStore.key(this).isEmpty()) {
      if (text.matches("AIza[\\w-]{20,}")) {
        GeminiKeyStore.setKey(this, text);
        addMsg("Key saved. Now tell me what to make.", false);
      } else {
        addMsg("That does not look like a Gemini key (it starts with AIza). Please paste it again.", false);
      }
      return;
    }
    addMsg(text, true);
    final boolean fix = wants(text, new String[] {"ठीक करो", "ठीक कर दो", "सुधारो", "सुधार दो", "fix"});
    final boolean build = !fix && wants(text, new String[] {"बनाओ", "बना दो", "बना दे", "बनादो", "बना दीजिए", "build", "create it", "make it", "generate"});
    setBusy(true);
    worker.execute(
        new Runnable() {
          @Override
          public void run() {
            try {
              if (fix) {
                doFix(text);
              } else if (build) {
                history.add(new String[] {"user", text});
                doBuild(null);
              } else {
                history.add(new String[] {"user", text});
                String reply = GeminiClient.chat(AiChatActivity.this, system, history);
                history.add(new String[] {"model", reply});
                addMsg(reply, false);
              }
            } catch (Exception e) {
              addMsg("Problem: " + e.getMessage(), false);
            } finally {
              setBusy(false);
            }
          }
        });
  }

  private static boolean wants(String text, String[] words) {
    String l = text.toLowerCase();
    for (String w : words) {
      if (l.contains(w.toLowerCase())) return true;
    }
    return false;
  }

  private void doFix(String userText) throws Exception {
    if (lastCode == null) {
      addMsg("Nothing has been built in this chat yet. Describe it and say \"बनाओ\" first.", false);
      return;
    }
    String err = "";
    if (lastFolder != null) {
      File f = new File(lastFolder, "last_error.txt");
      if (f.isFile()) err = readFile(f);
    }
    String note = "The current code is below. The user says: " + userText + "\n";
    if (!err.isEmpty()) {
      note += "Error from the last run on the phone:\n" + err + "\n";
    }
    note += "\nCURRENT CODE:\n" + lastCode;
    history.add(new String[] {"user", note});
    doBuild(null);
  }

  private static final String FORMAT =
      "Now build it. Output EXACTLY this format and nothing else (no explanation, no code fences):\n"
          + "===NAME===\nshort folder name, letters digits and spaces, max 30\n"
          + "===TITLE===\nname shown in the menu\n"
          + "===MENU===\non or off\n"
          + "===EVENTS===\ncomma separated from window_changed, app_opened, notification, service_started; or none\n"
          + "===HOOKS===\ncomma separated hook names to enable; or none\n"
          + "===CODE===\nthe complete Main.java\n"
          + "===END===";

  private void doBuild(String unused) throws Exception {
    List<String[]> turns = new ArrayList<>(history);
    turns.add(new String[] {"user", FORMAT});
    for (int attempt = 1; attempt <= MAX_TRIES; attempt++) {
      addMsg(attempt == 1 ? "Writing the code..." : "Fixing the error, try " + attempt + " of " + MAX_TRIES + "...", false);
      String reply = GeminiClient.chat(this, system, turns);
      Map parsed = parse(reply);
      if (parsed == null) {
        turns.add(new String[] {"model", reply});
        turns.add(new String[] {"user", "That was not in the required format. " + FORMAT});
        continue;
      }
      addMsg("Compiling on the phone...", false);
      File tmpJar = new File(getCacheDir(), "new_plugin.jar");
      String err = PluginCompiler.compile(this, parsed.code, tmpJar);
      if (err == null) {
        String where = save(parsed, tmpJar);
        history.add(new String[] {"model", "Built " + parsed.title + " (" + parsed.name + ")."});
        addMsg("Done. \"" + parsed.title + "\" is saved in " + where + ".", false);
        if (!tool && "on".equals(parsed.menu)) {
          addMsg("It is in the Extensions group of the screen reader menu.", false);
        }
        if (!"none".equals(parsed.events) || !"none".equals(parsed.hooks)) {
          addMsg("It runs on its own for: " + parsed.events + " " + parsed.hooks, false);
        }
        return;
      }
      if (err.length() > 4000) err = err.substring(0, 4000);
      turns.add(new String[] {"model", reply});
      turns.add(new String[] {"user", "The compiler gave these errors:\n" + err + "\n\nFix them. " + FORMAT});
    }
    addMsg("I could not get it to compile after " + MAX_TRIES + " tries. Tell me what to change, or say \"बनाओ\" again.", false);
  }

  private static final class Map {
    String name, title, menu, events, hooks, code;
  }

  private static Map parse(String text) {
    Pattern p =
        Pattern.compile(
            "===NAME===\\s*(.*?)\\s*===TITLE===\\s*(.*?)\\s*===MENU===\\s*(.*?)\\s*===EVENTS===\\s*(.*?)\\s*===HOOKS===\\s*(.*?)\\s*===CODE===\\s*(.*?)\\s*===END===",
            Pattern.DOTALL);
    Matcher m = p.matcher(text);
    if (!m.find()) return null;
    Map r = new Map();
    r.name = m.group(1).replaceAll("[^A-Za-z0-9 _-]", "").trim();
    if (r.name.isEmpty()) r.name = "Plugin";
    if (r.name.length() > 30) r.name = r.name.substring(0, 30).trim();
    r.title = m.group(2).trim().replace('\n', ' ');
    if (r.title.isEmpty()) r.title = r.name;
    r.menu = m.group(3).trim().toLowerCase().startsWith("off") ? "off" : "on";
    r.events = clean(m.group(4));
    r.hooks = clean(m.group(5));
    String code = m.group(6).trim();
    code = code.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("\\s*```$", "");
    r.code = code;
    return r.code.isEmpty() ? null : r;
  }

  private static String clean(String s) {
    s = s.trim().replace('\n', ',').replaceAll("[^A-Za-z_,]", "");
    return s.isEmpty() || s.equalsIgnoreCase("none") ? "none" : s;
  }

  private String save(Map m, File jar) throws Exception {
    File base = tool ? LuaPaths.tools(this) : LuaPaths.extensions(this);
    String name = m.name;
    if (!name.equals(lastName)) {
      int n = 2;
      while (new File(base, name).exists()) name = m.name + "-" + (n++);
    }
    File dir = new File(base, name);
    dir.mkdirs();
    File jarOut = new File(dir, "plugin.jar");
    if (jarOut.exists()) {
      jarOut.setWritable(true);
      jarOut.delete();
    }
    copy(jar, jarOut);
    jarOut.setReadOnly();
    write(new File(dir, "Main.java"), m.code);
    StringBuilder h = new StringBuilder();
    h.append("-- title: ").append(m.title).append('\n');
    h.append("-- menu: ").append(m.menu).append('\n');
    if (!"none".equals(m.events)) h.append("-- event: ").append(m.events.replace(",", ", ")).append('\n');
    if (!"none".equals(m.hooks)) h.append("-- hook: ").append(m.hooks.replace(",", ", ")).append('\n');
    h.append("-- java: Main\n");
    h.append("-- Written in Java by AI Create. Source: Main.java, compiled file: plugin.jar.\n");
    write(new File(dir, "main.lua"), h.toString());
    new File(dir, "last_error.txt").delete();
    lastCode = m.code;
    lastName = name;
    lastFolder = dir;
    return (tool ? "Tools/" : "Extensions/") + name;
  }

  private static String readFile(File f) {
    StringBuilder sb = new StringBuilder();
    try (BufferedReader r = new BufferedReader(new FileReader(f))) {
      String line;
      while ((line = r.readLine()) != null && sb.length() < 3000) sb.append(line).append('\n');
    } catch (Exception ignored) {
    }
    return sb.toString();
  }

  private static void write(File f, String text) throws Exception {
    try (FileWriter w = new FileWriter(f)) {
      w.write(text);
    }
  }

  private static void copy(File from, File to) throws Exception {
    try (java.io.FileInputStream in = new java.io.FileInputStream(from);
        java.io.FileOutputStream out = new java.io.FileOutputStream(to)) {
      byte[] b = new byte[65536];
      int n;
      while ((n = in.read(b)) > 0) out.write(b, 0, n);
    }
  }
}
