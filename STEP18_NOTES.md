# Step 18 - per-application gesture schemes (2026-10-10)

**हिंदी सार:** अब हर ऐप की अपनी जेस्चर स्कीम बन सकती है (Jieshuo की "पर-एप्लीकेशन जेस्चर स्कीम" जैसी)। जिस ऐप के लिए स्कीम बनी हो, उसमें उसके जेस्चर चलते हैं; बाकी जेस्चर और बाकी सब ऐप में ग्लोबल स्कीम (Step 17 वाली "Extension gestures") चलती है। कुछ भी कंपाइल या फ़ोन पर परखा नहीं गया। पहले GitHub build चलाइए और त्रुटियाँ भेजिए।

## How Jieshuo does it (read from cSr smali + the CSR with MK class 57 / 48 transcripts)
- A scheme is a file in the gestures folder. The global scheme name is one preference; the per-app schemes are a second preference holding a SET of package names, and each scheme file is named exactly like the package.
- Class n2/j keeps one table per package (gesture -> action). On every swipe it asks which app is in the foreground: if that package has a per-app table with an action on that gesture, it runs; otherwise it uses the global table. So a per-app scheme only holds what the app changes.
- Creating one asks "Enable as per-application scheme". Cancel = scheme exists but is not used until ticked in the manager. Editing it also works from a developer function inside the app.

## What is new here (all UNVERIFIED on Android; not compiled)
New in editor/:
- `AppGestureSchemeData.java` (pure Java): text format of all schemes in one preference `ms_app_gesture_schemes` (a "@package TAB on/off" line, then "gestureId TAB kind TAB name" lines). Lookup, rename, remove, "anything on?" check.
- `AppGestureSchemes.java`: preference wrapper (create, delete, on/off, get/set/clear), foreground package from `getRootInActiveWindow()`, app label. When no scheme with a gesture is on, a swipe costs only reading one preference.
- `AppGestureSchemesActivity.java`: the manager. First item "Create a new per-application gesture scheme" (choose an app from a list; "has a scheme" is marked), then every scheme as "App, on/off, N gestures". Tap = edit, long press = turn on/off or delete (delete asks to confirm).
Edited: `ExtensionGesturesActivity` (with the extra `scheme_pkg` it edits one app's scheme; unset gestures say "uses the global scheme"), `ExtensionGestures` (handle(): foreground app's scheme first, then global; rename/remove of an extension or tool now also fix the app schemes), `ResourceManagerActivity` (new row "Per-application gesture schemes"), `AndroidManifest.xml` (new activity).

## Added in the second half of Step 18: commands on gestures
- In the gesture list, tap a gesture > "Command (a screen reader function)..." shows all execute() commands by English name; the chosen one runs on that swipe (stored as kind `cmd`). Works in the global scheme and in app schemes.
- `LuaServiceApi.executeStatic(ctx, name)` runs one command without a script (builds a Lua service object, calls JxCommands.run, worker thread). `CustomVoiceCommands.run` uses it for kind `cmd`. A command that returns false says nothing.
- New command `ms_edit_app_gestures` ("Edit gesture scheme of current app", our own, not Jieshuo's; row added at the end of JxCommands.TABLE, handler in JxCommandTable): opens the scheme of the app in front, creates it if missing. Put it on a gesture, or call service.execute("Edit gesture scheme of current app") from Lua. Same idea as Jieshuo's developer function.
- Correction: the Step 17 notes said "60 remaining execute() commands". That was stale: all 173 Jieshuo commands already have a handler (REMAINING_COMMANDS.txt: Remaining none; Step 14 added the last 30). Open items are compile + phone tests, left/right shortcut settings screen, cloud clipboard server, drawn screen grid, Jieshuo camera screen.

## Tested on a computer (devtools/step18_test)
`java SchemeTests.java` -> ALL PASS (round trip, empty scheme kept, off scheme ignored, free gesture falls to global, rename/remove in every scheme, broken lines, tabs/newlines in names). The 10 changed or new Java files pass a syntax parse (commands-on-gestures part: syntax only, no unit test) and the manifest is valid XML. Nothing that uses Android classes was compiled.

## Decisions (change if you disagree)
1. A scheme only changes gestures YOU set in it. Everything else in that app uses the global scheme (same as Jieshuo).
2. A new scheme is turned ON at once (Jieshuo asks first). Turn off with long press.
3. Same limits as Step 17: only the 16 one-finger swipes, and an assigned gesture runs the extension/tool INSTEAD of the normal action. The script's return value is still ignored.
4. A gesture can run an extension, a tool, or (added later in Step 18) any execute() command. Not yet: plugins, multi-finger / multi-part / long-slow gestures, sharing or uploading a scheme.
5. Foreground app = package of the active window's root node at the moment of the swipe.

## Phone tests after the first build
Resource manager > Per-application gesture schemes > Create > pick WhatsApp > set "Swipe left then up" to an extension. In WhatsApp that swipe runs it; in another app it does what the global scheme says. Long press the scheme > Turn off: the swipe goes back to global. Rename and delete an extension that is in a scheme: the scheme follows / drops it. Delete the scheme.

## STILL TO DO
Remaining hooks, consume rule, `.spk/.gpk/.vpk` packs, scheme share/upload. Then NEXT_STEPS_ACCESSIBLE.md section E: GitHub build + fix errors.
