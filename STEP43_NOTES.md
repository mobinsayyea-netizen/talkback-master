# Step 43 - "App options" group in the actions menu (2026-10-12)

**हिंदी सार:** किसी भी ऐप पर फोकस रखकर एक्शन्स मेन्यू खोलें तो नया ग्रुप "App options" दिखता है: इस ऐप की जेस्चर स्कीम, नोटिफिकेशन पढ़ो/न पढ़ो, नोटिफिकेशन व्हाइटलिस्ट, ब्लैकलिस्ट, इस ऐप की स्पीच रेट, इस ऐप का वॉल्यूम, पैकेज नाम कॉपी। कुछ भी कंपाइल या फ़ोन पर परखा नहीं गया।

New: `editor/MsAppProfiles.java` (settings per app in pref `ms_app_profiles`). Edited: `TalkbackMenuProcessor` (group), `ContextMenuItemClickProcessor` (click), `TalkBackService.onAccessibilityEvent` (applies rate/volume when the app in front changes, restores the common values after), `MsNotificationController.handle` (blacklist / whitelist / read-off check), `res/values/ms_reading3_strings.xml` (ids + titles).
- In a launcher the focused item belongs to the launcher; the app is found by matching the item's text with installed app names.
- Rate and volume items cycle through presets (rate 50..300 %, volume 20..100 %) and then go back to the common value.
- Whitelist: when any app is whitelisted, only whitelisted apps (and apps set to "read") are spoken. Blacklist wins over everything.
- Existing `ms_rd_filter_blacklist` / `ms_rd_auto_blacklist` are TEXT-word filters, not app lists; they are untouched.
## Not done yet
Per-app voice, verbosity, mute-sounds, screen-change auto read, macro, "put app on a gesture"; the stream drop-downs (main / secondary / notification) for rate and volume reading controls.
