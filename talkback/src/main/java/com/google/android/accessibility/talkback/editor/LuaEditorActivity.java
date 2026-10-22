package com.google.android.accessibility.talkback.editor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Built-in Lua code editor with the Insert engine, for Advanced Screen Reader extensions and tools. */
public class LuaEditorActivity extends Activity {
  public static final String EXTRA_CREATE_TYPE = "create_type";
  public static final String EXTRA_FILE_PATH = "file_path";

  private EditText editor;
  private File activeFile;

  private final ArrayList<String> history = new ArrayList<>();
  private int historyPos = -1;
  private boolean applyingHistory = false;

  /** Insert function: every default function of Jieshuo (the 172 names of JxCommands.TABLE, minus "No command"). */
  private static final String[] FUNCTIONS_LIST = buildFunctionsList();

  private static String[] buildFunctionsList() {
    String[][] t = JxCommands.TABLE;
    String[] a = new String[t.length - 1];
    for (int i = 1; i < t.length; i++) {
      a[i - 1] = t[i][1];
    }
    return a;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_lua_editor);
    editor = findViewById(R.id.editor_content);

    ((Button) findViewById(R.id.btn_save)).setOnClickListener(v -> save(true));
    ((Button) findViewById(R.id.btn_insert)).setOnClickListener(v -> showInsertDialog());
    ((Button) findViewById(R.id.btn_tools)).setOnClickListener(v -> showCodeToolsDialog());
    ((Button) findViewById(R.id.btn_full)).setOnClickListener(v -> selectUnit(0));
    ((Button) findViewById(R.id.btn_para)).setOnClickListener(v -> selectUnit(1));
    ((Button) findViewById(R.id.btn_lines)).setOnClickListener(v -> selectUnit(2));
    ((Button) findViewById(R.id.btn_chars)).setOnClickListener(v -> selectUnit(3));
    ((Button) findViewById(R.id.btn_custom)).setOnClickListener(v -> askCustomSeparator());
    ((Button) findViewById(R.id.btn_ok))
        .setOnClickListener(
            v -> {
              if (save(true)) {
                finish();
              }
            });
    ((Button) findViewById(R.id.btn_cancel)).setOnClickListener(v -> finish());
    ((Button) findViewById(R.id.btn_execute)).setOnClickListener(v -> execute());
    ((Button) findViewById(R.id.btn_undo)).setOnClickListener(v -> stepHistory(-1));
    ((Button) findViewById(R.id.btn_redo)).setOnClickListener(v -> stepHistory(1));

    editor.addTextChangedListener(
        new TextWatcher() {
          @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
          @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
          @Override public void afterTextChanged(Editable s) {
            if (!applyingHistory) {
              pushHistory(s.toString());
            }
          }
        });

    String path = getIntent().getStringExtra(EXTRA_FILE_PATH);
    String createType = getIntent().getStringExtra(EXTRA_CREATE_TYPE);
    if (path != null) {
      openFile(new File(path));
    } else if (createType != null) {
      askNameAndCreate("extension".equals(createType));
    } else {
      finish();
      return;
    }
    maybeAskStorageAccess();
  }

  // ------------------------------------------------------------------ create / load / save

  private void askNameAndCreate(final boolean isExtension) {
    final EditText input = new EditText(this);
    input.setHint(R.string.dialog_enter_name);
    new AlertDialog.Builder(this)
        .setTitle(isExtension ? R.string.menu_create_extension : R.string.menu_create_tool)
        .setView(input)
        .setPositiveButton(android.R.string.ok, (d, w) -> {
          String name = input.getText().toString().trim().replaceAll("[\\\\/:*?\"<>|]", "_");
          if (name.isEmpty()) {
            finish();
            return;
          }
          File base = isExtension ? LuaPaths.extensions(this) : LuaPaths.tools(this);
          File dir = new File(base, name);
          dir.mkdirs();
          openFile(new File(dir, "main.lua"));
        })
        .setNegativeButton(android.R.string.cancel, (d, w) -> finish())
        .setOnCancelListener(d -> finish())
        .show();
  }

  private void openFile(File f) {
    activeFile = f;
    StringBuilder sb = new StringBuilder();
    if (f.exists()) {
      try (BufferedReader r = new BufferedReader(new FileReader(f))) {
        String line;
        while ((line = r.readLine()) != null) {
          sb.append(line).append('\n');
        }
      } catch (IOException e) {
        Toast.makeText(this, "Error loading file", Toast.LENGTH_SHORT).show();
      }
    }
    applyingHistory = true;
    editor.setText(sb.toString());
    applyingHistory = false;
    history.clear();
    historyPos = -1;
    pushHistory(sb.toString());
    setTitle(f.getParentFile().getName());
  }

  private boolean save(boolean toast) {
    if (activeFile == null) {
      return false;
    }
    try (FileWriter w = new FileWriter(activeFile)) {
      w.write(editor.getText().toString());
      if (toast) {
        Toast.makeText(this, "Saved: " + activeFile.getParentFile().getName(), Toast.LENGTH_SHORT).show();
      }
      return true;
    } catch (IOException e) {
      Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
      return false;
    }
  }

  private void maybeAskStorageAccess() {
    final SharedPreferences sp = getSharedPreferences("lua_ide", Context.MODE_PRIVATE);
    if (LuaPaths.hasAllFilesAccess(this) || sp.getBoolean("asked_storage", false)) {
      return;
    }
    sp.edit().putBoolean("asked_storage", true).apply();
    new AlertDialog.Builder(this)
        .setTitle("Storage access")
        .setMessage(
            "Allow access to all files to keep extensions in the TalkBack folder on your phone. "
                + "Otherwise they are kept in the app's own folder.")
        .setPositiveButton("Allow", (d, w) -> LuaPaths.requestAllFilesAccess(this))
        .setNegativeButton("Not now", null)
        .show();
  }

  // ------------------------------------------------------------------ undo / redo

  private void pushHistory(String text) {
    if (historyPos >= 0 && historyPos < history.size() && history.get(historyPos).equals(text)) {
      return;
    }
    while (history.size() > historyPos + 1) {
      history.remove(history.size() - 1);
    }
    history.add(text);
    if (history.size() > 100) {
      history.remove(0);
    }
    historyPos = history.size() - 1;
  }

  private void stepHistory(int delta) {
    int np = historyPos + delta;
    if (np < 0 || np >= history.size()) {
      Toast.makeText(this, delta < 0 ? "Nothing to undo" : "Nothing to redo", Toast.LENGTH_SHORT).show();
      return;
    }
    historyPos = np;
    applyingHistory = true;
    editor.setText(history.get(np));
    editor.setSelection(editor.getText().length());
    applyingHistory = false;
  }

  // ------------------------------------------------------------------ insert engine

  private void showInsertDialog() {
    String[] options = {
      getString(R.string.insert_function), getString(R.string.insert_plugins),
      getString(R.string.insert_tools), getString(R.string.insert_custom_voice),
      getString(R.string.insert_app), getString(R.string.insert_auto_click)
    };
    new AlertDialog.Builder(this)
        .setTitle(R.string.ide_insert)
        .setItems(options, (d, which) -> {
          switch (which) {
            case 0: showFunctionsPicker(); break;
            case 1: showFolderPicker(LuaPaths.extensions(this), R.string.insert_plugins, "plugin"); break;
            case 2: showFolderPicker(LuaPaths.tools(this), R.string.insert_tools, "tool"); break;
            case 3: showCustomVoiceDialog(); break;
            case 4: showApplicationsPicker(); break;
            case 5: showAutoClickDialog(); break;
            default: break;
          }
        })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  // ------------------------------------------------------------------ code tools (Step 17)

  private void showCodeToolsDialog() {
    String[] options = {
      "Format source code", "Analyze java package for imports", "Insert service function", "API help"
    };
    new AlertDialog.Builder(this)
        .setTitle(R.string.ide_code_tools)
        .setItems(options, (d, which) -> {
          switch (which) {
            case 0: formatCode(); break;
            case 1: analyzeImports(); break;
            case 2: showServiceFunctions(); break;
            case 3: showApiHelp(); break;
            default: break;
          }
        })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void formatCode() {
    String before = editor.getText().toString();
    String after = LuaFormatter.format(before);
    if (after.equals(before)) {
      android.widget.Toast.makeText(this, "Already formatted", android.widget.Toast.LENGTH_SHORT).show();
      return;
    }
    pushHistory(before);
    int cursor = Math.min(editor.getSelectionStart(), after.length());
    editor.setText(after);
    editor.setSelection(Math.max(cursor, 0));
    android.widget.Toast.makeText(this, "Code formatted", android.widget.Toast.LENGTH_SHORT).show();
  }

  private void analyzeImports() {
    final String code = editor.getText().toString();
    final java.util.List<String> lines = LuaImportAnalyzer.missing(code);
    if (lines.isEmpty()) {
      android.widget.Toast.makeText(this, "No missing imports found", android.widget.Toast.LENGTH_LONG).show();
      return;
    }
    StringBuilder sb = new StringBuilder();
    for (String l : lines) {
      sb.append(l).append('\n');
    }
    new AlertDialog.Builder(this)
        .setTitle("Add these imports?")
        .setMessage(sb.toString())
        .setPositiveButton(android.R.string.ok, (d, w) -> {
          pushHistory(code);
          editor.setText(LuaImportAnalyzer.insert(code, lines));
        })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void showServiceFunctions() {
    FilterPicker.show(this, "service.", ServiceApiList.serviceMethods(),
        item -> insertSnippet(ServiceApiList.snippet(item)));
  }

  private void showApiHelp() {
    final java.util.List<ServiceApiList.Help> help = ServiceApiList.apiHelp(this);
    if (help.isEmpty()) {
      android.widget.Toast.makeText(this, "The help file is missing", android.widget.Toast.LENGTH_LONG).show();
      return;
    }
    java.util.List<String> titles = new java.util.ArrayList<>();
    for (ServiceApiList.Help h : help) {
      titles.add(h.title);
    }
    FilterPicker.show(this, "API help", titles, item -> {
      for (ServiceApiList.Help h : help) {
        if (h.title.equals(item)) {
          new AlertDialog.Builder(this).setTitle(item).setMessage(h.text)
              .setPositiveButton(android.R.string.ok, null).show();
          return;
        }
      }
    });
  }

  private static String q(String s) {
    return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\"";
  }

  private void showFunctionsPicker() {
    new AlertDialog.Builder(this)
        .setTitle(R.string.insert_function)
        .setItems(FUNCTIONS_LIST, (d, which) ->
            insertSnippet("\nif service.execute(" + q(FUNCTIONS_LIST[which]) + ") then\n    return true\nend\n"))
        .show();
  }

  private void showFolderPicker(File base, int title, final String fn) {
    String[] names = base.list();
    if (names == null || names.length == 0) {
      Toast.makeText(this, "Nothing created yet", Toast.LENGTH_SHORT).show();
      return;
    }
    final String[] sorted = names;
    java.util.Arrays.sort(sorted);
    new AlertDialog.Builder(this)
        .setTitle(title)
        .setItems(sorted, (d, which) ->
            insertSnippet("\nif service." + fn + "(" + q(sorted[which]) + ", node) then\n    return true\nend\n"))
        .show();
  }

  private void showCustomVoiceDialog() {
    final EditText input = new EditText(this);
    input.setHint("Enter command name");
    new AlertDialog.Builder(this)
        .setTitle(R.string.insert_custom_voice)
        .setView(input)
        .setPositiveButton(android.R.string.ok, (d, w) -> {
          String cmd = input.getText().toString().trim();
          if (!cmd.isEmpty()) {
            insertSnippet("\nif service.execute(" + q(cmd) + ") then\n    return true\nend\n");
          }
        })
        .show();
  }

  private void showApplicationsPicker() {
    PackageManager pm = getPackageManager();
    Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
    List<String> names = new ArrayList<>();
    for (ResolveInfo ri : pm.queryIntentActivities(main, 0)) {
      String n = String.valueOf(ri.loadLabel(pm));
      if (!names.contains(n)) {
        names.add(n);
      }
    }
    Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
    final String[] arr = names.toArray(new String[0]);
    new AlertDialog.Builder(this)
        .setTitle(R.string.insert_app)
        .setItems(arr, (d, which) ->
            insertSnippet("\nif service.startApp(" + q(arr[which]) + ") then\n    return true\nend\n"))
        .show();
  }

  private void showAutoClickDialog() {
    final EditText input = new EditText(this);
    input.setHint(R.string.dialog_auto_click_hint);
    input.setMinLines(4);
    input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    new AlertDialog.Builder(this)
        .setTitle(R.string.insert_auto_click)
        .setView(input)
        .setPositiveButton(android.R.string.ok, (d, w) -> {
          List<String> steps = new ArrayList<>();
          for (String line : input.getText().toString().split("\n")) {
            String step = line.trim();
            if (!step.isEmpty()) {
              steps.add(q(step));
            }
          }
          if (steps.isEmpty()) {
            Toast.makeText(this, "Enter at least one click step", Toast.LENGTH_SHORT).show();
            return;
          }
          // Jieshuo's auto-click format is one Lua table containing the entire sequence.
          StringBuilder sb = new StringBuilder("\nif service.click{{");
          for (int i = 0; i < steps.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(steps.get(i));
          }
          sb.append("}} then\n    return true\nend\n");
          insertSnippet(sb.toString());
        })
        .show();
  }

  private void insertSnippet(String code) {
    int a = Math.max(editor.getSelectionStart(), 0);
    int b = Math.max(editor.getSelectionEnd(), 0);
    editor.getText().replace(Math.min(a, b), Math.max(a, b), code, 0, code.length());
  }

  // ------------------------------------------------------------------ granular mode

  /** 0 = full text, 1 = paragraph, 2 = line, 3 = character: selects that unit around the cursor. */
  private void selectUnit(int which) {
    String[] names = {"Full text", "Paragraphs", "Lines", "Characters"};
    String t = editor.getText().toString();
    int pos = Math.max(0, Math.min(editor.getSelectionStart(), t.length()));
    int s = 0;
    int e = t.length();
    switch (which) {
      case 1:
        s = t.lastIndexOf("\n\n", Math.max(0, pos - 1));
        s = s < 0 ? 0 : s + 2;
        e = t.indexOf("\n\n", pos);
        e = e < 0 ? t.length() : e;
        break;
      case 2:
        s = t.lastIndexOf('\n', Math.max(0, pos - 1));
        s = s < 0 ? 0 : s + 1;
        e = t.indexOf('\n', pos);
        e = e < 0 ? t.length() : e;
        break;
      case 3:
        s = Math.min(pos, t.length());
        e = Math.min(pos + 1, t.length());
        break;
      default:
        break;
    }
    editor.requestFocus();
    editor.setSelection(Math.min(s, e), Math.max(s, e));
    Toast.makeText(this, "Selected: " + names[which], Toast.LENGTH_SHORT).show();
  }

  /** Custom: you type the separator (for example a comma or a bracket); the piece around the cursor is selected. */
  private void askCustomSeparator() {
    final EditText input = new EditText(this);
    input.setHint("Separator, for example , or )");
    new AlertDialog.Builder(this)
        .setTitle("Custom")
        .setView(input)
        .setPositiveButton(
            android.R.string.ok,
            (d, w) -> {
              String sep = input.getText().toString();
              if (sep.isEmpty()) {
                return;
              }
              String t = editor.getText().toString();
              int pos = Math.max(0, Math.min(editor.getSelectionStart(), t.length()));
              int s = t.lastIndexOf(sep, Math.max(0, pos - 1));
              s = s < 0 ? 0 : s + sep.length();
              int e = t.indexOf(sep, pos);
              e = e < 0 ? t.length() : e;
              editor.requestFocus();
              editor.setSelection(Math.min(s, e), Math.max(s, e));
              Toast.makeText(this, "Selected: Custom", Toast.LENGTH_SHORT).show();
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  // ------------------------------------------------------------------ execute

  private void execute() {
    if (!save(false) || activeFile == null) {
      return;
    }
    if (TalkBackService.getInstance() == null) {
      Toast.makeText(this, "Screen reader is not running", Toast.LENGTH_SHORT).show();
      return;
    }
    final File f = activeFile;
    final Context app = getApplicationContext();
    new Thread(() -> {
      String err = LuaServiceApi.run(app, f, null);
      runOnUiThread(() ->
          Toast.makeText(this, err == null ? "Done" : err, Toast.LENGTH_LONG).show());
    }).start();
  }
}
