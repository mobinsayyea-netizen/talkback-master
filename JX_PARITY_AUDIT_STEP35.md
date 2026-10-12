# Jieshuo command/API list audit — Step 35

This audit records how the long command and method list supplied by the user on 2026-10-11 is to be used. It is a compatibility target, not proof that every entry already works.

## Command groups to map against `JxCommands`
- Screen-reader settings and suspension toggles: logging, browse-by-touch, speech/TTS, hotkeys, touch lock, curtain, edge gestures, earpiece mode, multi-finger gestures.
- Speech/TTS controls: select/switch engine, speech rate, accessibility/media volume, voice assistant/dictation, spell focused item, time/battery/network announcements.
- Focus and node actions: focused-element monitor, focus monitor manager, edit node alias, node properties, show all nodes, focused icon recognition, focus menu, screenshot of focus.
- OCR/translation/recognition: subtitles, automatic OCR, current screen/focus OCR, CAPTCHA, translation, form identification, video description, whole-screen description.
- Media/system: play/pause, next/previous, fast-forward/rewind, home/back/recents, notifications, quick settings, screenshot, screen lock, power menu, split screen.
- Text and clipboard: copy/paste/append, clipboard history, favorites, list copying, clear, full-text selection, selection mode, cloud clipboard.
- Navigation: character/word/sentence/paragraph/heading/link/control traversal, screen areas, scroll, slider adjustment, direct swipes, virtual navigation, reading and automatic browsing modes.
- Calling and messaging: answer/end call, chat history, automatic chat reading/history, continuous dictation.

## Service-method groups to map against `LuaServiceApi`
- Input/navigation state: `setInputMode`, `setSelection`, `setSelectionMode`, `setTouchMode`, `setTouchExplorationMode`, `setPauseGesture`, `setKeepFocusPosition`.
- TTS/audio: `setTtsEngine`, `setTTSSpeaker`, `setTTSSpeed`, `setTtsPitch`, `setTtsScale`, `setTTSVolume`, `setSound`, `setSoundVolume`, `setStreamVolume`, `setUseAccessibilityVolume`.
- Notifications: `setNotificationSpeak`, `setNotificationBlackList`, `setNotificationWhiteList`, `setNotificationSummary`, and notification app/bar/toast options.
- OCR/labels/dictionary: `setUseOCR`, `setUseImageToText`, `setNodeLabel`, `setUnknownLabel`, `setUseCustomDictSpeak`, `setUseCustomDictRegex`, `setUseCustomNodeName`.
- Gestures/hotkeys/sensors: `setUseHotkey`, `setUseEdgeGesture`, `setUseNavigationGesture`, `setUseMultiTouchMode`, `setUseFingerprintGesture`, `setSensor`, `setVibrate`.
- Runtime/data: `setProfiles`, `setSharedData`, `setLuaExtDir`, `setTimerMode`, `task`, `thread`, `ticker`, `timer`, `tool`, `speak`, `startActivity`, `startService`, `showVirtualScreen`.

## Implementation rule
Do not add a method that claims success unless it actually changes the corresponding TalkBack state or performs the requested action. Where no corresponding feature exists, a correctly typed compatibility method may fail safely, but the exact argument and return signatures must be verified first. A name-only list cannot safely determine Java overload types.

## Step 35 change related to this list
The async lifecycle fix ensures `task`, `thread`, and timer callbacks do not lose their Lua state immediately after the entry script returns. Static checks pass; Android build and runtime behavior are not verified.
