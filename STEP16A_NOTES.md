# Step 16a - Jieshuo service API completed + Insert function with all commands (2026-10-10)

**हिंदी सार:** Jieshuo की आधिकारिक API सूची (`JX_API_REFERENCE_zh.txt`) के गायब 13 फ़ंक्शन अब `LuaServiceApi` में हैं, और एडिटर के Insert > Function में अब सारे 172 कमांड के नाम आते हैं (पहले सिर्फ 10)। कुछ भी कंपाइल या फ़ोन पर परखा नहीं गया।

## What changed
- `LuaServiceApi.java`: toNextChar, toPreviousChar, toNextLine, toPreviousLine (use the existing commands command_to_next/previous_char and _row), toTextStart/toTextEnd (cursor in a text box, else first/last visible item of the list), toListViewStart/toListViewEnd (first/last VISIBLE child of the nearest scrollable parent; it does not scroll further), ocr(node) (OcrTool on the node's bounds; off the main thread it waits up to 12 s and returns the text, on the main thread it speaks the result and returns null; Android 11+), setTimerMode(bool), setAutoReadEnabled(bool), setAutoNext(bool), loadGesturePackage(name) (returns false: no gesture packages yet).
- `JxCommandTable.java`: `timerSet(api, on)` for setTimerMode.
- `LuaEditorActivity.java`: Insert > Function lists every name of `JxCommands.TABLE` (172, "No command" left out) and inserts `if service.execute("<English name>") then return true end`. Two commands share an English name ("Tools", "Timer"); the first one wins on lookup.
- All 43 functions of the official list now exist by name. The official list was checked by script.

## Decisions (change if you disagree)
1. Lua "line" = TalkBack line granularity (same as the command "Previous/Next sentence").
2. ocr(node) on the main thread cannot wait, so it speaks instead of returning text.
3. setAutoReadEnabled(true) = reading from the top; setAutoNext(true) = reading from the focus (same as the two commands).

## Still to do (unchanged from STEP15_NOTES.md)
Step 16: .ppk/.xpk import-export + init.lua, resource manager and backup screen, default "Extension" dialog, autostart + voice-assistant switches, extension on a gesture. Step 17: Format source code, Analyze imports, API help, `service.` filter list (1171). Hooks other than onAccessibilityEvent (onNotification, onKeyEvent, onGestureDetected ...) are not wired. Feedback screen: mute-apps list, precise-sound switch and (Android 12+) vibration intensity are not linked in the settings XML. FIRST: GitHub build and error fixing.
