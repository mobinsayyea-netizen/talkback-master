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

## Session notes: making our Lua extension system match Jieshuo's (UNFINISHED — continue here)
User goal (Mobeen): Jieshuo's extension (plugin) system and ours must have the SAME structure and the SAME `service.*` API; add all Jieshuo services; error margin ~zero because the whole screen reader rests on it. Reference zip: cSr.zip = Jieshuo Max 20260626 decompiled (smali, obfuscated; do NOT try to copy smali).

### What was found (done)
- Jieshuo's official extension API doc is inside its resources: `res/values-zh-rCN/arrays.xml`, array `code_help_items` (108 items). Saved here as **JX_API_REFERENCE_zh.txt** (Chinese; 103 items = full API + auto-click mini-language). Earlier note "service API designed by us" is now outdated: the real API is known.
- Full `service.execute()` command list (173 commands) saved as **JX_EXECUTE_COMMANDS.txt**.
- Jieshuo folders: Plugins (Extensions), Tools, Custom voice commands (cmd), Timer, Profiles. Hook names Jieshuo calls in scripts include: onAccessibilityEvent, onNotification*, onKeyEvent, onGestureDetected, onWindowStateChanged, onViewLongClicked(1-4), onVolumeChanged, onScrolled, onTransResult, onScreenCaptureDone (from smali strings; verify before relying).
- Our current `editor/LuaServiceApi.java` only has: execute (10 English names), click(text), startApp, plugin, tool, speak, toast, log, loadDex. It does NOT match Jieshuo yet.

### Jieshuo service API to implement (from JX_API_REFERENCE_zh.txt)
execute(cmd,node), postExecute(ms,cmd,node), getText(node), setText(node,text)/setText(text), copy()/copy(text), appendCopy()/appendCopy(text), paste()/paste(node), toNext()/toNext(node,bool), toPrevious()/toPrevious(node,bool), toNextChar/toPreviousChar/toNextLine/toPreviousLine/toTextStart/toTextEnd/toListViewStart/toListViewEnd, getRootNodeInfo(), ocr(node), setTimerMode(bool), setAutoReadEnabled(bool), setAutoNext(bool), isEditView/isSeekBar/isListView/isInListView/isInWebView/isClick(node), toClick()/toClick(node), toLongClick()/(node), toBack, toHome, toNotification, toParent (doc says recents), scrollBackward(node), scrollForward(node), loadGesturePackage(name), click(x,y), longClick(x,y), press(x,y,t), swipe(x1,y1,x2,y2,t), click({button list}) with the whole mini-language (`*` wildcard, `#n` index, leading `#` = custom label, `|` or, `&` and / `&!` not / `&type`, `$ms` wait, `<n` repeat, leading `<` locate only, `>n` wait count, leading `>app` open app, `%cmd` run command or Lua function, `[...]` direct tap/swipe forms, nested `{}` branches), postClick(ms,{list}), plugin(name,node) (must return the plugin's value, ours returns boolean now).
- Return rule: a gesture/plugin script returns true = consumed, false = fall to next layer (per-app gesture -> global gesture -> default).

### Code added this session (all UNVERIFIED, not compiled)
- Earlier in the same chat (shipped in MSScreenReader_fix.zip): voice command "open <app>" (MsAppLauncher) and screen-wake announcement using MS Advanced settings (time+date+year+battery, chosen voice).
- `TalkBackService`: new helpers `msFocusFeedback(FocusDirection.Builder)`, `msPartFeedback(Part.Builder)`, `msStartReading(boolean fromTop)`, `msStopReading()` (for toNext/toPrevious/scroll/read-all in the Lua API).
- `editor/JxGesture.java`: tap / long-press / swipe through `dispatchGesture` (for service.click(x,y), longClick, press, swipe). Not yet called from LuaServiceApi.

### Next steps (in order)
1. Rewrite `LuaServiceApi` so every function above exists with Jieshuo's exact name and argument forms (Lua passes tables as `com.luajava.LuaTable`; numbers arrive as Long/Double — accept both). Keep old methods (speak, toast, log, startApp, tool) as extras.
2. Implement `execute()` for all 173 names in JX_EXECUTE_COMMANDS.txt; where our app has no feature yet, return false (never throw).
3. Implement the auto-click mini-language parser as its own class (unit-testable, pure Java).
4. Hook functions: call the script's onAccessibilityEvent/onKeyEvent/etc. like Jieshuo; keep our header-comment events working.
5. Folder structure same as Jieshuo (Plugins/Tools/Custom voice commands/Timer/Profiles) — check what we use now (`Extensions`, `Tools`) and add aliases so Jieshuo extensions copy over unchanged. Plugin folder layout: `NAME/main.lua` (+ init.lua for project config, as AndroLua).
6. Compile on GitHub Actions and fix errors (nothing in this session has been compiled).
- Earlier unclear request "extension and tool must be the same" = this goal: same structure/API as Jieshuo's. User confirmed.

## zip 14 (this build) — step 1a of the Jieshuo-compatible API  [UNVERIFIED, not compiled]
New file JX_SERVICE_SIGNATURES.txt = every public method of Jieshuo's TalkManAccessibilityService with real smali signatures (names + argument types are reliable; method BODIES are control-flow obfuscated and cannot be read, so behaviour comes from JX_API_REFERENCE_zh.txt).
Added to LuaServiceApi: execute(cmd,node), postExecute, getText, setText (2 forms), copy/appendCopy/paste (all forms), toNext/toPrevious (+node forms), getRootNodeInfo, isEditView/isSeekBar/isListView/isInListView/isInWebView/isClick, click(x,y)/longClick(x,y)/press/swipe (5 and 6 args), toClick/toLongClick (+node), toBack/toHome/toNotification/toRecents/toParent, scrollForward/scrollBackward.
Rules learned: luajava mis-converts a Lua integer to boxed Double (returns Long), so the API uses PRIMITIVE double/int params only, never Double/Number. Absolute vs relative coordinates: value strictly between 0 and 1 = share of screen, else pixels (cannot keep Jieshuo's separate int/double overloads: luajava cannot tell them apart).
Known differences: toNext(node,bool) ignores the bool; toParent follows Jieshuo's doc (recents) - doc may be wrong, check on device; plugin() still returns boolean.

## zip 15+16 (one build) — auto-click language + all 173 execute commands  [Android side UNVERIFIED, not compiled]
NEW files in editor/: JxClickScript.java (pure-Java parser/runner, NO Android imports), JxClickEnv.java (phone side: find/click nodes), JxCommands.java (173-command table + executor). LuaServiceApi now has click(table), click(table,function), postClick(ms,table[,function]), callLuaFunction (for %func), execute(cmd[,node]) routed to JxCommands.
VERIFIED on a computer: JxClickScript passes 40 checks with a fake screen (devtools/jx_parser_test, see README.txt there). All new Java files pass a syntax check. NOT verified: anything that touches Android (JxClickEnv, JxCommands, LuaServiceApi) - never compiled.
Language rules implemented (from JX_API_REFERENCE_zh.txt): top list = alternatives; list inside it = sequence (stops at first missing item); list inside a sequence = optional branch; item symbols * # (index, negative from end; leading # = label) | & &! (also &type: 可编辑 可点击 列表 列表项目 编辑框) $ms < (repeat n; leading = locate only) > (wait n seconds; leading = open app) @app % (Lua function first, else command; function returning false halts everything) and all [..] forms except the 8-number colour search (returns false + message).
Choices NOT stated in Jieshuo's docs (tune on the phone): SETTLE 350 ms after each step, later sequence steps wait up to 1000 ms for the item, optional branch steps wait 300 ms, text match is whole-text, case-insensitive, on text or description; a function that returns nothing counts as "continue"; click(table,function) calls the function with the true/false result (guess); postClick ignores its function.
Custom labels (#label) are NOT supported yet (always "not found"). Colour search not supported.
execute(): every command is known by id (command_click), English name and Chinese name (all from Jieshuo's resources). Names "Tools" and "Timer" are used by two commands each (both unimplemented). Old names back/home/recents/copy/paste still work.
Commands that really work now (~60): settings, time/battery, back/home/recents/notifications/quick settings/screenshot/lock screen/power dialog/split screen, input method picker, voice assistant, answer/end call, click/long click/raw click/raw long click/double tap, 4 raw swipes, copy/append/paste/select all, next/previous focus, to top/end, next/previous granularity, read whole screen, scroll forward/back, slider +/-, 4 directional scrolls, media play/prev/next/ff/rewind, volume (media + accessibility). All other known commands return false (never throw).

## REMAINING WORK (in order)
1. plugin(name,node)/tool(name) must return the script's own return value (Object); plugin(a,b,node) form.
2. Remaining execute() commands: character/word/sentence/paragraph/heading/link/control navigation, previous/next window, selection mode, suspend/disable toggles (touch, feedback, hotkeys, gestures, main/secondary TTS), TTS engine selection, speech rate, clipboard history/favorites/cloud clipboard, OCR/describe/translate commands, camera, timer commands, network info, location, light, URL, virtual screen/navigation, focus monitor, dictionary, node alias/labels (#label), chat reading modes, quick/list/char/node modes.
3. toNextChar/toPreviousChar/Line/Word/Row/Paragraph/Window/Page, toTextStart/End, toListViewStart/End, startReadMode, setTimerMode/setAutoReadEnabled/setAutoNext, ocr(node), loadGesturePackage (need TalkBack granularity actors).
4. Make `service` a real ContextWrapper that forwards AccessibilityService methods (startActivity, performGlobalAction, getRootInActiveWindow...), because Jieshuo scripts call them on service.
5. Copy AndroLua task/thread/timer/ticker (LuaThread, LuaAsyncTask, LuaTimer, LuaTimerTask, Ticker are in AndroLua_pro-master.zip) + their Lua globals.
6. The ~200 setUseXxx/setXxx setters (see JX_SERVICE_SIGNATURES.txt): map to our settings where they exist, else return false silently.
7. Hook functions called by the service (onAccessibilityEvent, onKeyEvent, onGestureDetected, onNotification*, onWindowStateChanged, onViewLongClicked, onVolumeChanged, onScrolled ...) with the return-true-consumes rule.
8. Jieshuo folder layout (Plugins, Tools, Custom voice commands, Timer, Profiles) with aliases so Jieshuo extensions copy over unchanged.
9. FIRST GitHub Actions compile and fixing errors (nothing since the Lua engine step has ever been compiled).

## Step 4d — permissions screen reworked + click engine decision  [UNVERIFIED, not compiled]
Replaces the Step 4c text. permission/MsPermissionsActivity.java + res/values/ms_permission_strings.xml:
- Launcher icon (alias MsLauncherEntry) opens it until every permission is granted; after that a flag (prefs ms_permissions/onboarding_done) makes the icon open TalkBackPreferencesActivity directly. Settings opened from the system Accessibility page is NOT gated.
- One screen. Each row: name + state (मिल गई / बाकी है), one line saying WHY it is needed, and a "दें" button (only when missing). Rows: phone state, answer calls, call log, contacts, mic, notifications (API 33+), storage (API<30: read+write) or all-files access (API 30+), battery-optimisation exemption, Accessibility service on.
- Top buttons: "सभी अनुमतियाँ दें" (asks all runtime permissions at once, then opens all-files, battery, Accessibility settings one after another and continues when the user returns; every step is tried once per run so it cannot loop), "अनुमतियाँ जाँचें" (re-check + announce), "प्रतिबंधित सेटिंग्स..." (opens App info for Restricted settings), "बाहर निकलें" (first launch only; finishAffinity).
- New settings entry Settings > Other > Tools > "Check permissions" (res/xml/ms_group_9_other.xml, key ms_g9_check_permissions) opens the same screen with extra from_settings: no blocking, no exit button, shows current state.
- Android cannot grant Accessibility, all-files access or Restricted settings by itself; the user must flip those. Manifest got REQUEST_IGNORE_BATTERY_OPTIMIZATIONS.
- Deliberately NOT asked (feature does not exist here): overlay, exact alarm, auto-start, location, camera, notification listener. Add each only when its feature is built (location for "current location", camera for Jieshuo Camera).
Decision (user agreed): keep the repo's own click engine (JxClickScript + JxClickEnv). Step 2's AutoClickParser/Step/Runner are NOT merged and should not be. devtools/jx_parser_test now has 50 checks (40 old + 8 of the user's example lines: Send$1, <Play, Button*$1, %Direct swipe up$800, rem*|add to*, >whatsapp, and two sequences) - ALL PASS on a computer (java Run.java). Step 2's own 28-check file was not available in this session, so those exact cases were not copied.
Still open for the click engine: colour search [x1,y1,x2,y2,r,g,b,d], #labels, "Actions" command, step-builder UI, {{">Files", this.speak(...)}} form.
Still open overall: 91 execute commands (REMAINING_COMMANDS.txt), thread/task/LuaDialog, saved-only settings (MS_SETTINGS_NOTES.md), FIRST GitHub Actions build.

## Step 5 — 22 more execute() commands (104 of 173)  [UNVERIFIED, not compiled; syntax-parsed only]
TalkBackService: new msShowMenu(boolean actions), msOpenSettings(fragmentClassName), msDescribe(boolean wholeScreen) (Gemini: focused item via GeminiFunctionUtils.getPreferredImageDescriptionFeedback, whole screen via getPreferredScreenOverviewFeedback on the window root). New editor/JxPickerActivity (list of extensions / list of apps; manifest entry added).
Commands now wired (JxCommandTable): Plugins + Extensions -> extension list; Application -> app list; Main menu, Recognition menu, Per-app gestures menu, Functions menu, Focus menu -> TalkBack's grouped menu (Jieshuo has 4 separate menus; here they are all the same menu); Actions -> TalkBack actions menu; Gesture scheme description + Edit per-app gestures -> gestures settings page; Edit dictionary / Edit node alias / Add content blacklist -> their settings pages (not "add the focused item" yet); Clipboard history + Favorites -> ClipboardActivity (same screen for both); Open an URL (first link in focused text, else clipboard); Describe focused item / Detailed description of focused item / Recognize focused icon -> Gemini description of focus (all three are the same call); Describe entire screen / Detailed description of entire screen -> Gemini screen overview (same call).
To check on the phone: the menus open, settings pages open on the right fragment, Gemini description speaks.

## Step 6 — 9 more execute() commands + switches (113 of 173)  [UNVERIFIED, not compiled; syntax-parsed only]
- Disable hotkeys: toggles saved flag ms_cmd_disable_hotkey; MsOperationController.onKeyEvent ignores volume/headset shortcuts while it is on. Speaks "Hotkeys off/on".
- Suspend secondary TTS engine: toggles ms_cmd_disable_async_tts; MsSecondaryTts.speak returns without speaking while on (notifications/chat on the secondary voice go silent).
- Passthrough mode -> TalkBack pass-through (TalkBackService.msPassThrough). Selection mode -> toggles TalkBack text selection on the focused item (msSelectionMode). Reading mode -> read from top. Character-by-character mode -> sets granularity CHARACTER; Node browsing mode -> DEFAULT (msSetGranularity).
- Timer / Pause timer: simple stopwatch kept in JxCommandTable (first call starts, later calls say the time; Pause toggles pause/resume). Not the Jieshuo timer folder; does not survive a service restart.
STILL NOT DONE (REMAINING_COMMANDS.txt lists them; they return false, never throw). Reason by group:
- Needs a new feature first: current location (needs location permission + code), Jieshuo Camera, voice dictation / continuous dictation / Input / voice questions about focus or screen (need a speech-input flow), cloud clipboard copy/paste (needs a server), read all notifications / notification box / chat history / automatic chat reading (+history) (need a notification store), focus monitor (+manager), virtual screen / virtual navigation, identify form, CAPTCHA, subtitles, OCR of the whole screen, screenshot of focus, show all nodes, enable logging, Custom voice commands / Tools / Timer / Profiles folders.
- Needs a design decision: suspend browse by touch, suspend speech feedback + touch, lock touch, suspend edge gestures, suspend screen-reader gestures, toggle multi-finger gestures, suspend main TTS, accessibility volume suspend, earpiece mode, switch TTS engine (TalkBack here has no real suspend; each needs a flag read in the right place).
- Settings flags missing: automatic translation, automatic OCR, lift to activate, tap to move focus (no saved switch exists to toggle).
- Modes with no counterpart yet: list browsing, enhanced detail, quick browsing, screen grid, granular browsing/editing, list copying, full text editing, edit current text, long press and hold, left/right shortcut, select navigation type, previous/next element of a type, backwards automatic browsing.

## Step 17 (2026-10-10) - see STEP17_NOTES.md
Done (unverified, not compiled): packages .ppk/.xpk, Resource manager, Extension dialog, autostart + voice switches, extension on gesture, opt-in hooks (4 of ~10), editor Code tools. REMAINING: GitHub build first; other hooks; per-app gesture schemes.


## Step 28 (2026-10-11)
- Updated Lua Editor automatic-click insertion to generate a single Jieshuo-style table sequence and `return true` guard.
- Added `LuaServiceApi.openUrl(String)` and sound API methods for clock, scroll, gesture begin/end, action item, plus signature-compatible overloads. The legacy overload arguments are currently ignored pending verification of their exact semantics.
- Confirmed ExtensionListActivity already implements keyword filtering and long-press Delete/Rename/Send (Android share chooser), so no duplicate controls were added.
- Blocked: the archive does not actually contain `reference/androlua/` or the planned Java sources for LuaAsyncTask/LuaThread/LuaTimer/Http/Download. Engine repair and native-library work must wait for those source files; no fake replacement classes were added.
- Not build-tested; user runs GitHub Actions.


## Step 29 (2026-10-11) — guarded import fallback [UNVERIFIED]
- `assets/lua/import.lua` no longer assumes `LuaAsyncTask`, `LuaThread`, and `LuaTimer` always exist: class binding is guarded with `pcall`. When absent, task/thread/timer calls register work with the existing serialized `service.scheduleTask()` + `__jx_run` scheduler. The fallback is deliberately not claimed as full AndroLua parity; thread object behavior and timer cancellation need runtime tests.
- `LuaServiceApi.doFile(path, args...)` now pushes positional arguments before calling the loaded Lua chunk, instead of silently discarding them.
- Source archive inspection confirmed `reference/androlua/`, `LuaAsyncTask.java`, `LuaThread.java`, `LuaTimer.java`, `Http.java`, `Download.java`, and the planned native libraries are not present in the supplied ZIP. No Java stubs or native binaries were invented.
- Not compiled; no Lua runtime or Android build available in this environment. User must run GitHub Actions and report build logs.


## Step 30 (2026-10-11) — upstream source audit [UNVERIFIED]
- Confirmed public upstream source pages for the missing AndroLua helper classes. The source archive is not present in this project ZIP and the current environment could not clone/download GitHub into the working directory.
- Added a source map and a static verification script; no pretend Java replacements were added.
- No Android build or device test was run.


## Step 31 (2026-10-11) — HTTP fallback + decompiled API references [UNVERIFIED]
- Replaced the runtime `assets/lua/http.lua` implementation that required missing `socket`, `socket.url`, `ltn12`, `mime`, and `socket.headers` modules with a Java `URL`/`URLConnection` implementation. It exposes `get`, `post`, `download`, and multipart `upload` with the documented AndroLua-style return shapes. This is a compatibility implementation, not the original `com.androlua.Http` Java class; runtime behavior still needs testing.
- Preserved the old LuaSocket implementation in `reference/legacy_http_luasocket.lua` for license/history reference only; it is not loaded by the app.
- Extracted Jieshuo `LuaAsyncTask`, `LuaThread`, `LuaTimer`, `LuaTimerTask`, `Http`, `Download`, and `Ticker` smali files from the user-provided `cSr.zip` to `reference/jieshuo_smali/`. These are reference-only decompiled files, not build inputs and not Java source.
- Added `devtools/step31/verify_step31.py`; 6/6 static checks passed. No Lua interpreter, Android build, or phone test was run. `LuaAsyncTask`/`LuaThread`/`LuaTimer` Java implementations and native `bson`/`cjson`/`crypt`/`regex` modules remain outstanding. Package ID, signing, and keys were not changed.


## Step 33 (2026-10-11) — service hooks and aliases
- Added `runHookBoolean` and integrated opt-in `onKeyEvent`, `onGestureDetected`, and `onVolumeChanged` hooks. Key/gesture `true` return is used to consume the event; volume hook runs asynchronously.
- Added common AccessibilityService forwarding methods and Jieshuo-style folder path aliases.
- Static source-marker checks pass. Full setter API, native libraries, remaining commands/features, real package import, Android build and device validation are still open. See `STEP33_NOTES.md`.

## Step 35 — asynchronous Lua object lifecycle (2026-10-11)
- Fixed `LuaServiceApi.regGc()` previously being empty by tracking `LuaGcable` objects and adding `unregGc()` to the LuaContext lifecycle contract.
- Prevented normal script completion from closing the Lua state while registered asynchronous tasks/threads/timers still need it; state closes when scheduled work and registered objects are idle.
- Added unregister paths for async task completion/cancellation, Lua thread termination, and one-shot timer callbacks.
- Added `devtools/step35/verify_step35.py`; static checks only. Android build/device behavior is unverified.
- Still open: native modules, exact API/setter parity, unsupported commands needing new features/design, real .ppk package tests, and GitHub Actions build.

## Step 37 (2026-10-11) — fix live setter mappings + guard HTTP errors
- Fixed a Step 36 bug: real settings mappings were writing into the separate `jx_compat_settings` preference file, while MobinSR features read TalkBack's normal preference store. Real mappings now write to `SharedPreferencesUtils` preferences.
- Added actual mappings for `setUseLog`, `setAutoTrans`, `setUseHotkey` (inverted flag), and `setIncallMode` / earpiece mode. Earpiece command and direct API setter share the same helper.
- HTTP `get`, `post`, `download`, and multipart `upload` now guard top-level network/file exceptions and attempt connection cleanup. Success return shapes remain unchanged; failures can return an extra error string.
- Added `devtools/step37/verify_step37.py`; all Step 30–37 static scripts passed.
- Still not done: native `bson/cjson/crypt/regex` integration, real behavior for stored-only setter placeholders, remaining command features, Android build, Lua runtime tests, phone tests, real `.ppk` compatibility. Do not claim full Jieshuo parity.
