# Step 48 — Why Jieshuo extensions did not run, and the crash shield (2026-10-19)

Built on GitHub `main` (same as snapshot 551b7a2). Includes Step 46 and Step 47, which were not in `main` yet.

**हिंदी सार:** आपके बैकअप की सारी 58 स्क्रिप्टें (39 एक्सटेंशन + 17 टूल में से Lua फाइलें) ऐप के असली Lua इंजन से लोड करके जांची गईं (इंजन को Linux पर इसी के सोर्स से बनाया)। 47 ठीक थीं, 11 नहीं। नीचे हर कमी और उसका सुधार है। साथ में एक्सटेंशन के थ्रेड/टाइमर की गड़बड़ से पूरा ऐप बंद न हो, इसकी सुरक्षा।

## Result of loading all 58 scripts with the app's own Lua engine
- 47 load fine (the app's Lua already allows `if cond` without `then`).
- 10 start with `=`: author-locked/encoded scripts that only Jieshuo's own native engine opens. NOT supported and not worked around (Jieshuo itself refuses to show them). Extensions: OpenAI, Text To Audio, Master Typing, Convert text to audio, Advance describer with gemini, sistem audio cover br apon. Tools: ToolNova APK Manager v1.5, Audio cinemateca, add text on image, WhatsApp chat link generator. They need an unlocked version from the author, or a new extension written by us.
- 1 precompiled script (`Voice recorder 2.0`) was made by a 32-bit Lua: the app's loader said `size_t size mismatch`.

## Fixes
1. **Bytecode from 32-bit phones** (`jni/lua/lundump.c`): the loader now accepts a chunk whose `size_t` is 4 or 8 bytes. Tested: `Voice recorder 2.0` loads with the app's engine on Linux.
2. **Missing `service.*` functions** (`editor/LuaServiceApi.java`), found by comparing every `service.X` the scripts call with what the app has:
   - `handler.post/postDelayed(fn)` (main thread), `timer(fn, ms)` (once), `task(fn)` (own Lua state via `LuaThread`, so a slow web request cannot block the app; fn must not use outer variables, same as AndroLua).
   - `regEvent("onTimer", fn)`: calls fn every interval (`setTimerInterval`, default 1 s, 0.2–60 s) while `setTimerMode` is not false; the Lua state stays open while an event is registered; the same extension started again replaces its older copy; 20 errors in a row stop it; `regEvent("onTimer", nil)` removes it. Other event names are written to the script log and ignored.
   - `setTimerMode(on)` is now only the on/off switch of onTimer. Before it started the stopwatch and said "Timer started".
   - `getAllText()`, `getFocusView()`, `getNodeInfoLabel(node)`, `findAccessibilityNodeInfoByText(text[, index])` (returns the first element or nil, so `if service.find...("Skip") then` works), `stop()` (stops speech), `installApp(path)` (FileProvider), `deleteApp(package)`. Manifest: added `REQUEST_DELETE_PACKAGES`.
3. **Crash shield:** `LuaThread.run`, `LuaTimerTask.run` and `LuaAsyncTask.doInBackground` now catch every Throwable and report it with `sendError` (before, only `LuaException` was caught). `TalkBackService.uncaughtException`: an error on a background thread whose first frames are in `com.androlua`, `com.luajava` or `editor/Lua*` is written to the error log and does not end the process (same idea as Jieshuo's crash handler). Main-thread crashes and all other crashes still go to the system as before.
4. **Step 46:** `LuaDialog`, `SingleLineAdapter`, `import "com.nirenr.talkman.TalkManAccessibilityService"` alias (see STEP46_NOTES.md).
5. **Step 47:** sound themes connected to the service (see STEP47_NOTES.md).

## Checked here / NOT checked
- Checked: all 58 scripts loaded with the app's own Lua sources; patched `import.lua` and `Voice recorder 2.0` load; the changed Java files have no syntax errors (javac parse; the symbol errors are only the missing Android SDK).
- NOT checked: no Android SDK here, so nothing was compiled into an APK or run on a phone. Run the GitHub Actions build first. A script that loads can still fail inside (wrong key, no internet, Android version).

## Heads-up
- `Extensions/Gemini text summarizer` has a Google API key written inside the script. If that script was shared with anyone, create a new key and change it in the script.
- `Gemini I text to audio by mobeen` sends the table `state` into `thread(...)`. A thread has its own Lua state, so `state.isGenerated = true` may not reach the main script, and PLAY may say "Please generate audio first". Test on the phone; fix is to keep the flag in `service.setSharedData`.

## Phone tests
1. Backup extensions `my`, `ded and time`, `sinema`: start one, announcements/loop should run; the app must keep working.
2. `11 लैब`, `Gemini text summarizer`: background request runs, the app stays responsive, the spoken result arrives via `handler.post`.
3. `apps store by nitin kumar tool`: Install the App opens the installer; Uninstall opens the uninstall question.
4. `Voice recorder 2.0` starts.
5. `application menu`: dialog with app list opens.
6. Sound theme: restored sounds play (Step 47).
