# Step 46 — Classes that Jieshuo extensions and tools need (2026-10-17)

**हिंदी सार:** आपके असली बैकअप (`mobin_niv_bakkap.zip`: 39 extensions, 17 tools) की सारी स्क्रिप्टें पढ़ीं और देखा कि वे कौन सी Java क्लासेस मांगती हैं। तीन चीज़ें MS Screen Reader में नहीं थीं, तीनों अब जोड़ दी हैं। इस वजह से `application menu` (2 extensions), `apps store by nitin kumar tool` और `YouTube audio content describe with Gemini I` नहीं चलते थे।

## What was missing (found by reading all 56 `main.lua` files of the backup)
| Script uses | Used in | Problem | Fix |
|---|---|---|---|
| `LuaDialog(this)` (`dlg.View=`, `setTitle`, `setMessage`, `setButton`, `show`, `dismiss`) | application menu (x2), apps store tool | class did not exist -> script stops at once | new `com/androlua/LuaDialog.java` |
| `SingleLineAdapter(service, String(items))` | application menu (x2) | class did not exist | new `com/androlua/SingleLineAdapter.java` |
| `import "com.nirenr.talkman.TalkManAccessibilityService"` | YouTube audio content describe with Gemini I | `import.lua` raised "cannot find ..." at line 5 | alias table `JX_CLASS_ALIAS` in `assets/lua/import.lua` -> `com.google.android.marvin.talkback.TalkBackService` |

Everything else the scripts import is a normal Android/Java class (Intent, MediaPlayer, TextToSpeech, SpeechRecognizer, URL, ...) or `require "import"` / `require "cjson"` (cjson C sources are already in `jni/cjson`).

## How the new classes work (written from how the scripts call them, not copied from Jieshuo)
- `LuaDialog extends AlertDialog`: same public names as the scripts use (also `setItems`, `setAdapter`, `setSingleChoiceItems`, `setMultiChoiceItems`, `setOkButton`, `setNegButton`, `setNeuButton`, `setOnClickListener`, item listeners, `hide`, `isShowing`). If the context comes from the accessibility service (the Lua `service`/`this` wraps it, Step 42) the window type is TYPE_ACCESSIBILITY_OVERLAY, so it opens on top of any app. A failing `show()` is logged and never crashes the service.
- `SingleLineAdapter extends ArrayAdapter<String>`: one-line rows, items cut at 5000 characters.
- `import.lua`: only the name of the Jieshuo service class is mapped; the script's variable name stays `TalkManAccessibilityService`.

## Checked
- Read all scripts: no other non-standard class is imported (list in this note's table).
- `import.lua` patch: same Lua syntax result as before the patch (the only complaint, line ~259 `k = ...`, is Lua 5.4-only and was already there; the app uses Lua 5.3).
- NOT compiled and NOT run: the two Java files could not be built here (no Android SDK). Run `devtools/compile_check.py` and the GitHub Actions build first.

## Phone tests (after the build)
1. Extension `application menu`: a dialog with a 2-column list of apps should open; tap one -> it opens.
2. Tool `apps store by nitin kumar tool`: dialog with title, message and text box.
3. Extension `YouTube audio content describe with Gemini I` must start (no "cannot find" error).

## Not done on purpose
Jieshuo's own Lua modules `su`, `loadmenu`, `permission`, `logcat`, `console`, `bmob`, `test`: no script in this backup uses them.
