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
