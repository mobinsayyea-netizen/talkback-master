# Accessible MobinSR — what is left and how to manage it (written 2026-10-04)

**हिंदी सार:** यह फ़ाइल बताती है कि क्या बाकी है, क्या जोड़ना है और काम कैसे चलाना है। नई चैट में लिखिए: "NEXT_STEPS_ACCESSIBLE.md और NOTES_ACCESSIBLE_STEP3.md पढ़ो और अगला काम करो"।

## How we work (decided with the user)
1. User reports in Hindi, short answers. AI builds only on his "बनाओ" / go-ahead; one question at a time.
2. Nothing can be compiled or run in the AI's sandbox. Loop = AI writes -> user builds on GitHub Actions -> user sends the error log (or the words) -> AI fixes. Expect 2-3 loops per batch.
3. Each zip = FULL repo with a unique name and a step number (Accessible_MobinSR_Full_Repo_StepN_date.zip). Never mix the old step2_auto_click_engine.zip into it (a parallel click engine; see below).
4. Do not touch key / package / signing (HANDOFF.md). Keep online features (translation, image recognition) working.
5. Jieshuo API names must stay the same (JX_API_REFERENCE_zh.txt, JX_SERVICE_SIGNATURES.txt).
6. Decided: NO AI/LLM fallback for unknown commands. Table-driven only.

## A. execute() commands — 82 of 173 done, 91 left (REMAINING_COMMANDS.txt)
How to add one: open editor/JxCommandTable.java, add ONE line in the static block:
`MAP.put("command_id", (api, ctx, s, n) -> ...);` (ids are in JxCommands.TABLE). Return true if it worked, false if not. Do not throw. Reusable helpers: s.msRunTextAction(0..4), s.msFocusFeedback(...), s.msPartFeedback(...), s.msStartReading(), s.msChangeSpeechRate(), s.msDimScreen(), s.msSpeakMain(), api.speak/asyncSpeak/getText/insertText.
Groups left (do in this order, one build each):
1. Switch/suspend (needs a design, TalkBack here has no real suspend): suspend touch browsing, speech+touch, hotkeys, edge gestures, main/secondary TTS, accessibility volume, screen reader gestures; lock touch; earpiece mode; toggle multi-finger; toggle TTS engine; enable logging. Idea: one prefs flag per switch (ms_cmd_*) read in the right places (MsOperationController for hotkeys, MsSecondaryTts for secondary TTS, gesture controller for gestures).
2. Recognition: describe focused item / entire screen (+detailed), recognise icon/subtitles/form/CAPTCHA, OCR of whole screen, automatic translation/OCR switches, voice questions about focus/screen, take screenshot of current focus. Reuse existing Gemini / OCR / translate code (ms_gemini, SelectorController).
3. Menus and lists: main, recognition, functions, actions, per-app gestures menu, focus menu, clipboard history, favorites, notification box, chat history, show all nodes, focus monitor (+manager), edit dictionary, edit node alias, add content blacklist, Extensions/Tools/Plugins/Profiles/Timer/Application/Custom voice commands folders. Reuse the TalkBack menu groups (TalkbackMenuProcessor.addMsGroupMenus) and Lua extension list.
4. Modes: character, list, node, enhanced detail, passthrough, lift to activate, tap to move focus, reading, quick browsing, selection, screen grid, virtual screen/navigation, granular browse/edit, auto-next backwards, select navigation type, previous/next element of a type, full-text editing, edit current text, list copying, long press and hold, left/right shortcut, input.
5. Needs a new feature first: current location, timer + pause timer, cloud clipboard copy/paste, read all notifications, automatic chat reading (+history), voice dictation (+continuous), Jieshuo Camera, open URL, gesture scheme description, edit per-app gestures.

## B. Lua scripts the user pasted (must keep working)
Done this session: asyncSpeak, getEditText, insertText, playSoundTick, global `this`.
Still missing: `thread(fn, ...)` and `task(ms, fn)` (Lua state must stay alive while a task is pending; now one fresh state per run and closed after), LuaDialog, SingleLineAdapter, global `dialog`/AlertDialog from Lua with the service context, loadlayout with a service context, getSharedData/setSharedData check, service.click({{">app", this.speak("...")}}) (a Lua call inside the table).
Rule for scripts: Lua needs `then` after `if ... `. The pasted lines "if service.click({...})" have none; add `then`.
Check each script on the phone after the build and report in words which line fails.

## C. Auto-click engine
This repo has editor/JxClickScript + JxClickEnv (uses service.click mini-language). step2_auto_click_engine.zip has a second engine (AutoClickParser/Step/Runner, parser unit-tested with 28 checks). DECIDED (Step 4d): keep the repo's engine; do not merge Step 2's files. Gaps in both: colour search [x1,y1,x2,y2,r,g,b,d], custom # labels, "Actions" command, step-builder UI.

## D. Settings still saved-only (see MS_SETTINGS_NOTES.md)
fast node, wrap warning, auto page scroll, auto window switch, keep type move, auto view type, most Clicking options, edit-cursor volume keys, multi press, back-break auto mode, cloud labels, scenario TTS routing, caller/chat separate TTS.

## E. First build (biggest risk)
Lua engine (NDK/C), everything from zips 1-12 and these steps were never compiled. First GitHub Actions run will likely show many errors: (a) C build for arm64, (b) com.android.cglib / LuaUtil / LuaBitmap imports, (c) androidx vs old support classes, (d) JxCommandsNav: Feedback.focusDirection(...).setHtmlTargetType(...) must be honoured in Actors.java (line ~727). Read the annotations from the workflow job, send them, fix in order.

## F. Naming (later)
New unique app/project name based on "Mobin"; remove TalkBack/Jieshuo names step by step. For now only the zip name changes.


## G. Step queue (2-3 tasks per full-repo zip, hardest first; numbers keep rising)
Rule from the user: zip named Accessible_MobinSR_Full_Repo_StepN_<name>_<date>.zip; N goes 7, 8, 9, 10... This list is updated in every zip.
- Step 7 DONE (unverified): `#name` custom labels. JxClickEnv.findAll(byLabel=true) uses TalkBackLabelManager.getLabelForViewIdFromCache (labels made with the TalkBack "add label" feature). Test: label a button, then service.click({"#mylabel"}).
- Step 8 DONE (unverified), three tasks:
  1. Lua `thread(fn, ...)` and `task(ms, fn, ...)` (also `task(fn, callback)` AndroLua style). Lua state now stays open until the last task ran (LuaServiceApi.scheduleTask/runTask/finishMain, one lock, max 200 pending, max delay 10 min). Timing of the Lua prelude is untested.
  2. Colour search `[x1,y1,x2,y2,r,g,b,d]`: JxClickScript n==8 -> Env.clickColor (default false, so devtools test still compiles) -> JxClickEnv.clickColor takes a screenshot (Android 11+), scans the rectangle, taps the first match. Values below 1 are fractions of the screen.
  3. `command_disable_touch` (Suspend browse by touch): TalkBackService.msToggleTouchBrowse switches touch exploration off/on. Safety choice: it turns itself back on after 2 minutes (MS_TOUCH_SUSPEND_MAX_MS) so a blind user cannot be locked out. Change the constant if he wants it to stay off.
- CORRECTION (Step 9): the Step 8 note saying `ms_tts_engine` is "saved only" was WRONG. It is read in utils/.../FailoverTextToSpeech.java (MS_TTS_ENGINE_KEY, with a change listener), and `ms_async_tts_engine` in MsSecondaryTts. Only the grep missed utils/.
- Step 9 DONE (unverified), three hard tasks:
  1. `command_toggle_tts_engine`: JxCommandTable.nextTtsEngine cycles `ms_tts_engine` through installed engines and speaks the new engine's name.
  2. Custom voice commands: editor/CustomVoiceCommands (file `<base>/Custom voice commands/commands.txt`, lines phrase TAB ext|tool TAB folder), editor/VoiceCommandsActivity (list, New voice command, delete), hook in VoiceCommandProcessor.handleSpeechCommand (final results only, exact phrase after lower-casing and removing punctuation, runs the extension/tool on its own thread). Menu item "Custom voice commands" and execute("Custom voice commands") open it.
  3. Create automatic click function: editor/AutoClickBuilderActivity (menu item "Create automatic click function"). Step types: button name, own label (#), open app (>), command/function (%), tap point, swipe, colour search, raw line; optional app limit (@) and pause ($). Edit/move/delete steps, Test (runs after 3 s behind the screen), Save writes Extensions/<name>/main.lua as `service.click({{ "step", ... }})` between `-- steps begin/end` markers, so it can be reopened for editing. Not covered by the builder: nested branches {{...}} inside a sequence, `<n` repeat and `>n` wait count as separate fields (they can be typed with "Write the line myself"). JxClickScript.checkItem validates each line.
  Still missing for these: a way to open an existing function in the builder (AutoClickBuilderActivity.show(ctx, name) exists but nothing calls it yet; do it with the extension manager in Step 10).
- Step 10 DONE (unverified):
  1. editor/ExtensionManagerActivity (menu item "Manage extensions and tools"): list of all extensions and tools; per item Run, Edit code, Edit steps (only for functions made by the builder, found by the `-- steps begin` marker), Add voice command, Rename (voice commands follow the new folder name), Send (text of main.lua via share sheet), Delete (asks first, also removes its voice commands). The `-- title:` header is not changed on rename.
  2. Commands: directory_tools and tool_title (JxPickerActivity.KIND_TOOLS lists Tools and runs the chosen one), directory_timer (opens the clock app's timers, ACTION_SHOW_TIMERS), input_method (system keyboard picker).
  Still missing from "give a name and a gesture": gestures. Voice-command names are done; assigning a TalkBack gesture to an extension is not (needs a look at GestureController / the gesture preference screen).
  Checked and NOT done on purpose: command_pause_a11y_volume (pref_a11y_volume is the saved volume level, not an on/off switch); switch_multi_finger_gestures (no preference found); command_incall_mode, command_pause_gesture, command_disable_edge_gesture_title, command_false_touch_prevention, command_disable_main_tts need real hooks in core classes.
- COMMAND WORKFLOW: see COMMANDS_WORKFLOW_57_hi.md (groups A to H and their order). Group A is done except chat_history, command_notification_box, profiles_title (they need data or a decision).
- Step 11 DONE (unverified), groups C and G (9 commands):
  editor/MsRecognition: command_ocr_text (OCR of the screen with OcrTool, spoken), command_verification (OCR of the focused item or screen, letters and digits only, spoken one by one and copied), command_take_screenshot2 (focus area saved as PNG in <base>/Screenshots), command_recognition_caption (lower 45% of the screen is OCR'd every 1.5 s, new text spoken; off by the same command or after 30 minutes), voice_picture_description and voice_picture_description2 (you speak a question, Gemini answers about the focus or the whole screen; uses TalkBackService.msGeminiAsk -> Feedback.geminiRequest), command_virtual_table (Gemini lists the form fields).
  editor/MsSpeechInput (SpeechRecognizer helper) and editor/MsDictation: command_voice_input (listens once, types at the cursor with ACTION_SET_TEXT) and voice_input_mode (continuous; stops after 3 quiet rounds or 15 minutes).
  Known limits: Gemini answers only if TalkBack's Gemini setup/opt-in is on; speech recognition needs the microphone permission and the phone's recogniser; command_auto_trans and use_ocr_title were NOT made because no automatic-OCR or automatic-translate hook exists yet (they would only be a saved flag).
- Step 12 DONE (unverified), groups D and E, 8 commands:
  editor/MsNavModes: select_granularity_mode (JxPickerActivity.KIND_NAVTYPE, saves `ms_nav_type`: headings, links, controls, characters, words, lines, paragraphs, and web-only buttons, check boxes, edit fields, graphics, lists, tables, radio buttons, combo boxes, landmarks), to_next_granularity_item and to_previous_granularity_item (use the saved type through JxCommandsNav.target/move, which are now package-private), auto_previous (moves focus backwards every 1.6 s, stops at the start, with the same command, or after 20 minutes), command_long_hold (presses the middle of the focus for 1.5 s with api.press).
  editor/TextViewerActivity: all_text_title (full text of the focus, select and copy), command_long_edit (edit and "Save to the field" with ACTION_SET_TEXT), command_long_copy (visible texts of the focused list, one per line, copy all or selection).
  NOT made in D/E, and why (meaning taken from the original app's Korean/Croatian strings): command_tap_mode ("press to move focus") and command_up_tap ("lift finger to activate") need a touch layer inside TalkBack's touch exploration; command_grid_mode needs a screen-grid overlay; split_menu_title and split_edit_title (granular browsing/editing: text split into pieces), list_mode_title, command_quick_mode, command_supper_mode (detailed focusing) need the user to say exactly how they should behave. These 8 move to group H.
- Step 13 DONE (unverified), 6 commands:
  TalkBackService: msToggleGesturesPaused (command_pause_gesture: handleOnGestureById returns false while paused), msToggleMainTts (command_disable_main_tts: SpeechControllerImpl.setMute; the second TTS is not muted), msToggleFeedbackSuspend (command_disable_feedback: mute + touch exploration off). SAFETY: all three undo themselves after 5 minutes (MS_SUSPEND_MAX_MS), because a blind user could otherwise be locked out.
  editor/MsTouchLock (command_false_touch_prevention): see-through TYPE_ACCESSIBILITY_OVERLAY that takes all touches; same command removes it. Whether TalkBack's own touch exploration still passes through the layer must be tested on a phone.
  editor/MsFocusMonitor + FocusMonitorsActivity (add_node_changed_list, node_changed_list_manager): watches the focused element by app + view id (saved in `<base>/Focus monitors.txt`) or, without an id, by screen position (until restart); when its text changes, the new text is spoken. Hook: TalkBackService.onAccessibilityEvent -> MsFocusMonitor.onEvent. Tap an item in the manager to stop watching.
  Checked and NOT done: command_disable_edge_gesture_title (this app has no edge gestures), switch_multi_finger_gestures (no setting found), command_incall_mode (TalkBack speech uses the accessibility audio stream; the earpiece cannot be chosen from here), command_pause_a11y_volume (the setting is the saved volume level), command_start_log (unclear what is logged). These 5 move to group H.
- Step 14 DONE (unverified): all remaining 30 commands + `%speak(text)`; details and decisions in STEP14_NOTES.md. NEXT: first GitHub build and fixing errors (section E), then phone tests.
- (old Step 14 plan, now done): group F rest (virtual screen, virtual navigation, show all nodes), group G rest (chat messages, notifications), then "Actions" command check (the step-builder UI was done in Step 9); `service.click({{">app", this.speak("...")}})`: Lua runs this.speak(...) while building the table (before the click), so it cannot work as written; use the `%speak(text)` form in the list instead and make `%speak(...)` call service speak.
- (old plan, replaced by COMMANDS_WORKFLOW_57_hi.md) execute() groups 2 to 5 (recognition, menus, modes, new features), one group per step.
- After each step: update this list and REMAINING_COMMANDS.txt, keep JIESHUO_SYMBOLS_STATUS_hi.md in sync.
