# Step 47 — Sound themes were never connected to the service (2026-10-18)

**हिंदी सार:** बैकअप से आई आवाज़ें इसलिए नहीं बदलती थीं क्योंकि सर्विस शुरू होते समय साउंड थीम जोड़ी ही नहीं जाती थी। `TalkBackService.initializeInfrastructure()` में सिर्फ एक कमेंट था ("sound themes ... were removed") और `FeedbackController.setSoundOverrideProvider(...)` को कहीं से बुलाया ही नहीं जाता था, इसलिए हर आवाज़ डिफॉल्ट बजती थी। थीम का चुनना, Jieshuo backup से restore और फाइलों का मैपिंग सब सही था, पर चलता नहीं था।

## Cause (found by reading the code)
- `FeedbackController.playAuditory()` asks `soundOverrideProvider.getOverridePath(resId)` first. The provider was `null`: nothing in the project called `setSoundOverrideProvider`, and `new SoundThemeManager(...)` was created only inside the settings screens.
- `TalkBackService` still had `extraEventSounds` declared and used (stop, onAccessibilityEvent, pause/resume) but it was never created, so it stayed `null`.

## Changes
1. `TalkBackService.initializeInfrastructure()`: create `SoundThemeManager`, `feedbackController.setSoundOverrideProvider(...)`, create and `start()` `ExtraEventSounds` (the existing hooks call it).
2. `FeedbackController.playAuditoryFromPath()`: sound cache key is now path + last-modified + size, so a restored/replaced file is not played from the old cache. The file is still loaded from the real path.

## NOT verified
- Not compiled, not run on a phone (no Android SDK here). Run the GitHub Actions build.

## Phone test
1. Restore the Jieshuo backup again (Advanced > Backup and restore > Restore from Jieshuo backup) or pick a sound theme.
2. Move focus over a button, a text, a link; scroll. The theme's sounds should play, not the default ones.
3. Toast, dialog and keyboard show/hide should play the theme's extra sounds when the theme assigns one.
4. If still default: Sound theme settings -> check the active theme and that the slot has a file.
