# Lua extension system (AndroLua + Create Extension/Tool IDE) — progress notes

**हिंदी सार:** यह फ़ाइल नई चैट/दूसरे अकाउंट के लिए है। नीचे लिखा है कि Lua इंजन और एडिटर वाला काम कहाँ तक हुआ और आगे क्या बचा है। नई चैट में लिखिए: "LUA_IDE_PROGRESS.md पढ़ो और बचा हुआ काम पूरा करो"।

Rules for the AI (from the user, Mobeen): reply in Hindi, short, phone-sized. Say "दृष्टिबाधित", never "अंधे". Ask before creating files unless he says "बनाओ"/build. He cannot run code in the sandbox; he installs the APK (GitHub Actions build) and reports in words. Be honest about what is unverified.

## Decision
Use nirenr's **AndroLua_pro** (MIT) C engine (Lua 5.3 + LuaJava) directly, not LuaJ. Source was uploaded as `AndroLua_pro-master.zip` (not stored here). jieshuo's own repo has no app source, so the `service.*` API is designed by us.

## Steps (7) and status
1. DONE (unverified build) — Engine C sources copied to `talkback/src/main/jni/` (`lua/` core without lua.c/luac.c, `luajava/`), own `Android.mk`/`Application.mk` (ABIs armeabi-v7a + arm64-v8a, 16 KB page flag).
2. DONE (unverified build) — `talkback/build.gradle`: `ndkVersion "27.1.12297006"` + `externalNativeBuild { ndkBuild }`. Workflow installs `ndk;27.1.12297006`. ABI filters already existed in `shared.gradle`.
3. DONE (unverified build) — Java: `com.luajava` (16 files), `com.androlua` (LuaContext, LuaBitmap, LuaGcable, LuaUtil, LuaEnhancer), `com.android.cglib` (dexmaker for LuaEnhancer). `LuaEnhancer` patched: uses `LuaEnhancer.sContext` instead of AndroLua's LuaApplication.
4. DONE — Lua scripts in `talkback/src/main/assets/lua/` (import, json, base64, hex, http, loadbitmap, loadlayout); copied to filesDir/lua on first run. Licenses in `assets/licenses/` (MIT; keep them).
5. DONE (unverified) — `editor/LuaServiceApi.java`: global `service` in Lua (also the LuaContext for import.lua). Functions: `execute(name)`, `click(text)`, `startApp(label)`, `plugin(name,node)`, `tool(name,node)`, `speak`, `toast`, `log`; `print()` is routed to `service.log` (speaks it). Fresh Lua state per run.
6. DONE (unverified) — `editor/LuaEditorActivity.java` (Save, Insert x6, Granular Mode = selects unit at cursor, Execute, Undo/Redo), `ProjectCreateDialog.java`, `LuaPaths.java`, layout `activity_lua_editor.xml`, strings `ide_strings.xml`. Manifest: activity + storage permissions. Menu: "Create Extension"/"Create Tool" added to `res/menu/context_menu.xml` and handled in `ContextMenuItemClickProcessor`.
7. NOT DONE — first GitHub Actions build + fixing errors. Expect compile errors from: (a) C build for arm64, (b) `com.android.cglib`/`LuaUtil`/`LuaBitmap` imports, (c) androidx vs old support classes.

## Known gaps / ideas for next
- Auto-click snippet now generates one `service.click("text")` per line (the doc's table form is not reliably converted by luajava).
- `execute()` names implemented: Speak time and battery level, Screen reader settings, Notification bar, Quick settings, Take a screenshot, Back, Home, Recents, Copy, Paste. NOT implemented: Suspend browse by touch, Actions, Virtual screen, Current location, Speak current lighting (they return false).
- `task/thread/timer` of AndroLua (LuaThread, LuaAsyncTask, LuaTimer) are not copied yet; import.lua only needs them if scripts call them.
- Storage: uses /sdcard/TalkBack if "all files access" is granted, else the app's own external folder. Editor asks once.
- Menu items may be hidden if TalkBack's "customize menus" setting filters them — check on device.
- Later (from his ideas): long-press an extension -> Delete / Send / Edit / assign gesture; per-app notification on/off from the app-icon long-press menu.
- Do not touch key/package/signing (see HANDOFF.md).

## Feedback settings (copy of Jieshuo's screen) — status
Rules from user: replicate Jieshuo's whole "Feedback settings" EXCEPT "download more sound themes". Screen renamed from "Sound and vibration" to "Feedback settings" (res/values/strings.xml). User said: finish it fully, not half; OK to use Jieshuo's bundled default sounds.
- DONE (unverified build): vibration intensity list (Light 12/Medium 32/Strong 48/Strongest 64) -> `utils/.../FeedbackController.applyHapticIntensity` (Medium = unchanged). Pref `pref_ms_vibrate_intensity`.
- DONE (unverified): "Apps where additional sound effects are not used" -> `soundtheme/AppSoundMuteActivity`, pref `pref_ms_no_sound_apps`. Effective: `ExtraEventSounds` tracks the front app (`currentPackage`) and `SoundThemeManager.playSlot` skips ticked apps. Applies only to the extra slots, not to TalkBack's own 25 sounds.
- DONE (unverified): "precise sound effect" switch (`pref_ms_precise_sound_effect`, default off). STORED ONLY: the text-matching sound feature does not exist yet.
- DONE (unverified): 54 extra sound slots (`SoundThemeManager.buildSlots`). Seven have a Jieshuo default file (res/raw/jx_*.ogg): focus0, focus4, beep, cancel, tick, clock, camera_click. Others silent until a theme assigns a file.
- Triggers wired (`soundtheme/ExtraEventSounds`, fed from `TalkBackService.onAccessibilityEvent`): talkman_start/stop, feedback_paused/resume, power_disconnected, power_low, unlock, raise/lower_volume, dialog, toast, edit_box, check_box, seek_bar, progress, has_action, scroll_top/bottom/page, inputmethod_show/hide, copy, paste (TextEditActor), to_back, screenshot (SystemActionPerformer), auto_ocr/_done/_error (OcrTool), auto_trans (TranslateEngine).
- NOT wired (no matching feature/event in this TalkBack yet): focus0, focus4, beep, cancel, tick, clock, camera_click, action_item, page_up/down, progress_up/down, progress_100, timer_start/end, recognition_*, previous_text/next_text, append_copy, clear, clear_notification. They can be assigned and previewed, but nothing plays them.
- NOT DONE: first GitHub Actions build + fixing compile errors.
