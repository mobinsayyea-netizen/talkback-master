# MS Screen Reader - settings port from Jieshuo (CSR) - notes

## Zip 1 (this zip): TTS settings + Secondary TTS settings
Settings > "TTS engine and voice": engine, system TTS button, speed, pitch, volume, audio focus, proximity.
Settings > "Secondary TTS settings": engine, speed, pitch, volume, audio focus, proximity, accessibility volume, touch stop.
- Main engine choice (key ms_tts_engine) is applied in FailoverTextToSpeech.updateDefaultEngine.
- Secondary values are SAVED (keys ms_async_tts_*) but not yet USED by speech: needs a second TTS instance (next step).
- Existing keys reused for main: pref_rate_volume, pref_pitch_volume, pref_speech_volume, pref_use_audio_focus, pref_proximity.
- Not yet done from Jieshuo main TTS: single-TTS switch, rate acceleration, accessibility volume, touch stop, keep-awake (main).

## Zip 2: Operation settings (this zip includes zip 1)
Settings > "Operation settings": Navigation, Clicking, Shortcut keys (volume keys), Other (shake).
- Keys are ms_op_* . Code: talkback/MsOperationController.java, hooked in TalkBackService (onServiceConnected, onDestroy, onKeyEventInternal) and FocusProcessorForLogicalNavigation (wrap).
- WORKING (wired): Use volume keys (master), long press volume up/down, both keys together, locked-screen long press, short press adjusts volume (with optional system volume panel), Shake action + sensitivity, Wrap navigation on/off.
  Actions available: do nothing, stop speech, voice assistant, quick settings, home, back, notifications, recents.
- STILL SAVED ONLY (not wired): fast node, wrap warning, auto page scroll, auto window switch, keep type move, auto view type, all Clicking settings, edit-cursor volume keys, headset button, multi press, shake-answer-call, back-break auto mode.
- Left out for now (big, needs TalkBack gesture engine work): multi-part gestures, edge gestures, fingerprint gestures, per-app gesture scheme, gaming mode, hotkey schemes.

## Zip 4 (all-in-one, includes zips 1-3)
- Secondary TTS now WORKS: MsSecondaryTts (own TextToSpeech instance: engine, speed, pitch, volume, audio focus, proximity stop, accessibility volume, touch stop). Used for notifications.
- Notification settings WORK (MsNotificationController): auto read (off/locked/unlocked/both), toast, no read while touching, no read in calls, summary only, read source, secondary voice, queue.
- MS advanced settings: automatic time announcement (MsTimerController): interval, active hours.
- Not done: scenario TTS profiles, content filtering, clicking settings wiring, translate/OCR/typing (TalkBack already has its own), Operation items listed in zip 2 notes.

## Zip 5 (all-in-one, includes zips 1-4)
- MS advanced settings: choose what to speak (time, date, year, battery), speak after unlock (ACTION_USER_PRESENT), interval timer, voice (main/secondary), battery-optimization button.
  NOTE: TalkBack's own "tell time when screen wakes" is separate; turn it off if you hear the time twice.
- Operation: headset button action, double/triple press of volume keys, shake answers a ringing call (needs Phone permission granted in app info; ANSWER_PHONE_CALLS added to manifest).
- Clicking (MsClickController): single tap to activate, lift to activate, hold to long press + duration. Built on touch events: NEEDS TESTING (double tap may click twice).
- Scenario voices: chat messages (chat apps list, read on/off, main/secondary voice).
- NOT done (reasons): content filtering and caller announcement (deep compositor/phone-state work), routing whole TalkBack speech scenarios (reading mode, browsing) to the secondary voice, big gestures (multi-part, edge, fingerprint, per-app scheme), Voice assistant (last).

## Next zips (planned order, from user's structure list)
2 Notification settings (auto read, toast, summary only, source, no read during calls)
3 Advanced settings (voice assistant, translate, OCR, timer, typing)
4 Scenario TTS profiles, content filtering, reading settings, power management
Each next zip must contain all earlier files too.
Reference: Jieshuo res/xml/main_tts_setting.xml, async_tts_setting.xml, notification_setting.xml, advanced_setting.xml.

## Zip 6 (all-in-one, includes zips 1-5): Reading settings, part 1
Settings > "Reading settings" > four sub-screens (Jieshuo content_setting order).
- Dynamic alert reading: usage hints (TalkBack key pref_a11y_hints), key echo on-screen and physical (TalkBack keys), read volume when changing (NEW, MsReadingController: VOLUME_CHANGED_ACTION broadcast, speaks percent), read whole window on window change (NEW, reads visible text 0.7 s after a window opens).
- List reading: element positions (pref_speak_container_element_positions), list range while scrolling (pref_verbose_scroll_announcement).
- Label reading: element type (pref_speak_roles), unlabelled controls (pref_speak_element_ids).
- Dynamic content: progress updates (pref_allow_frequent_content_change_announcement), changes in the focused item (NEW, ms_rd_focus_content_changed, live regions always spoken), window name (NEW, ms_rd_window_title).
- NEW switches are applied in compositor/EventFilter.sendEvent via MsReadingController.shouldDropEvent.
- NOT done (no TalkBack equivalent, needs own engine): node relationship in browsing mode, real-time list index, read all visible list content on scroll, keep list position, web control type, input hints of text boxes, state first, changes in the whole window.
- UNVERIFIED: not compiled or run here. Volume reading uses an undocumented system broadcast; may not fire on some phones.

## Zip 7 (all-in-one, includes zips 1-6): Reading settings, part 2
Settings > "Reading settings" now has 7 sub-screens. New in this zip:
- Screen state reading (MsReadingController): speak "Unlocked" on unlock, speak "Screen locked" when the screen turns off, time reading format (system / 12-hour / 24-hour, used by MsTimerController), always read battery after unlock, unread notification count after unlock.
  NOTE: the count is "new notifications that arrived while the screen was off" (counted from events), not the phone's real unread list (that needs Notification access). Order of the time announcement (Advanced) and these phrases after unlock is not fixed.
- Character explanation: example words for letters (TalkBack key pref_phonetic_letters), read capital letters (TalkBack key pref_capital_letters).
- Custom label and dictionary: Custom label manager (opens TalkBack's own label manager), user dictionary on/off, regular expressions on/off, dictionary text (one "word=spoken text" per line). Applied in Compositor (all normal speech) and msSpeakMain (announcements). Plain mode ignores letter case.
- NOT done: read the text first when explaining, index of a latin letter, custom node aliases, cloud labels (needs a server). Dictionary does not yet cover the secondary voice or text-typing echo.
- UNVERIFIED: not compiled or run here.

## Zip 8 (all-in-one, includes zips 1-7): Reading settings, part 3
Settings > "Reading settings" now has 10 sub-screens. New in this zip:
- Caller announcement (MsCallController): reads "Incoming call from <name or number>" (name from Contacts, else digits one by one), repeat 1/2/3/5 times every 4 s while ringing, optional ringtone-volume voice (own TTS with ringtone audio usage), speaks call length when a call ends (measured from answer to hang-up, no call-log needed). A row opens App info so you can allow Phone, Call logs and Contacts. Without Call logs permission Android hides the number, so nothing extra is said (TalkBack still reads the call screen).
- Content filtering (MsContentFilter, applied in Compositor before the dictionary and in notification speech): blacklist (plain line = exact whole text, "contains:word", "re:regex"), and "filter unwanted controls" = skip text made only of symbols (single characters are kept so typing/character navigation still work).
- Chat auto-reading: switch, chat apps list, voice (same keys as Scenario voices screen), plus NEW blacklist: a chat notification containing any line (case-insensitive) is not read.
- NOT done: image filter on web pages and strict web filter (Jieshuo's is aimed at Chinese text, no TalkBack equivalent), separate TTS settings for caller/chat voices, reading the open chat window (only notifications are read), cloud labels (needs a server).
- UNVERIFIED: not compiled or run here.

## Zip 9 (all-in-one, includes zips 1-8): leftovers from zip 7
- Character explanation screen: "Read the text first when explaining" (says the letter, then its example word, e.g. "a, apple"; needs example words on) and "Read the index of a latin letter" (adds "number 1" for a, up to 26 for z; works even with example words off). Code: ProcessorPhoneticLetters.speakPhoneticLetterForTraversedText. Only for character navigation in text, not the on-screen keyboard.
- Custom label and dictionary screen: "Use custom node aliases" + "Aliases manager" (MsNodeAlias). One line per alias: view id or exact element text = spoken name, e.g. com.whatsapp:id/send=Send message. Hooked in AccessibilityNodeFeedbackUtils.getNodeTextOrLabelDescription, so the alias replaces the element's name when TalkBack builds it (role and state words are still added). Matching is whole text, ignoring case.
- Still not done: cloud labels (needs a server).
- UNVERIFIED: not compiled or run here.

## Zip 10 (all-in-one, includes zips 1-9): grouped main menu (step 1)
The normal TalkBack menu (same gesture as before) now shows three groups, like Jieshuo's main menu. Code: TalkbackMenuProcessor.addMsGroupMenus (items are moved into groups at the end of prepareMenu, so TalkBack's own item logic is untouched).
- Recognition menu: Translation, Text recognition (OCR), Text recognition and translation, Describe image, Screen overview (if available), Video description start/stop.
- Navigation menu: Navigation settings (granularity), Read from top, Read from current, Find on screen.
- Functions menu: Copy, Append, copy/spell/repeat last spoken phrase, Text formatting, Languages, Dim/brighten screen, Tell time on/off, System actions, Audio ducking, Sound feedback, Vibration feedback.
- Left at the top level: typo suggestions, custom actions, page navigation, labels, verbosity, settings, voice commands, keyboard shortcuts, Lua create items.
- New: Translation / OCR / Copy / Append menu items run the same code as the reading controls (SelectorController.msRunTextAction) on the focused item.
- Not yet: Clipboard history and Favorites (next step), moving/hiding items by the user (step 3), per-app menu.
- UNVERIFIED: not compiled or run here. Check that opening a group and going back works, and that Describe image (which has its own sub-list) still opens inside Recognition.

## Zip 10b (merged into zip 11): Voice assistant -> TalkBack voice commands
- The "Voice assistant" action (default on long press volume down; also usable for volume up, both keys, shake, headset button) now starts TalkBack's OWN voice commands (VoiceCommandActor / VoiceCommandProcessor), the same listener as the "Voice commands" gesture. Before, it only opened the phone's system assistant.
- Code: TalkBackService.msStartVoiceCommands() (sends Feedback.voiceRecognition START_LISTENING_IF_SCREEN_NOT_LOCKED); MsOperationController.perform("assistant").
- New list item "Phone assistant (system)" (value system_assistant) keeps the old behaviour.
- Works with TalkBack's command set only (open apps list, home, back, notifications, copy/paste, find, etc.). Jieshuo-style "Voice assistant capabilities", continuous dictation and custom commands are NOT built yet.
- First use shows TalkBack's voice-commands intro dialog and asks for Microphone permission.
- UNVERIFIED: not compiled or run here.
- Note: earlier notes (zips 1-9) are partly stale. Clipboard, Updater, Settings backup, Scenario screen and sound themes exist in the code now.


## Zip 11 (all-in-one, merges zip 10 menu groups + zip 10b voice assistant): full backup/restore + new default sounds
- Settings > About & Updates > "Backup and restore" (SettingsBackupActivity, engine = backup/MsBackupManager).
- ONE zip (ms-screen-reader-backup.zip) holds: every SharedPreferences file (main settings, ms_gemini = Gemini key + model, ms_translate, sound_theme_prefs, lua_ide, anything else; device-only files ms_backup, ms_updater and library files are skipped), files/sound_themes (themes, assignments, sound pool), ms_clipboard_history.json, files/lua, TalkBack/Extensions and TalkBack/Tools (Lua), custom labels (labels table exported as JSON, re-inserted on restore).
- Folder backup: "Choose backup folder" uses the system folder picker, so a Google Drive folder works (needs the Google Drive app installed). No Google sign-in or API setup. Folder permission is kept across restarts. "Back up now", "Restore from the folder", and a switch for automatic backup (service checks every 15 min, backs up if last backup is older than 6 h; runs only while the screen reader is on).
- Single file backup/restore still exists (zip file). Old v1 json backup files are NOT readable by the new restore.
- After a reinstall the folder must be picked again (Android forgets folder permission), then press Restore.
- Restore replaces sound themes, labels and Lua folders as a whole, writes settings over existing keys. Turn the screen reader off and on once afterwards.
- Default sounds: all 32 built-in sounds (res/raw, utils res/raw) replaced by new soft synthesized tones (same file names). The original Google TalkBack sound files are not in this project, so they could not be restored. Braille raw sounds untouched.
- UNVERIFIED: not compiled or run here. Drive folder write depends on the Drive app's documents provider.

## Zip 12 (all-in-one, includes zip 11): Lua extensions connected to the menu and to events
- New editor/LuaExtensionManager. Every folder Extensions/NAME/main.lua is an extension. Header lines in the first 20 lines of main.lua: `-- title: Name`, `-- menu: off` (hide from menu), `-- event: app_opened, notification` (comma separated).
- Menu: new "Extensions" group in the TalkBack menu (after Functions) with one item per extension; picking one runs it on a worker thread; Lua global `node` = item with screen reader focus. Code: TalkbackMenuProcessor.addExtensionsMenu, ContextMenuItemClickProcessor (menu ids 0x4D530000+n).
- Events: window_changed, app_opened (package changed), notification, service_started. Hooked in TalkBackService.onAccessibilityEvent / onServiceConnected. Lua globals: event_type, event_package, event_text. Same extension+event runs at most once per 1.5 s. Runs on one background thread, one after the other. Errors are spoken.
- New extensions created in the editor start with a header template.
- Not done: screen_on/off, key and gesture events, long-press Delete/Edit on an extension, many service.execute() names.
- UNVERIFIED: not compiled or run here. Lua engine itself (NDK) was never built yet.
