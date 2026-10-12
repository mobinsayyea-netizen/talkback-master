# Step 14 - the last 30 commands (all decisions taken by the assistant)

**हिंदी सार:** बाकी सभी 30 कमांड अब कोड में हैं (173 में से 173)। जहाँ आपका फ़ैसला चाहिए था वहाँ मैंने फ़ैसला खुद लिया, नीचे हर एक का लिखा है। कुछ भी कंपाइल या फ़ोन पर परखा नहीं गया है।

New files: `editor/MsExtras.java` (all 30 handlers + event hook + gesture blocking), `editor/MsListActivity.java` (one list screen used by many commands). Edited: `JxCommandTable` (one line: `MsExtras.register(MAP)`), `TalkBackService` (two hooks: `MsExtras.onEvent`, `MsExtras.gestureBlocked` in handleOnGestureById), `JxClickScript.runCommand` (`%speak(text)`), `AndroidManifest.xml` (MsListActivity, location permissions).

## What each command does now
| Command | Behaviour (decision) |
|---|---|
| command_notification_box | list of stored notifications (newest first, file `Notifications.txt`, max 200); tap = copy; top rows Copy all / Delete all |
| chat_history | same screen for chat messages (`Chat history.txt`) |
| command_speak_all_notification | speaks the last 20 stored notifications |
| command_auto_list_speak | switch: in chat apps (WhatsApp, Telegram, Messenger, Signal, SMS, ...) a new last message in the list is spoken; first look only remembers |
| command_auto_list_speak_history | switch: save chat messages to history (default on) |
| command_all_node | list of ALL nodes of the screen with class, id, text, bounds; tap = copy |
| command_virtual_screen | list of visible texts/buttons in reading order; tap = click it |
| command_virtual_navigation | same list; tap = move focus there |
| command_grid_mode | list as "Row r, column c" (3 columns x 4 rows); tap = move focus (a list, not a drawn grid) |
| list_mode_title | items of the list around the focus; tap = move focus |
| split_menu_title | focused text cut into sentences; tap = copy piece |
| split_edit_title | same as "edit current text" |
| command_start_log | switch: writes events to `Logs/log.txt` (rolls at 1 MB) |
| command_disable_edge_gesture_title | switch: suspends the corner (L-shaped) swipes. This app has no real edge gestures, so this is the closest meaning |
| switch_multi_finger_gestures | switch: suspends all 2/3/4-finger gestures (found by reflection over AccessibilityService.GESTURE_*) |
| command_pause_a11y_volume | flips `ms_async_tts_accessibility_volume` (the existing switch of the settings screen) |
| command_incall_mode | best effort: audio mode in-communication, speaker off. TalkBack speech uses the accessibility stream, so it may do nothing on some phones |
| command_auto_trans | real: after each focus the translation is spoken too (uses the existing Translate settings) |
| use_ocr_title | real: focus on an element with no text and no description -> OCR of its area is spoken (Android 11+, one per 2.5 s) |
| command_supper_mode | switch: after each focus, extra details are spoken (type, clickable, checked, item n of m, position) |
| command_up_tap | switch: when the finger lifts after touch exploration, the focused item is clicked (event TOUCH_EXPLORATION_GESTURE_END) |
| command_tap_mode | turns lift-to-activate off (TalkBack's normal tap-to-focus, double-tap-to-activate) |
| command_quick_mode | switch: speech rate up two steps / down two steps |
| command_left_shortcut / right | runs the function saved in `ms_left_shortcut` / `ms_right_shortcut` (any Jieshuo function name); if none, previous / next element of the chosen navigation type. No settings screen to pick it yet |
| profiles_title | list of folders in `<base>/Profiles` that contain main.lua; tap runs it |
| camera_title | opens the phone's own camera (no Jieshuo-style camera screen) |
| command_current_location | last known location -> address by Geocoder (else latitude/longitude). Needs location permission; if missing it opens App info |
| command_copy_to_cloud / paste | no server: text goes to `<base>/Cloud clipboard.txt` (it becomes a cloud clipboard if that folder is synced by Drive/Syncthing); paste puts the file text on the clipboard and pastes it into the focused field |
| `%speak(text)` in auto-click | speaks the text and goes on to the next step |

"Actions" command: `actions` was already mapped in Step 5 (TalkBack actions menu).

## Still not done / honest limits
- Nothing from Steps 1-14 has been compiled. FIRST: run the GitHub build, send the annotations (see NEXT_STEPS_ACCESSIBLE.md section E).
- Phone tests needed: lift to activate (timing 140 ms), auto chat reading (list detection differs per app), auto OCR/translate speech overlap, notification capture (needs TalkBack to receive notification events), earpiece mode.
- No screen to choose the left/right shortcut function; no real drawn screen grid; no real cloud server; no Jieshuo camera screen.
- Saved-only settings in MS_SETTINGS_NOTES.md section D are unchanged.
- Quota note: if the free quota ends, start the next chat with "NEXT_STEPS_ACCESSIBLE.md, STEP14_NOTES.md, HANDOFF.md padho, build errors thik karo".
