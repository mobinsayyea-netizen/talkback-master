# Step 28+ plan: finish the Lua extension/tool system (written BEFORE building, 2026-10-11)

**हिंदी सार:** यह फ़ाइल उस नई चैट या दूसरे अकाउंट के लिए है जो टोकन खत्म होने के बाद काम आगे बढ़ाए। इसमें लिखा है कि हमें क्या-क्या बनाना है, कहाँ से कौन-सी फ़ाइल लेनी है, क्या पक्का है और क्या अंदाज़ा। हर काम के आगे [ ] या [x] है; बनाते समय मैं इसे बदलता जाता हूँ। नई चैट में यह लिखिए: "PLAN_STEP28_LUA_COMPLETION.md पढ़ो और बचा हुआ काम पूरा करो"। कोई और फ़ाइल देने की ज़रूरत नहीं: ज़रूरी स्रोत और संदर्भ इसी zip के अंदर हैं (नीचे "Where everything is")।

## Rules from the user (Mobeen)
Reply in Hindi, short, phone-sized. Say "दृष्टिबाधित", never "अंधे". He cannot run code or tests; he builds the APK on GitHub Actions himself and reports in words, so do NOT spend effort on the build; be honest about what is unverified. Ask before creating files unless he says "बनाओ"/build. Low quota: reuse TalkBack's own code as much as possible, take only STRUCTURE from Jieshuo. Do not touch key/package/signing (HANDOFF.md). Goal: extensions and tools must have the SAME structure and the SAME `service.*` API as Jieshuo (error margin ~zero).

## Where everything is (nothing else needs to be shared)
- This repo zip = the whole project. Notes: LUA_IDE_PROGRESS.md (history), STEP14..STEP27_NOTES.md, NEXT_STEPS_ACCESSIBLE.md, JX_API_REFERENCE_zh.txt (Jieshuo's own API doc, 103 items), JX_EXECUTE_COMMANDS.txt (173 commands), JX_SERVICE_SIGNATURES.txt (every public method of Jieshuo's service with types), REMAINING_COMMANDS.txt.
- `reference/jieshuo_tutorials_26_videos_audio_text.txt` = the 26 Jieshuo advanced tutorials (speech to text, Roman Hindi, words can be wrong). `reference/jieshuo_class_52_multipart_audio_text.txt` = classes incl. multi-part gestures.
- `reference/androlua/UPSTREAM_SOURCE_MAP.md` documents the AndroLua_pro source locations; the actual upstream Java source tree is not included. `reference/jieshuo_smali/` contains selected smali reference files extracted from the user-provided `cSr.zip` in Step 31; these are not build inputs.
- Jieshuo decompile (cSr.zip) is too large to store; the facts taken from it are in this plan and in the JX_* files.

## Facts found in Step 28 research (verified by reading files)
1. Jieshuo's own Lua modules are only: import, loadbitmap, loadlayout, loadmenu, logcat, permission, su, console, bmob, test. It has NO LuaSocket/ltn12/mime/http.lua. Its HTTP is Java: `com.androlua.Http` and `Download`. So: do NOT add native socket.
2. Jieshuo ships 4 Lua-side native libs: libluajava (Lua 5.3 with bit32 and bson inside), libcjson, libcrypt, libregex. We had only lua + luajava. Its JNI names differ (extra `_checkVip`, `_setCpu`); irrelevant, we compile our own pair.
3. Jieshuo's import.lua and ours are almost identical (502 vs 503 lines). Both bind `com.androlua.LuaAsyncTask`, `LuaThread`, `LuaTimer` at load time. We lacked those classes, so `require "import"` very likely failed in every script. Fixing this is the first job.
4. Structure from the tutorials: Function, Extension (= plugin), Custom voice command, Tool all use the same code pattern and are interchangeable. Extensions list: keyword filter box + New; tap = execute; More options = Edit, Download, Extension manager; long press (double-tap-hold) = Delete, Rename, Share. Editor: top row path, undo, redo, search, recent files, open, save, new file, open project, new project, format, analyze imports, API help; bottom Full text, Paragraphs, Lines, Custom, Characters, OK; Execute and Insert. Insert menu: function, plugins, tools, custom voice command, application, automatic click. Insert automatic click turns one-button-per-line text into ONE table: `if service.click{{"a","b","c"}} then return true end` (all lines = one sequence).
5. Tutorials use `service.openUrl(url)`, `service.playSoundTick()/Clock()/Scroll()`, `service.speak(text)` (main/secondary TTS), `service.paste(text)`.

## Work list (do in this order; mark [x] when done; all UNVERIFIED until a GitHub build passes)
### A. Engine pieces (fixes the import bug)
- [ ] A1 Compatibility Java classes/helpers now exist in the project, but are not verified ports of upstream AndroLua. Step 35 fixes lifecycle registration; exact parity still needs build/runtime tests. Step 29 adds a guarded Lua-level fallback in `import.lua` using MobinSR’s existing serialized scheduler, so `require "import"` no longer immediately fails just because the three helper classes are absent. This is a compatibility fallback, NOT full Java class parity; copy the original classes when their source becomes available.
- [ ] A2 `Http.java` and `Download.java` compatibility implementations exist, but are not verified ports of the original source; not copied or fabricated. Step 31 adds a Java-URLConnection-based Lua `http` compatibility module, but it is not the original Java class and is not runtime-verified.
- [ ] A3 Natives: add `bson` into the lua lib, and separate shared libs `cjson`, `crypt`, `regex` (sources from AndroLua jni/, `Android.mk` lines in `talkback/src/main/jni/Android.mk`). No socket, no luagl/tcc/lluv/sensor (size).
- [x] A4 Replaced runtime `assets/lua/http.lua` with a Java `URL`/`HttpURLConnection`-based compatibility module exposing `get`, `post`, `download`, and multipart `upload`; it no longer requires LuaSocket/ltn12/mime. The previous LuaSocket file is preserved under `reference/legacy_http_luasocket.lua`. Static checks pass; Android/Lua runtime behavior remains UNVERIFIED.
### B. Small API and editor gaps (from the tutorials)
- [x] B1 Editor "Insert automatic click" (`LuaEditorActivity`) now generates Jieshuo's one-table form with `if ... then return true end`; not yet verified by Android build/runtime.
- [x] B2 Added `openUrl`, clock/scroll/gesture/action-item sounds and the documented overload signatures in `LuaServiceApi`, using TalkBack sound slots. `int`/`float` overload parameters are currently compatibility-only and ignored; verify intended parameter semantics against Jieshuo before claiming exact parity. Not yet build/runtime verified.
- [x] B3 Verified existing `ExtensionListActivity` already has keyword filtering, long-press Delete/Rename, and Send via Android share chooser; no duplicate UI added.
### C. Jieshuo parity of the service (the rest of LUA_IDE_PROGRESS.md "REMAINING WORK")
- [ ] C1 Hooks: Step 33 added opt-in `onKeyEvent` and `onGestureDetected` (true consumes) and async `onVolumeChanged`; other hooks from `LUA_IDE_PROGRESS.md` and Android latency/runtime verification remain.
- [ ] C2 Step 33 added common forwarding methods (`startActivity`, `performGlobalAction`, `getRootInActiveWindow`, `getWindows`, `getServiceInfo`, `setServiceInfo`, context/resource accessors); compare the remaining public API against signatures and verify on Android.
- [ ] C3 API surface: Step 36 added 230 missing scalar/String void setter signatures from JX_SERVICE_SIGNATURES.txt (including 112 setUseXxx boolean setters) as safe compatibility methods; 4 exact settings mappings are confirmed, other values are stored only and are NOT feature implementations. Remaining callback/object overloads and real behavior mappings still open.
- [ ] C4 Step 33 added path aliases for Plugins/Extensions, Tools, Custom voice commands, Timer, Profiles; full data-format and behavior compatibility remains unverified.
- [ ] C5 Auto-click leftovers: colour search `[x1,y1,x2,y2,r,g,b,d]` was done in Step 8 (phone test pending); "Actions" command; `{{">Files", this.speak(...)}}` form.
### D. Cannot be finished without a new feature or a phone (write down, do not fake)
- ~60 execute() commands that return false (camera, voice dictation, cloud clipboard server, notification store, virtual screen, focus monitor, screen grid, suspend-type switches needing a design decision). List: REMAINING_COMMANDS.txt, STEP14/15 notes.
- Real Jieshuo .ppk packages may not open (our .ppk is our own zip layout): test with one real package.
- First GitHub Actions build and fixing errors (the user does the build and sends the log).

## After each item
Update this file ([x] + one line of what was done and what is still unverified), add a section to LUA_IDE_PROGRESS.md, keep a pure-Java test where possible (devtools/*_test, run with `java -m jdk.compiler/com.sun.tools.javac.Main` then `java -cp`).

## Status log (newest last)
- 2026-10-11 Step 35: fixed Lua async-object lifecycle tracking (`regGc`/`unregGc`) so the Lua state is not closed while registered tasks/threads/timers still depend on it; one-shot timer cleanup added. Static checks only; no Android build/device test.
- 2026-10-11 Step 31: replaced the runtime LuaSocket-dependent `http.lua` with a Java networking implementation; extracted seven Jieshuo `.smali` files from the user-provided `cSr.zip` into `reference/jieshuo_smali/` for signature/dependency inspection only (not compiled); added `devtools/step31/verify_step31.py`. Static checks pass, but Java helper classes and native modules remain absent; no Android build/device test.
- 2026-10-11 plan written, nothing built yet.
- 2026-10-11 Step 28: B1 auto-click snippet updated to one-table sequence; B2 service API surface added; B3 existing search/delete/rename/share behavior confirmed. Unverified until GitHub Actions/device tests.
- 2026-10-11 Step 29: `import.lua` now probes optional AndroLua task/thread/timer classes with `pcall` and has a fallback that registers work with `service.scheduleTask`/`__jx_run`; `LuaServiceApi.doFile()` now passes positional arguments to Lua chunks. Static checks only; no Android build or Lua runtime test. Full Java helper classes, HTTP/Download classes, and native libraries remain outstanding.
- Blocker found: this ZIP does NOT contain `reference/androlua/` or source files `LuaAsyncTask.java`, `LuaThread.java`, `LuaTimer.java`, `Http.java`, `Download.java`, despite the plan saying the AndroLua source was included. A1/A2 and native library additions cannot be safely completed from the supplied ZIP without those sources. No substitute classes have been fabricated.


## Step 30 (2026-10-11) — source audit and safe integration preparation [UNVERIFIED]
- Checked the upstream AndroLua_pro public repository and located source references for `LuaAsyncTask.java`, `LuaThread.java`, `LuaTimer.java`, `LuaTimerTask.java`, `Http.java`, `Download.java`, `TimerX.java`, `TimerTaskX.java`, and `AsyncTaskX.java`.
- The uploaded MobinSR ZIP still does not contain these upstream source files. This environment could inspect the upstream pages but could not download/checkout the repository into the project workspace. Therefore these classes were NOT copied into the app and no substitute stubs were invented.
- Added `reference/androlua/UPSTREAM_SOURCE_MAP.md` with source locations, dependencies, and safe porting checks so the source gap is explicit and reproducible.
- Added `devtools/step30/verify_step30.py`, a local static audit for key Step 28/29 source markers and source-presence status. This checks source text only; it does not compile Java, execute Lua, or prove Android behavior.
- No package ID, signing configuration, or keys changed.
- Remaining blocker: actual upstream Java/helper and native sources must be brought into the workspace before A1/A2/A3 can be completed safely.


## Step 32 status update (2026-10-11)
- [x] Added local compatibility implementations for LuaAsyncTask, LuaThread, LuaTimer, LuaTimerTask, Ticker and helper bases. **Not upstream-identical and not Android-build-verified.**
- [x] Added a common-case Java Http facade and DownloadManager-backed Download helper. **Not full Jieshuo overload parity; not runtime-verified.**
- [x] Implemented LuaServiceApi.call() using the existing Lua-state lock; set() now uses that lock too.
- [x] Expanded reference-only Jieshuo Smali files for the runtime/helper classes.
- [ ] Native cjson/crypt/regex source integration and bson remain open; proprietary prebuilt binaries from cSr.zip were not copied.
- [ ] Complete all Http overloads and exact task/thread/timer semantics; run GitHub Actions build and phone tests.
See STEP32_NOTES.md for the exact limits and test status.


## Step 33 status update (2026-10-11)
- Added opt-in key and gesture hooks with explicit `true` consumption, plus asynchronous volume-change hooks. Source checks pass; runtime timing and hook semantics remain unverified.
- Added common AccessibilityService forwarding and folder path aliases. This is partial parity, not complete API parity.
- Native libraries/bson, full `setUseXxx`/`setXxx` mappings, remaining hook types, real `.ppk` compatibility, Android build and phone tests remain outstanding. See `STEP33_NOTES.md`.


## Step 34 update (2026-10-11)
- Added a limited `cjson.lua` compatibility shim backed by bundled JSON4Lua. This is not a native cjson module.
- Native bson/cjson/crypt/regex integration, full setter parity, real `.ppk` tests, Android build, and phone tests remain open.


## Step 36 status update (2026-10-11)
- Added a bulk compatibility surface for 226 missing scalar/String `void set...` overloads from the signature reference, including 112 `setUseXxx(boolean)` setters. Values are persisted separately and can be inspected by scripts.
- Confirmed behavior mappings for OCR, edge gesture suspension, multi-finger gesture suspension, and accessibility-volume preference. Other setter values are stored-only placeholders, not claimed functional parity.
- Added `JX_PARITY_AUDIT_STEP36.md`. Static signature checks pass; Android build/device tests are still required. Native modules and full feature behavior remain major open work.

## Step 37 status update (2026-10-11)
- Fixed live setter mapping storage to use TalkBack's actual feature preferences rather than `jx_compat_settings` for mapped settings.
- Added direct behavior mappings for logging, automatic translation, hotkeys, and earpiece mode, in addition to OCR, edge/multi-finger gestures, and accessibility volume.
- Added guarded error returns for HTTP public functions.
- Static checks from Steps 30–37 pass. Android build/runtime remains unverified.
- Native `bson/cjson/crypt/regex` integration and true implementations for stored-only API placeholders remain major open tasks.


---
## Step 38 status (2026-10-11)
- A1/A2 (real AndroLua LuaAsyncTask/LuaThread/LuaTimer/LuaTimerTask/Http/Download/Ticker + util): DONE in source, compiled against android.jar; not run on a device.
- A3 (native bson, cjson, crypt, regex): DONE in source; host-tested; NDK build not yet run. cjson.lua shim removed.
- C3 setter count is 226 (not 230).
- Open: first real Actions build, device tests, Jieshuo .ppk import, remaining stub commands, extra upstream natives (lfs, zip, socket, lsqlite3).
- Details: STEP38_NOTES.md; checks: devtools/step38/verify_step38.py.

## Step 39 status (2026-10-11)
- All 174 Jieshuo command ids now have a handler (17 added in MsNavCommands + command_clean). Earlier "~60 false" figure was wrong.
- Open: stored-only setters, first Actions build, device tests, Jieshuo .ppk import, long-press/double-tap timing options (user to choose), extra upstream natives.
- Details: STEP39_NOTES.md; checks: devtools/step39/verify_step39.py.

## Step 40 status (2026-10-11)
- Four gesture speed settings added (swipe length, hold time, long-click extra, scroll read wait); defaults unchanged. See STEP40_NOTES.md.
- Open: phone tuning of the values, fast scroll via swipe, stored-only setters, first Actions build.

## Step 41 status (2026-10-11)
- Gesture speed applies live with faster defaults; 3 more setters are real; devtools/compile_check.py added.
- Remaining stored-only setters (~204) have no feature behind them (see STEP41_NOTES.md): feature work, not wiring.
- Open: first Actions build, phone tuning, Jieshuo .ppk import, fast scroll via swipe (optional).
