# Step 45 — Restore from a Jieshuo backup, part 3: settings + gestures (2026-10-13)

**हिंदी सार:** Advanced settings > Backup and restore > "Restore from Jieshuo backup" अब आपकी Jieshuo सेटिंग्स और gestures भी लाता है (जहाँ MS में वही सेटिंग है)। जो नहीं आया, उसकी पूरी सूची एक text फ़ाइल में सेव होती है। Keys, tokens, passwords कभी नहीं पढ़े जाते।

Builds on STEP43/STEP44 (same screen, same zip). Code:
- `backup/JxSettingsMapper.java` (pure Java) decides Jieshuo key -> MS key and value.
- `backup/MsPrefCatalog.java` is GENERATED (`python3 devtools/step45/gen_catalog.py`): every `ms_*` setting with its type and, for fixed lists, its allowed values. Run it again whenever a settings screen changes.
- `backup/JieshuoSettingsRestore.java` reads the settings `.dat` (whitelist reader), writes to the default preferences, calls the gesture restore, writes the report file.
- `editor/JxGestureImport.java` (pure) + `editor/JxGestureApply.java` (Android) + `editor/JxFunctionAliases.java` (other English spellings of function names).
- `JieshuoRestore` now also takes `Gestures/**` out of the zip and shows the new counts; `JieshuoRestoreActivity` text updated.

## What is restored (rule: nothing is guessed)
1. **Same name in MS**: Jieshuo key `x` -> MS key `ms_jx_x` when that setting exists (e.g. double_fling_time, ocr_level, edge/fingerprint gesture actions, typing switches).
2. **Hand-checked table** (`JxSettingsMapper.SAME_MEANING`, ~45 pairs): clicking/navigation (single tap, lift to activate, hold duration, wrap navigation ...), volume keys, headset, shake, notification options, caller announcement options, dictionary on/off, filter, second-voice options, timed announcement (use_alarm/interval/from/to).
3. **Actions** (volume keys, shake): Jieshuo action name -> MS action ("Voice assistant" -> assistant, "Open the notification panel" -> notifications, ...). Actions MS does not have (Timer, Increase volume, "Suspend speech feedback ...") are NOT restored; MS keeps its own value.
4. **Voice engines** `tts_engine` / `async_tts_engine` only if that engine package is installed on this phone.
5. **Function lists** (fingerprint, edge, multi-part prefs): Jieshuo function name -> MS function id through the function table (+ `JxFunctionAliases`, e.g. "Recent apps" -> "Recent applications").
A value is written only if it has the type of the MS setting and, for a fixed list, is one of its values. Existing MS values for these settings are REPLACED (this is an explicit restore).

## Never read
Names with token, api key, secret, password, user/account ids, uuid, git user, Gemini keys, base/download URLs, update/log/crash state, setup flags. 27 keys on the real file. The Gemini key file is still not copied (Step 44).

## Gestures
- Global scheme = the folder named in Jieshuo's `gesture_package` (here "Custom"): each non-Default entry goes to MS's global gesture scheme (`ExtensionGestures`), replacing that gesture only.
- Per-app schemes = the other folders in `Gestures/` (app name -> installed app by label). Switched on if the app is in `app_gesture_package`.
- Targets: `Function/X` -> MS function (`cmd`), `Plugins/X` -> extension (folder name matched with trim, case ignored), `Tools/X` -> tool, `Application/X` -> a small extension "Open X" is created (`service.startApp("X")`) and assigned.
- Multi-part gestures: `<first>_double_fling_<second>_gesture` + `use_<first>_double_fling_gesture` -> MS multi-part pairs (`MsMultiPartGestures.key`), plus the "mp on" switch for first parts that were on.
- Left out and listed: taps, two/three/four-finger swipes, long swipes, edge gestures (MS has no slot for them in the schemes), functions MS does not have, extensions or apps that are not there.

## Verified (on a computer, on the real backup `mobin_niv_bakkap.zip`)
`python3 devtools/step45/verify_step45.py <backup zip>` -> 6/6 PASS.
- Settings: 426 keys = 154 restored + 14 not restored (value does not fit / no equal action) + 226 no MS setting + 27 private + 5 handled elsewhere.
- Gestures with all three extension names and apps present: 8 global, 4 in app schemes (Gemini, WhatsApp), 9 multi-part actions + switch, 2 "Open app" extensions, 5 left out (2-finger taps x2, long swipe x1, "Advanced text copying" function x1, and the per-app 2-finger tap).
- No key, token or account value in the plan; volume-key action and swipe examples checked by name.
- `devtools/compile_check.py` clean; the 8 new/changed files also compile clean with the extra check on `backup/`.

## NOT verified
- NOT built with Gradle, NOT run on a phone. `JieshuoSettingsRestore`, `JxGestureApply`, the activity text and `JieshuoRestore` changes only passed javac with android.jar.
- Some settings are read once at service start: restart the screen reader after restoring.
- "Open WhatsApp"-style extensions depend on `service.startApp` finding the app by its label.
- Jieshuo `use_screen_on_speak` and friends were NOT mapped: MS's "Speak lock state" settings mean something else (the wake announcement is `ms_adv_unlock_announce`).

## Not restored on purpose (no MS setting or meaning differs): 226 keys
TTS speed/pitch/volume per engine (Jieshuo 0-100 scale vs MS rate), sound_volume, time_announcer_* (Gemini voice announcer), timer_* state, auto_speak_apps, notification black/white lists and content_blacklist JSON (format differs), hotkey schemes, profiles, view_type_move/web_type_move (different element ids), many Jieshuo-only options. The full list is in `Jieshuo restore report.txt` in the MS base folder after a restore.

## Next
- First GitHub Actions build and phone test of Steps 43-45 together (restore screen, report file, a few gestures).
- Candidates from the 226: per-engine speech speed with a scale conversion, notification app lists, the Gemini voice announcer, hotkey schemes, `command_long_copy` / "Advanced text copying" naming.
