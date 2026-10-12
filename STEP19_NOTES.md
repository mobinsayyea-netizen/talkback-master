# Step 19 - Operation settings and Advanced settings, same structure as Jieshuo (2026-10-10)

**हिंदी सार:** आपकी मुख्य सेटिंग्स में "Operation settings" और "Advanced settings" थीं ही नहीं। उनकी पुरानी स्क्रीन (`MsOperationFragment`, `MsAdvancedFragment`) बनी रह गई थी, पर कहीं से खुलती नहीं थी, और उनकी चीज़ें "Shortcut Settings" और "Date & Time Settings" में बिखर गई थीं। अब दोनों स्क्रीन मुख्य सेटिंग्स में वापस हैं और उनका ढाँचा, नाम और क्रम Jieshuo की असली `action_setting.xml` और `advanced_setting.xml` से अपने आप बने हैं (ट्रांसक्रिप्ट की क्लास 62, 57, 41, 72 वगैरह से मेल खाते हैं)। कुछ भी कंपाइल या फ़ोन पर परखा नहीं गया।

## What was wrong
1. `preferences.xml` had only 9 MS groups; no "Operation settings", no "Advanced settings". `MsOperationFragment` and `MsAdvancedFragment` were not opened from anywhere.
2. The old Operation screen had 5 groups of 38 items, the real one has 13 entries and about 190 items. Missing: Gestures default settings (+ Shortcut gestures, Edge gestures, Multi-part gestures with 16 sub-screens), Gesture scheme items, Gaming mode, Hotkey scheme, Fingerprint gestures, many Navigation / Clicking / Shortcut keys items. "More key actions" was our own group that does not exist in Jieshuo (its double/triple press items belong inside Shortcut keys).
3. The old Advanced screen was only time announcements + battery. The real one has 15 entries: Voice assistant and translation, OCR, Timer, Operation (14 items), Typing, Menu customization, Article reading mode, Autostart extensions, Scheduled tasks, Profiles, Resource manager, Backup and restore, Language, Power management, Other.
4. The same `ms_op_*` items were repeated in "Shortcut Settings" (group 5) with different titles. That is harmless (same key = same value) but confusing; left as is.

## What is new
- Main settings: "Operation settings" (before Reading & Verbosity) and "Advanced settings" (before Other Settings), same relative order as Jieshuo's main menu.
- 40 screens `res/xml/ms_jx_*.xml` + 40 tiny fragments `MsJx*Fragment` + `MsJxBaseFragment` + `values/ms_jx_arrays.xml`. All made by `devtools/step19_gen/gen_settings.py` from Jieshuo's own xml (run it again with the cSr `res` folder and the repo root; it is safe to re-run). All 40 screens are also in the settings search.
- Items that already worked keep their OLD keys (`ms_op_*`, `ms_adv_*`, `ms_ext_autostart`, TalkBack's speech-overlay key), so nothing already wired breaks: volume keys, long/double/triple press, headset, shake (+sensitivity, answer call), wrap navigation, single tap, lift to activate, hold to long press (+duration), timer / time announcements.
- Hooked to screens we already have: Gesture schemes settings -> Extension gestures; Gesture scheme manager and Per-application gesture scheme -> Per-application gesture schemes (Step 18); Resource manager; Backup (Local backup/restore -> backup screen); Export all -> Resource manager; Customize menus; Cursor highlighting -> Focus indicator; Autostart extensions -> our switch; Display speech output -> TalkBack's switch; "Download Jieshuo Beta" replaced by "Check for updates". Voice assistant and translation opens Gemini key, Gemini voice command, Translate and OCR. Timer settings = our time announcement screen (What to speak / When to speak).
- Kept as extras at the end of a screen: Battery optimization (Power management), Check permissions (Advanced > Other).

## What does NOT work yet (be honest in testing)
- About 195 of 302 items are only SAVED: switches say "Saved. Not active yet." in their summary; lists, multi-select lists and text boxes (OCR, multi-part gestures, edge gestures, fingerprint, hotkey scheme, gaming mode, typing, menu options, article keywords...) do the same but their summary shows the value, so they have no such note. There is no engine behind them. Wiring them one by one is the next work.
- 18 items are shown dimmed with "Not available yet" (Gesture scheme description, Scheduled tasks, Profiles, Language settings, cloud backup / restore, floating menu list, Application volume shortcuts, Baidu help link...).
- Lists whose real entries Jieshuo fills from code have a short guess: Default navigation gestures style, Gesture scheme and Hotkey scheme show only "Default"; OCR engine has 4 names; Subtitle recognition modes show only "Ask every time"; navigation gesture trigger range is 1 to 4. Fix when we wire them.
- Jieshuo uses check boxes, we use switches (same on/off meaning, same TalkBack look as the rest of the app).
- `ms_op_headset_action` (what the headset button does) is not in Jieshuo's list, so it is not on the new Operation screen; it is still in Shortcut Settings.
- The old `MsOperationFragment` / `MsAdvancedFragment` and their xml are now unused; left in place.

## Tested
All 40 xml files + arrays + preferences.xml parse; every referenced activity, fragment and array exists; no duplicate key inside a screen; the 42 new/changed Java files pass a syntax parse. Nothing was compiled.

## Phone test after build
Settings > Operation settings: 13 entries in this order: Gestures default settings, Gesture scheme, Gesture schemes settings, Gesture scheme description (dimmed), Gesture scheme manager, Per-application gesture scheme, Gaming mode settings, Navigation settings, Clicking settings, Shortcut keys settings, Hotkey scheme settings, Fingerprint reader gestures, Other settings. Open Shortcut keys settings, change "Long press of the volume up key" and test it. Settings > Advanced settings: 15 entries (Voice assistant ... Other settings), Timer settings opens the time announcements, Operation settings has 14 items. Search for "multi-part".

---
## Step 20 (2026-10-10): Typing and OCR connected to the engines TalkBack already has
Not compiled, not tested on a phone.
- **Typing settings** (`MsJxAdvTypingSettingsFragment`): 5 switches now change TalkBack's own settings and show its current values when the screen opens: Echo typed characters + Echo typed words -> keyboard echo (on-screen and physical: both / characters / words / none); Read capital letters -> capital letters (say "capital" / nothing); Speak example words for letters -> phonetic letters; Double tap to type -> typing confirmation (on = double tap, off = lift to type). The "Saved. Not active yet." note is removed from these 5.
- Still save-only in Typing: gestures for candidates, suspend touch/gestures while keyboard shown, speak keyboard elements, read candidates, speak passwords (Android controls it; needs a special permission).
- **OCR** (`OcrTool`): "OCR language" now works: Automatic (Hindi, Marathi, English, then English fallback as before) or English only. "OCR engine" shows only Offline OCR (ML Kit), the one that exists. Old guessed Chinese/Japanese/... languages removed (no such model in the app).
- Still save-only in OCR: auto-speak, auto image description, accuracy, subtitle modes, Baidu.
- Note: `gen_settings.py` was not changed, so re-running it would undo the OCR arrays and the 5 summaries.

---
## Step 21 (2026-10-10): separate menus, like Jieshuo. Not compiled, not tested on a phone.
New files (editor/): `MsMenus` (all logic and default lists), `MsMenuActivity` (menu window), `MsMenuItemsActivity` (customize screen), `MsFloatingMenu` (strip under the status bar).
- **Main menu** command now opens its own list (your list + "More"). Default list: Granular browsing mode, List browsing, Read the whole screen, Voice assistant, Screen reader settings, Extensions, Suspend speech feedback and browse by touch. "More" opens the **multi-column menu** (3 columns, reads "Row r, column c: name", default 28 entries, ends with "More"), and "More" there opens **all functions** (the whole command table). With "Use the multi-column menu" on, Main menu opens the multi-column menu directly.
- **Functions menu** command opens its own window: Clear, Select, Copy, Append, Paste, Clipboard history, Favorites, Granular editing mode, List copying, Open keyboard, Recognition menu. With "Show actions..." on, the focused item's own named actions are added (Uninstall, Open chat, ...); with "...dynamic focus related options" on, Click, Long click, Expand, Collapse, Dismiss (if the item has them).
- **Recognition menu** command opens its own window with the 14 recognition commands.
- The same 8 missing entries (Clear, Select, Paste, Clipboard history, Favorites, Granular editing mode, List copying, Open keyboard) were also added to the Functions group of TalkBack's grouped menu.
- **Clear fixed**: it now empties the focused text box (it used to clear the clipboard).
- **Customize** (main menu, multi-column menu, floating menu): Add (functions / extensions and tools / applications), Default, and per entry Move up, Move down, Delete.
- **Floating menu**: "Use the floating menu" shows a strip under the status bar (default: one button "Main menu"). It is an accessibility overlay, no extra permission.
- "Current per-app gestures menu" and "Focus menu" still open TalkBack's grouped menu.
- Open questions: "Select" runs "Full text selection" (check it matches Jieshuo's Select); "Open keyboard" taps the focused text box; the audio does not list the Recognition menu fully, so its entries are the existing recognition commands.

---
## Step 22 (2026-10-10): Reading settings checked against the audio (classes 35-45) and the GitHub repo
The GitHub repo (last commit Oct 6) has the same reading files as the zip, and does not have Steps 19-21.
- The 10 Jieshuo-layout reading screens (`MsReadingFragment`) were not opened from anywhere. Added a link "Reading settings (Jieshuo layout)" in Reading & Verbosity, under "Verbosity preset and details". The flat screen keeps the order you made. Both use the same keys, so they stay in sync.
- Added 6 missing items, in both the flat screen and the sub-screens (save only, "Saved. Not active yet."): read relationship between elements in node browsing mode; read web page control type (default on); read the index when the control has no label; read input hints for text boxes; automatically read changes of the current window; filter images on web pages.
- Not added: chat-messages TTS settings and an app picker for chat apps (needs a new screen); "Use cloud labels" (needs cloud); "Strict web content filter" (Chinese only). (Step 23 below fixes the list items.)
- Typing "Read capital letters" and Character explanation "Read capital letters" change the same TalkBack setting.

---
## Step 23 (2026-10-10): Reading settings compared with Jieshuo's own `content_setting.xml` (from cSr.zip)
Jieshuo's Reading settings have 10 screens: Dynamic alert 5, List 6, Label 6, Dynamic content 4, Chat 4, Screen state 5, Caller 5, Character explanation 4, Content filtering 4, Custom label/alias/dictionary 7. Titles and defaults of everything added in Step 22 match it.
- Added (save only): List: Allow reading of element indexes and display range in lists; Read the index of the currently focused list item while scrolling; Read all visible content of the list when scrolling using the commands; Keep current list position while scrolling (list now 6 with the 2 TalkBack-backed ones). Label: Read the state of the focused item first (label now 6; Step 22 wrongly said 5).
- Still missing: "Chat messages auto-reading TTS settings" and "Caller info TTS settings" (a separate TTS screen per feature), chat app picker with checkboxes, "Use cloud labels", "Strict web content filter" (non-Chinese content; Chinese only).

---
## Step 24 (2026-10-10): four Reading settings made to work (new `MsReadingExtras`). Not compiled, not tested.
- **Allow reading of element indexes and display range in lists**: off = TalkBack stops saying list positions (hooked into `GlobalVariables.getSpeakCollectionInfo`).
- **Read the state of the focused item first**: on = "checked, name, type" order (hooked into `GlobalVariables.getDescriptionOrder`).
- **Automatically read changes in the current window**: on = text that changes in a part of the window that is not focused is spoken (focused item and live regions are already read by TalkBack). Same text is not repeated within 1.5 s.
- **Read all visible content of the list when scrolling using the commands**: after Scroll back/forward/up/down worked, the visible list items are read (default on, like Jieshuo).
- Still saved only (they need changes inside TalkBack's speech rules, not just a switch): Read web page control type, Read the index when the control has no label, Read the input hints for text boxes, Filter images on web pages, Read relationship between elements in the node browsing mode, Read the index of the focused list item while scrolling, Keep current list position while scrolling; also the chat and caller TTS settings.

---
## Step 25 (2026-10-10): Reading settings cleaned up
Count of the 48 Jieshuo reading items: 10 are TalkBack's own settings, 31 are MS Screen Reader features that work, 7 were saved only.
- Removed the 7 items that did nothing (they are listed under Step 24 "still saved only"; add them back when their engine exists) and the Step 22 link to the 10 Jieshuo-layout screens, so nothing shows twice. The flat Reading & Verbosity screen is again your arrangement, with only options that work, plus the 4 added in Step 24.
- The 10 sub-screens stay in the code (not opened from anywhere), also without the dead items.
