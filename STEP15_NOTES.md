# Step 15 - Extensions and Tools screens + Jieshuo-style editor (2026-10-08)

**हिंदी सार:** इस स्टेप में Extensions और Tools की अलग स्क्रीन बनी (खोज का बॉक्स, New बटन, सूची; टैप = चलाओ; डबल-टैप-होल्ड = Add to home / Delete / Edit वगैरह), और एडिटर जीशू जैसा हुआ। कुछ भी कंपाइल या फ़ोन पर परखा नहीं गया। जीशू की संरचना `audio-text-all.txt` (वीडियो 3 से 7, 10, 21) और जीशू के manifest/strings से ली गई है। कोड पूरी तरह नया लिखा गया है, जीशू का कोड कॉपी नहीं किया।

## What Jieshuo does (studied, used as the model)
- Settings > Advanced settings > Extensions (and Tools). Screen: search box (Enter keyword), New button, list. Tap = run. Touch-and-hold menu: Add to home, Delete, Edit.
- New = name dialog (OK / Cancel) -> text editor: Save on top, big text box in the middle, below it Full text / Paragraphs / Lines / Custom / Characters / OK, under that Execute and Insert. Insert = function, plugins, tools, custom voice command, application, automatic click.
- Folders in Jieshuo: `plugin/<name>/main.lua`, `tool/<name>/main.lua`; packages `.ppk` (extension) and `.xpk` (tool) are zips; `init.lua` holds appname/appver/mode/ext. Other packs: `.spk` sound, `.gpk` gesture, `.vpk` voice commands, `.tpk`, `.kpk`, `.lpk`, `.rpk`, `.cbk` clipboard backup, `.fbk` favorites backup.
- The audio-text-all.txt file is a tutorial transcript: it does NOT contain source code of extensions or tools, only how the screens behave.

## What is new in this zip
New files (editor/): `ExtensionListActivity.java` (one screen for both kinds, `kind` = ext or tool), `RunShortcutActivity.java` (invisible, behind the home-screen shortcut).
Edited: `LuaEditorActivity.java` + `res/layout/activity_lua_editor.xml` (new button arrangement; granular dialog replaced by direct buttons; OK = save and close; Cancel; Custom asks for a separator and selects the piece around the cursor; new extension starts EMPTY, no header lines), `ProjectCreateDialog.java` (menu "Create extension / tool" now asks the name first, then opens the editor), `JxPickerActivity.java` (extensions/tools kinds open the new screen), `LuaExtensionManager.java`, `AndroidManifest.xml` (two activities).

## Decisions taken (change if you disagree)
1. BEHAVIOUR CHANGE: an extension appears in the TalkBack context menu only if its first 20 lines contain `-- menu: on`. Before, no header meant "in menu". Reason: new extensions made the menu grow without being asked. Old extensions made from the earlier template keep `-- menu: on` and are unaffected; old ones without header disappear from that menu (still run from the list).
2. Search box uses "contains" (Jieshuo's video shows names that START with the typed letters).
3. OK in the editor saves and returns to the list (Jieshuo returns to the home screen).
4. "Add to home" uses the modern pinned-shortcut API (Android 8+); the launcher asks the user to confirm. Activity is not exported (so other apps cannot start your extensions). If the shortcut does nothing on a phone, try `android:exported="true"` on RunShortcutActivity, but that lets other apps trigger extensions.
5. Old combined screen ExtensionManagerActivity (menu "Manage extensions and tools") is kept as it was.

## Phone tests needed (after the first successful GitHub build)
New > name > editor opens with a big empty text box; Save; OK; extension in list; tap runs it; touch-and-hold menu (all items); Add to home (pin dialog, icon, tap runs it with screen reader on); Duplicate ("name 2"); Rename (voice commands follow); Delete; search box filters; Custom selection; extension without `-- menu: on` is NOT in TalkBack menu; Tools list opens from the Tools command.

## STILL TO DO (so you can continue in another account) - in this order
**Step 16:**
1. `.ppk` / `.xpk` import and export (zip of the folder). Import by opening the file from WhatsApp/Telegram/file manager (intent-filter ACTION_VIEW and ACTION_SEND), export/share as a file (needs a FileProvider; check if the manifest already has one). Read and write `init.lua` (appname, appver) so names show like Jieshuo.
2. Manager screen ("Extension manager" / resource manager): import resources, backup and restore (settings, clipboard, favorites, voice commands, gesture schemes).
3. The default function "Extension" (Jieshuo: swipe up then left): a dialog with all extensions, then Cancel, Manage, Custom voice commands. Check what `execute("Extensions")` does now (it opens the new list screen; add Manage and Custom voice commands rows).
4. Settings: "Autostart extensions" (setup_plugin) and the two voice-assistant switches "allow to execute extensions" / "allow to execute tools" (voice_helper_use_plugin / voice_helper_use_tool).
5. Assign an extension to a gesture (listed as "plugin" in Jieshuo's gesture assign list) - needs a look at JxGesture / the gesture preference screen. Also "give a name and a gesture" from Step 10.
**Step 17 (editor):**
6. Editor menu items: Format source code, Analyze java package for imports, API help.
7. `service.` list: Jieshuo has 1171 services; typing `service.` then letters filters them (video 10). Build a picker with filter from JX_SERVICE_SIGNATURES.txt / LuaServiceApi.
8. "Insert function" shows only 10 fixed names now; Jieshuo lists all default functions (the 152 developer functions). Use the 173 ids/names from JxCommands.TABLE. Also check that `service.plugin(...)` and `service.tool(...)` (written by Insert plugins / Insert tools) really exist in LuaServiceApi; if not, add them.
9. Decide: Execute from the editor runs on top of the editor screen itself (same as before); Jieshuo runs it as in a normal screen. Maybe delay 3 seconds like the builder's Test.
**Later / not decided:**
10. Jieshuo "Function manager" (functions live inside gesture schemes, per-app gesture schemes, Create a new function vs Create a new automatic click): our "function" idea is different. Needs a design talk first.
11. Download extension from a server (needs a server; probably skip).
12. Share to server (Jieshuo "share"), `.spk/.gpk/.vpk/.tpk` packs.
13. The older "Still not done" items in STEP14_NOTES.md (left/right shortcut chooser screen, screen grid, cloud clipboard, camera screen, edge gestures) and NEXT_STEPS_ACCESSIBLE.md sections B and D.
14. FIRST in any new chat: run the GitHub build and send the errors (NEXT_STEPS_ACCESSIBLE.md section E). Nothing from Steps 1-15 has been compiled. If these are still happening, report them: crash when opening Extensions, no launcher icon, service shows "not working" after a crash. Step 15 replaced the Extensions screen, so the crash may be gone or different.

## Ready sentence for the next chat
"NEXT_STEPS_ACCESSIBLE.md, STEP15_NOTES.md, HANDOFF.md padho, build errors thik karo, phir Step 16 (.ppk/.xpk import export) banao."
