# MS Screen Reader — handoff notes (read this first in a new chat)

**हिंदी सार (for Mobeen):** यह फ़ाइल नई चैट के लिए है। इसमें बताया है कि MS Screen Reader क्या है, अब तक क्या बना, आपने क्या-क्या तय किया, और आगे क्या करना है (क्रम से)। नई चैट में बस लिखिए: "MS Screen Reader का काम आगे बढ़ाओ, रेपो talkback-master की HANDOFF.md पढ़ो"।

---

## 1. The user and how to work with him

- **Mobeen Sayyed** (मोबीन सैयद). "MS" in "MS Screen Reader" = his initials.
- Blind, uses a screen reader daily. Works **only from his phone** (Moto G45, Android; earlier a Samsung J7 Nxt). No computer.
- Speaks/writes **Hindi**, often by voice typing, so words come out garbled (see glossary at the end). Reply in Hindi, **short**, phone-screen sized. Lead with the answer.
- He decides in steps: **discuss first, build only when he says "बना"**. Do not start building on your own. Ask one question at a time.
- He gets frustrated by long silent waits and repeated status text. Give a short answer, then work. Very long chats became slow — **keep this handoff and start fresh chats**.
- He does not understand internal numbers/names (e.g. `focus0..focus4`) — decide technical things yourself and tell him simply.
- He builds APKs with his own "Accessible GitHub APK Builder" HTML tool, and via GitHub Actions. He installs from a **direct release link**.
- GitHub user: `mobinsayyea-netizen`. All his repos are public.
- **Security:** a GitHub classic token was pasted into the old chat (scopes repo, workflow, delete_repo). It is **not stored here**. In the new chat he must give a **new** token (fine-grained or classic, minimal scopes: contents + workflows + secrets/actions for the repo). The old token should be deleted at github.com → Settings → Developer settings. Never write a token into a file or commit.

## 2. What the project is

- **MS Screen Reader = Google's open-source TalkBack source** (Apache-2.0) in this repo, rebuilt with Mobeen's changes and renamed. It must feel clearly different from TalkBack, but the **app structure/menus must not be broken** (he said: don't touch the structure).
- Android package (technical name, unchanged): `com.android.talkback`. `minSdk 26`, `targetSdk 30`, `compileSdk 36`. He wants it to support **Android 10 up to 16/17** (currently installs on Android 8+; OCR needs Android 11+, because taking a screenshot from an accessibility service is Android 11+).
- Build: GitHub Actions workflow `.github/workflows/build.yml` runs `gradle assemblePhoneDebug` (debug variant, `debuggable=true`), signs it with a **stable key from repo secrets**, uploads an artifact and publishes a **GitHub Release** named `build-<run number>` with `MSScreenReader.apk`.
- **Direct download (always latest):** https://github.com/mobinsayyea-netizen/talkback-master/releases/latest/download/MSScreenReader.apk
- **Updates must install over the old app (no uninstall).** This works because: same package, same signing key, and `versionCode = GITHUB_RUN_NUMBER` (always rising). **Never change the key or package.** Repo secrets (names only): `KEYSTORE_B64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` (= `msr`), `KEY_PASSWORD`. If those secrets are lost, the next update would need an uninstall. The current key certificate SHA-256 starts with `4509c918ce656518`.
- Build time is about 5–8 minutes. The workflow turns compile errors into GitHub annotations: read them with the API `GET /repos/mobinsayyea-netizen/talkback-master/check-runs/<job id>/annotations`.
- Commit messages containing `[skip ci]` do not trigger a build (use for docs-only changes).

### Working environment notes (for the AI)
- The sandbox has a Linux shell; network is limited to GitHub, PyPI, npm, etc. `git clone` of public repos works. Push using `git -c http.extraHeader="Authorization: Basic <base64 x-access-token:TOKEN>" push` so the token is not saved in `.git/config`.
- Release assets can be downloaded and inspected with `androguard` (`pip install androguard --break-system-packages`).
- Large pushes: `-c http.postBuffer=524288000`.
- The TalkBack source is big (about 4000 files). Java code in `talkback/src/main/java/com/google/android/accessibility/talkback/`.
- He cannot test quickly for you; he installs the APK and reports back in words. You cannot run the app. Be honest about what is unverified.

## 3. What is DONE (by build)

| Build | Contents |
|---|---|
| 2 (part A) | Renamed every visible "TalkBack" to "MS Screen Reader" in all string resources (script over `res/values*`), app/service label. **All TalkBack sounds replaced by CSR-pack sounds** (mapping below). Six new sounds. Battery level spoken after the time when the screen wakes (English, short: `Battery 65%`). Stable signing + rising versionCode + GitHub Release link. |
| 3 | **Copy** and **Translate** reading controls. On-phone translation (ML Kit Translate + Language ID). First-use permission dialog for language packs. "Translate" settings screen. App now has its own **name + icon** and a **launcher entry** (activity-alias `MsLauncherEntry`) so it shows in the app list (needed to long-press → App info → "Allow restricted settings"). The **Default** reading control turned on. New settings category **Recognition**. |
| 4 | **OCR** reading control (on the phone, ML Kit text recognition: Latin + Devanagari via Google Play services). OCR+Translate combined. **Gemini key box (shown once)**, Gemini settings screen (key + model), key/model read at request time, quota handling. |
| 5–8 | **English-only resources** (`resConfigs "en"`), OCR message on Android 10 ("needs Android 11"). APK went from 100.6 MB to **about 74 MB**. Both 32-bit and 64-bit libs are kept (his choice). |

Duplicate `resConfigs "en"` lines exist in the root `build.gradle` (4 lines). Harmless, **clean up to one line** in the next real build.

### 3.1 Sound mapping used (TalkBack resource name ← CSR file)
Raw resources are in `talkback/src/main/res/raw`, `braille/common/src/phone/res/raw`, `utils/src/main/res/raw` (same base names, mp3/ogg).
- focus ← "new gesture tone (1).mp3"; focus_actionable ← "focus0 (2).mp3"; view_entered ← focusin.mp3; hyperlink ← camera_focus.mp3
- long_clicked ← "long press.mp3"; gesture_begin ← window.ogg; gesture_end ← G.mp3; window_state ← "Window change.mp3"
- volume_beep ← volume.mp3; tick ← tick.mp3; scroll_tone ← scroll.mp3; chime_up ← "Scroll up.mp3"; chime_down ← "Scroll down.mp3"
- complete, calibration_done ← finalize.mp3; loading ← searching.mp3; typo ← error.mp3; formatting ← click.mp3; double_beep ← Border.mp3
- turn_on, turn_off ← Signal.ogg; browse_mode_on/off ← start.mp3 / pause.mp3; display_connected/disconnected ← start.mp3 / pause.mp3
- **New event sounds:** `screen_on` ← Screen_unlocked.mp3, `screen_off` ← "device unlock.mp3", `power_connected` ← "START CHARGING.ogg", `power_full` ← "FINISH CHARGING.ogg", `keyboard_focus` ← "Keyboard element.mp3", `clipboard` ← "click (2).ogg" (used for copy and paste).
- **Excluded on purpose (doubtful origin):** all `bdspeech_*` files and the WhatsApp-named click file (`double tapAUD-…WA…`). Do not add them. Marathi talking-clock files (Hour/Minute/Hourly) are **not** added yet.
- He **did not like several of these sounds** and wants to revisit them after the features are done (see TODO).

### 3.2 Code map of what was added/changed
- `talkback/.../ExtraSounds.java` – plays a short raw sound on the accessibility audio stream.
- `monitor/RingerModeAndScreenMonitor.java` – screen-on sound, screen-off sound, `Battery N%` appended after the time.
- `monitor/BatteryMonitor.java` – charger-connected and charging-complete sounds.
- `compositor/rule/EventTypeViewAccessibilityFocusedFeedbackRule.java` – keyboard keys use `keyboard_focus` sound.
- `actor/TextEditActor.java` – copy/paste sound.
- `selector/SelectorController.java` – new reading controls `COPY_TEXT`, `TRANSLATE_TEXT`, `OCR_TEXT` (enum entries, list, description, `adjustSelectedSetting` handlers, helper methods). Keys/defaults/ids live in `res/values/donottranslate.xml`; titles in `res/values/ms_strings.xml`; check boxes in `res/xml/selector_menu_preferences.xml`.
- `talkback/.../translate/` – `TranslateEngine`, `TranslateSetupActivity`, `TranslateSettingsActivity`, `OcrTool`.
- `actor/gemini/` – `GeminiKeyStore`, `GeminiKeyActivity`, `GeminiSettingsActivity`; patched `GeminiRestEndpoint`, `GeminiRestRequestPerformer`, `GeminiConfiguration`.
- `AndroidManifest.xml` (module + root): activities, `MsLauncherEntry` alias, app label/icon. `res/xml/preferences.xml`: new **Recognition** category.
- `shared.gradle`: ML Kit dependencies (text-recognition, devanagari, translate, language-id). Root `build.gradle`: versionCode, signing, `resConfigs`.

## 4. Behaviour he specified (must keep)

**Reading controls** (swipe up/down changes the value; in TalkBack code "next" = swipe down, "previous" = swipe up):
- **Copy:** swipe **down** = copy the focused element's text. Swipe **up** = append it as a **new line below** the earlier copied text. No extra settings.
- **Translate:** swipe **up** = speak the **original** text; swipe **down** = speak the **translation**. Default target **Hindi**, source auto-detect. On-phone engine. If the focused item has **no text** (e.g. a picture), run OCR first, then translate.
- **OCR ("Read text (OCR)"):** swipe **down** = read the text of the focused item from the screen; swipe **up** = read it and translate. **OCR must stay on the phone (no Gemini)** so Gemini quota is saved.
- **Language packs:** must download on **mobile data as well as Wi-Fi** (no Wi-Fi-only rule). If Translate was never set up, the permission to download is asked **right there** (dialog Download / Cancel), not in Settings; after he says yes, later packs download automatically.
- Speech of the translation uses `LocaleSpan(hi)`; needs a Hindi TTS voice.

**Gemini:** only for **image description, screen description and asking questions** (TalkBack's built-in features, wired to his key). He wants to add another Gemini feature himself later, so quota must be spared. Key box appears **only once ever** (Save or Cancel both stop it); afterwards only a short message "Gemini key is not set. Add it in settings, Recognition, Gemini." Quota finished: full message at most once per 10 minutes, otherwise short "Quota finished"; no requests for 2 minutes after a 429. Default model `gemini-flash-latest` (editable). For "what is in the picture" use **only Gemini** for now (free on-device labels not wanted now).

**Reorganised settings** (not done yet): categories **General settings, TTS settings, Sounds, Recognition, Controls, Typing, Advanced, About & Updates**. TTS options stay exactly as they are. Keep preference **keys unchanged** (only move/rename). Remove "Share your feedback" and "Contact Google Disability support". OCR & Translate settings live together under Recognition.

## 5. TODO — in this order

1. **Clipboard (next build).** Persistent history (kept until he deletes it). Screen with **check boxes to select several items ("Manage")**, plus **Favorite** and **Delete** on the selected. "Delete all" must keep favorites. Open it with **two-finger triple tap** (add a new shortcut action in `GestureShortcutMapping`, default on `TWO_FINGER_TRIPLE_TAP`, still re-assignable), plus an activity-alias "Clipboard" in the app list and an entry in settings. Record copies made by the Copy control and TalkBack's own copy (`TextEditActor`). Limit: Android blocks reading the clipboard from the background, so only these copies are captured.
2. **Settings restructure** (section 4) + **settings backup/restore** to a file (he had to uninstall once and lost settings) + a **separate on/off switch for the battery announcement** (now it follows the "tell time when screen wakes" setting). Do it step by step and show him the plan first.
3. **Clean-up:** one `resConfigs "en"` line.
4. **Part C (big):** make screen/focused-item **description and Q&A** work through his Gemini key (check whether TalkBack's own gating, e.g. blank supported-locale strings in `GeminiConfiguration`, hides the feature), **icon recognition**, a menu item **"Speak time and battery"** (`CONTROL_TELLING_TIME` reading control already exists but is off by default).
5. **In-app "Check for updates"** in About & Updates. A working example is `Updater.kt` in the repo `Mobin-Launcher` (GitHub Releases API + PackageInstaller). Needs `INTERNET` and `REQUEST_INSTALL_PACKAGES`.
6. **Sounds:** he wants to re-choose sounds after the features. Add a **Sounds screen** where each event can use any CSR sound or none. Ask him which ones bothered him. Later: the **Marathi talking clock** (files were in his `CSR.zip`: Clock/Hour, Clock/Minute, Clock/Hourly; ask him to re-upload if needed; the zip is not in the repo).
7. **Size (later, optional):** now about 74 MB. More savings only with risk: R8 shrinking (~8–10 MB), removing Braille (changes structure, no). ML Kit Translate alone is ~28 MB of native libs (both ABIs).
8. **Maybe later:** Play Store release (needs a different package name/branding since "TalkBack" is Google's trademark, a signed AAB, target SDK update, accessibility permission declaration, privacy policy, and a check of the sound files' rights).

## 6. Things to verify on his phone (unknown until he tests)
- Right-to-left / order of sounds; keyboard-key sound; battery line timing.
- That the Gemini key box actually appears from TalkBack's image-description path.
- First-time download of the Hindi/Marathi OCR model from Google Play services ("not ready yet, try again" message).
- Whether `takeScreenshot` works on his device.
- Behaviour on Android 10 (no OCR), and on Android 16/17 (untested; `targetSdk 30` may need raising later).

## 7. Related repos
- `mobin-talkback` — his own **from-scratch** Kotlin screen reader `MS_Screen_Reader_v1-43` (~7,400 lines). It has the reading-granularity idea (swipe up-then-down cycles Character/Word/Line/List/Copy; copy mode appends). We decided to take ideas into the TalkBack-based app instead of continuing it.
- `Mobin-Launcher` — his accessible Android launcher (see its `HANDOFF.md`).
- Others: `Advance-audio-editor.-`, `lMS-newspaper.-` (Suno News), `Amrapali-keyboard.-`, `PDF-reader.-`, `Universal-reader.-`, `mobin-tts`, `mobin-driving-simulator`, `Mobin-PDF-Reader`.

## 8. Glossary of his voice-typed words
- "मिस / एस स्क्रीन रीडर" = MS Screen Reader. "टॉकबैक" = TalkBack. "होशियार / ओसीआर" = OCR. "फोकस / फॉक्स" = focus. "डिफ़ॉल्ट" = Default. "अपेंड" = append. "क्लिपबोर्ड" = Clipboard. "बना" = start building. "जीसू / जीशू" = Jieshuo (a Chinese screen reader), "कॉमेंट्री स्क्रीन रीडर" = Commentary Screen Reader (CSR). "उल्टे हाथ" = left, "सीधे हाथ" = right. "रो कॉलम" = rows and columns. "एपीके / एपी की" = APK / API key. "रिपोर्ट" often means repo.
