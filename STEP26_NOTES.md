# Step 26 - Multi-part gestures (2026-10-10)

**हिंदी सार:** मल्टी-पार्ट जेस्चर अब असल में काम करने के लिए बने हैं (Jieshuo क्लास 52 के हिसाब से): पहला पार्ट (24 जेस्चर में से कोई), उँगली उठाओ, फिर तय समय के अंदर दूसरा पार्ट। दूसरा पार्ट समय पर आया और उस पर काम रखा है तो वही काम चलता है; नहीं आया तो पहले जेस्चर का अपना सामान्य काम उतनी देर बाद चलता है। कुछ भी कंपाइल या फ़ोन पर परखा नहीं गया; सिर्फ़ स्टेट-मशीन कंप्यूटर पर परखी गई।

## How it works (from the class 52 transcript)
- Part 1 = one of 24 gestures: the 16 one-finger swipes + 3-finger and 4-finger swipes (up/down/left/right). Each has its OWN switch "Use ... multi-part gesture", off by default. Only a gesture whose switch is on waits; every other gesture has no delay.
- Part 2 = any of 37 gestures: the 16 swipes, 2/3/4-finger swipes (12), 2/3/4-finger single/double/triple taps (9). (The transcript says 42; the other 5 are probably "slow"/long gestures, which this app does not have.)
- Time between parts: setting `ms_jx_double_fling_time` (300 to 1000 ms, default 500). Setting `ms_jx_use_double_fling` is the global switch.
- Part 2 in time and has an action: that action runs, part 1's own action does NOT. No part 2 in time: part 1's own action runs after the wait. Part 2 has no action: part 1's own action runs at once and the new gesture is handled as usual (it may itself start a new part 1).
- Same gesture twice (right, right) works. Sounds: gesture_end sound after part 1, gesture_begin sound when part 2 runs (both follow the sound theme).

## Code
- `editor/MultiPartCore.java` - the state machine, pure Java (tested).
- `editor/MsMultiPartGestures.java` - ids and names (24 + 37), key numbers, engine glue (Handler timeout, sounds, runs the action), `intercept()`.
- `editor/MultiPartGesturesActivity.java` - screen: level 1 = 24 part 1 gestures (on/off, how many set); level 2 = the switch + 37 part 2 gestures; tap one to choose: no command / Screen reader action (TalkBack) / Command / Extension / Tool. Setting an action also switches part 1 on. `EXTRA_PKG` edits one app's scheme (not yet opened from the per-app list, see below).
- `TalkBackService.handleOnGestureById`: after the pause/blocked checks, `MsMultiPartGestures.intercept` runs first; the rest of the old method moved into `handleGestureAfterMultiPart` (also used when part 1 times out via `msRunGestureNormally`). New `msRunTalkbackAction(key)` runs a TalkBack shortcut action. `onUnbind` resets a waiting part 1.
- Saving: old scheme text format, no format change. Number = 100000 + part1 * 1000 + part2; part2 = 0 is the switch (kind `mp`). New kind `tb` = a TalkBack shortcut action key (list taken from the `shortcut_*` arrays). Global scheme = ExtensionGestures, per-app = AppGestureSchemes (foreground app first, then global), so backup / per-app work with the same code as Step 18.
- Settings: Operation settings > Gestures default settings: "Use multi-part gestures" and the duration are live (the "Saved. Not active yet." note is gone). "Multi-part gesture settings" now opens `MultiPartGesturesActivity`. The 17 old small screens (4 swipes each) are not opened from anywhere and removed from settings search; their files stay in the code.
- Manifest: new activity entry.

## Tested on a computer
`devtools/step26_test` (README inside): 10 groups, ALL PASS (part 2 in time, timeout, part 2 unassigned, chained part 1, right-right, switch off, global off, duration, reset, unique keys). The Android classes were only syntax-parsed (only "no Android jar" errors). Nothing compiled.

## Limits and open points (be honest in testing)
1. 3-finger and 4-finger gestures only arrive when "use multi-finger gestures" is on (Android 11+). 
2. A part 1 with its switch on is delayed by the duration when no part 2 follows (as in Jieshuo). Keep the switch off for gestures that must be instant.
3. Per-app: `MultiPartGesturesActivity.show(ctx, pkg)` exists, but the per-app scheme list does not open it yet (the app scheme screen is still ExtensionGesturesActivity). Foreground app's scheme is already read by the engine, so schemes edited later work. Next small step: a row "Multi-part gestures" inside the per-app scheme screen.
4. Character-by-character mode, gestures blocked by "no multi-finger", and the paused-gestures flag behave as before (checked before multi-part).
5. `devtools/step19_gen/gen_settings.py` was not changed: re-running it would restore the old multi-part entry and the "Not active yet" note.
6. Not done: part 2 with slow/long gestures (none exist here), sharing a scheme.

## Phone test after build
1. Operation settings > Gestures default settings: turn on "Use multi-part gestures", duration 1000 for a start.
2. Multi-part gesture settings > Swipe up then right > tap "Swipe up" (second part) > Command > "Speak time" (any), the switch turns on.
3. Do "swipe up then right", lift, swipe up within 1 s: the time is spoken (sound after part 1, another after part 2). Do it and wait: the normal action of "swipe up then right" runs after about 1 s.
4. Do "right, right" with Swipe right set under Swipe right.
5. Check a gesture without a switch has no delay.
