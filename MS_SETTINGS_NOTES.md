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
