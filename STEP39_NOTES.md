# Step 39 — the last 17 Jieshuo commands (2026-10-11)

## Correction
Earlier summaries said "~60 execute() commands still return false". That was wrong. Counting real handlers (case labels, MAP.put in JxCommandTable/MsExtras, id arrays) only 18 of the 174 ids had no handler.

## Done
- New `MsNavCommands.java` (registered from JxCommandTable): previous/next window, heading, link, control, character, word, sentence, paragraph; `command_speak_net`; `command_clean` (reuses MsMenus.clearText).
- Heading/link/control walk the active window's node tree (heading = isHeading, link = clickable span or role "link", control = button/check/edit/switch/spinner/seekbar or a clickable labelled leaf) and move accessibility focus.
- Character/word/sentence/paragraph keep a reading position per node (editable fields move the real caret) using BreakIterator; tested on host JVM: English, Hindi word, emoji pair, edges (16 cases PASS, devtools/step39/SegmentationTest.java).
- Manifest: added ACCESS_WIFI_STATE (for Wi-Fi name/signal in command_speak_net).
- Found and fixed by javac: getRoleDescription() does not exist on the framework AccessibilityNodeInfo (read from extras instead).

## Verified
- devtools/step39/verify_step39.py 5/5; step38 23/23; steps 30,31,33,36,37 still pass.
- javac: MsNavCommands has no errors of its own (the only remaining message is s.getWindows() on TalkBackService, which cannot resolve here because its parent class comes from an external Google library; the same call compiles on AccessibilityService).

## NOT verified
- No Gradle/NDK build, no phone test; focus movement and window switching untested on a device.
- Setters from Step 36 that only store a value are still stored-only.

## Timing answer (double tap and hold, scroll)
- Hold time of the double-tap-and-hold gesture = system "Touch and hold delay" (GestureMatcher.completeAfterLongPressTimeout). Double-tap timeout = system value minus 50 ms (GestureConfiguration).
- Our long-click command waits system long-press + 150 ms (LuaServiceApi.java:987, JxCommands.java:397). Jieshuo's setLongClickTime is stored-only.
- Scroll down uses the app's ACTION_SCROLL_DOWN; its speed belongs to the app.
- Not changed yet: waiting for the user's choice (shorter hold via new preference, real setLongClickTime, or swipe-based fast scroll).
