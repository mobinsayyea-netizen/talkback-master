# Step 41 — gesture speed applied live, real setters, compile check (2026-10-11)

## Done
- Swipe length and hold time now apply as soon as the setting changes (Swipe keeps a weak registry and recomputes its thresholds; GestureManifold listens to the two preferences).
- Defaults are now the faster values: swipe 0.6 cm, hold 65 % of the phone's touch-and-hold delay, service long-press extra +80 ms, wait after scroll 400 ms. Anyone who already picked a value keeps it. Change them in Settings > Operation > Gestures (first list entry = old behaviour).
- Setters now writing real settings: setDoubleFlingTime, setUseFingerprintGesture, setUseFloatMenu (plus the 8 from Step 37).
- New tool `devtools/compile_check.py`: runs javac on our code in a few seconds and prints only real errors (it ignores what it cannot see: generated R, androidx/guava annotations, TalkBackService parent class). Tested: clean on the current tree; catches an injected typo. Run it before every Actions build.

## Finding about the remaining setters
- The settings screens contain 810 preference keys (copied from Jieshuo's structure), but only about 65 keys are read by any Java code.
- Of the 207 stored-only setters, 196 have no matching key at all; 2 match a key nothing reads; 9 matched by name, of which only 3 are backed by a feature (done). ms_jx_game_mode is an app-list setting, not a switch, so it was left out.
- The 5 typing setters (setUseInputMethodDoubleTap/SpeakCommit/SpeakCommitSpace/SpeakWord, setUsePinyinSpeak) only appear on a settings page; no behaviour reads them. Wiring them would change nothing until the feature itself is written.
- So the remaining stored-only setters need the features written first, not wiring. That is feature work, not a quick fix.

## Verified
- verify_step41 6/6, step38 23/23, step39 5/5, step40 23/23; steps 30,31,33,36,37 still pass.
- compile_check.py: no real errors in editor/*, androlua/*, utils/gestures/*.

## NOT verified
- No Gradle build, no phone test. The speed values are a first guess to tune on the phone; below about 0.5 cm or 50 % gestures may trigger by accident.
- Scroll distance/speed is still decided by each app.
