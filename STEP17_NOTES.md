# Step 17 - packages, resource manager, extension dialog, gestures, switches, hooks, editor tools (2026-10-10)

**हिंदी सार:** इस जिप में Step 16 के सारे 5 काम और Step 17 के editor वाले काम हैं: .ppk/.xpk import-export, Resource manager, डिफ़ॉल्ट "Extension" डायलॉग, तीन स्विच, एक्सटेंशन को जेस्चर पर लगाना, कुछ hooks, और editor में Format code / Analyze imports / service. सूची / API help। कुछ भी कंपाइल या फ़ोन पर परखा नहीं गया। सबसे पहले GitHub build चलाइए और त्रुटियाँ भेजिए।

## What is new (all UNVERIFIED on Android; not compiled)
New files in editor/:
- `ExtensionPackages.java` (pure Java): `.ppk` = extension, `.xpk` = tool, both plain zips (main.lua at the root; init.lua with appname/appver/ext is added on export if missing). Import accepts a zip with one wrapping folder, rejects "../" paths (zip slip), needs main.lua, max 60 MB / 3000 files. Also packAll/restoreAll (Extensions + Tools + Custom voice commands in one zip).
- `PackageImporter.java` + `PackageImportActivity.java`: import from a Uri; if the name exists asks Replace / Keep both / Cancel; a file that is neither .ppk nor .xpk asks Extension or Tool. The activity has intent-filters (VIEW for *.ppk/*.xpk, SEND for octet-stream/zip), so a file opened from WhatsApp/Telegram/file manager lands here. Export uses the existing FileProvider (`cache/export/` added to file_paths.xml).
- `ResourceManagerActivity.java`: Import, Extensions, Tools, Custom voice commands, Extension gestures, Save extensions to zip, Restore from zip, link to the existing full backup (SettingsBackupActivity, covers all settings).
- `ExtensionDialogActivity.java`: the default "Extension" function (execute("Extensions") now opens it): all extensions, then Manage, Custom voice commands, Cancel.
- `MsExtSettings.java`: switches `ms_ext_autostart` (service_started extensions), `ms_ext_voice_allow_ext`, `ms_ext_voice_allow_tool` (default all on). Wired in LuaExtensionManager.onServiceStarted and CustomVoiceCommands.match.
- `ExtensionGestures.java` + `ExtensionGesturesActivity.java`: the 16 one-finger swipes (GESTURE ids 1-16) can run an extension or tool. Hook is the first lines of TalkBackService.handleOnGestureById: an assigned gesture runs the script and does NOT run TalkBack's own action. Pref `ms_ext_gestures`. Rename/Delete in the list keep it in sync. Long-press > "Assign to gesture".
- `LuaFormatter.java` (indent by block keywords), `LuaImportAnalyzer.java` (adds `import "pkg.Class"` for ~170 known Android/Java class names), `ServiceApiList.java` (service. list by reflection over LuaServiceApi + API help from assets/ide/JX_API_REFERENCE_zh.txt), `FilterPicker.java`.
Edited: LuaEditorActivity (new "Code tools" button: Format source code, Analyze java package for imports, Insert service function, API help), ExtensionListActivity (Import button; long-press: Export package, Assign to gesture), JxPickerActivity, CustomVoiceCommands, LuaExtensionManager (opt-in hooks), LuaServiceApi (runHook), TalkBackService (gesture hook), AndroidManifest, file_paths.xml, ms_group_9_other.xml (new category "Extensions" in Settings > Other).

## Hooks (partial)
Opt-in header line in main.lua: `-- hook: onNotification, onWindowStateChanged`. Names wired: onWindowStateChanged, onNotification, onScrolled, onViewLongClicked; the function is called as f(package, text) after the script ran once (so top-level code also runs; global `service_hook` holds the hook name). NOT done: onKeyEvent, onGestureDetected, onVolumeChanged, onTransResult, onScreenCaptureDone, and the "return true = consumed" rule (the return value is ignored). Reason: Jieshuo's real arguments cannot be read from its obfuscated code; onKeyEvent needs a synchronous call on the service thread.

## Tested on a computer (devtools/step17_test)
ExtensionPackages (pack, import, wrapper folder, zip slip, no main.lua, packAll/restoreAll), LuaFormatter (indent, idempotent), LuaImportAnalyzer: ALL PASS. All 40 editor Java files + TalkBackService pass a syntax parse. Nothing that uses Android classes was compiled.

## Decisions (change if you disagree)
1. The .ppk/.xpk format is OUR zip layout; real Jieshuo packages might not open (unknown internal layout) - test with one if you have it.
2. "service." list = real public methods of LuaServiceApi (not Jieshuo's 1171). API help = Jieshuo's own help text (Chinese).
3. A gesture with an extension loses its normal action. Only the 16 one-finger swipes are offered.
4. Hooks are opt-in by header line so old extensions do not start running on every event.

## Phone tests after the first build
Open a .ppk from WhatsApp; export an extension and import it again (Keep both); Settings > Other > Extensions (3 switches, Resource manager, Extension gestures); assign Swipe up-then-left to an extension and swipe; Editor > Code tools > Format / Analyze imports / service list / API help; execute("Extensions") dialog; Save extensions to zip and Restore.

## STILL TO DO
FIRST: GitHub build + fix errors (NEXT_STEPS_ACCESSIBLE.md section E). Then: remaining hooks (onKeyEvent etc.), consume rule for gesture scripts, per-app gesture schemes / function manager (needs design talk), the 60 remaining execute() commands, Feedback screen links (mute-apps list, precise-sound switch, vibration intensity not linked in settings XML), server download/share, `.spk/.gpk/.vpk` packs.
