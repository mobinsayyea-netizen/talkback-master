# TTS merge notes: Advanced Screen Reader + Nuance Vocalizer/Eloquence engine (personal use)

Status: tested on Moto G45 (Android 15). Hindi, Marathi and English read correctly, mixed text too,
with "Advanced Screen Reader TTS" as the default engine. Latest build is v7 (see "Open items").

IMPORTANT: this repo is public. The engine binaries (libNuanceVocalizer.so, libeloqiumjni.so) and the voice
files (*.xmf, *.jet) come from a third-party APK (All_In_One_TTS.apk, package aio.tts). Never commit them
here or put an APK that contains them in a public Release. Keep them on the phone only.

## How the merged APK is made (no Gradle needed; done in a chat with code execution)
Tools (from GitHub releases): apktool 2.11.1 (jar), uber-apk-signer 1.3.0 (jar), Java 21.
Inputs: the Advanced Screen Reader release APK (MSScreenReader.apk, build 48 or newer) and All_In_One_TTS.apk.

1. `apktool d -s -f base.apk -o base`        (-s keeps the original classesN.dex untouched)
2. `apktool d -r -f All_In_One_TTS.apk -o aio` (-r = no resources; we only need smali, libs, assets)
3. Make a trimmed smali tree: copy aio/smali/{aio, org/eloqium, com/nuance} only. Build it with
   `apktool b -r -f aio_trim -o aio_trim.apk`, take its classes.dex and save it as base/classes25.dex
   (use the next free number). The activities and settings screens of aio are NOT needed.
4. Smali patches in the trimmed tree:
   - aio/tts/App.smali: make `context` a public static field.
   - aio/tts/Tts.smali, onCreate: after super.onCreate set App.context = getApplication(),
     System.loadLibrary("nofdsan") inside try/catch, then call the Prefs "...Once" migrations that App.onCreate used
     to call (resetVolumeRatePitchToNewDefaultsOnce, resetNumberEmojiVoiceToMatchLanguageOnce,
     migrateDictionaryEnToVocalizerEnglishOnce, migrateGoogleVolumeDefaultOnce, migrateDictionaryToPerLanguageOnce,
     seedDictionaryWordExclusionDefaultsOnce, seedDictionaryWordExclusionDefaultsV2Once).
     Do NOT use aio's App class or its activity lifecycle callbacks.
   - aio/tts/Dbg.smali (new): forwards real Vocalizer problems to MsErrorLog.log("Advanced TTS", msg) (Error log screen,
     copied to clipboard). Filters out any message containing "callback", "problem in" or "error log" (otherwise the
     spoken alert causes a feedback loop). Max 30 messages per process, no repeats. Also Dbg.crumb() -> MsErrorLog.crumb().
     Keep it simple: a wide-register mistake here once caused a VerifyError that silenced the whole TTS.
   - Replace android.util.Log.e calls in aio/ and com/nuance/ with Dbg.e; hook DetailedLog.log and DiagnosticLog.log to
     Dbg.maybe(msg) (only messages mentioning "vocalizer" plus fail / returned false / not ready).
   - Tts.onSynthesizeText: renamed to onSynthesizeText$orig and wrapped by a new onSynthesizeText that catches Throwable
     (a "Bad audio format 0" IllegalArgumentException from callback.done() used to kill the SynthThread).
5. Manifest (base/AndroidManifest.xml): add inside <application>
   `<service android:name="aio.tts.Tts" android:exported="true" android:permission="android.permission.BIND_TEXT_TO_SPEECH">`
   with intent-filter action android.intent.action.TTS_SERVICE + category DEFAULT, and meta-data android.speech.tts ->
   @xml/ms_tts_engine. Create res/xml/ms_tts_engine.xml = `<tts-engine android:settingsActivity="com.android.talkback.TalkBackPreferencesActivity"/>`.
   Set android:extractNativeLibs="true".
6. Native libs into base/lib/arm64-v8a/: libNuanceVocalizer.so, libeloqiumjni.so, libnofdsan.so.
   Do not add liblanguage_id_l2c_jni.so or tflite_langid.tflite.jpg: the base app already has them.
7. libnofdsan.so patch: the file offset 0x2ec holds `mov w0,#1` (20 00 80 52). Change it to `mov w0,#0` (00 00 80 52), so the
   fdsan level is DISABLED. With level 1 (WARN_ONCE) Nuance closing a file descriptor made the whole app die
   (CRASH_NATIVE: fdsan_error <- close <- CVocalizerEngine::vplatform_file_Close).
8. Assets into base/assets/vocalizer/ (only the four voices to keep the APK small):
   shared: lid.xmf clm.xmf sysdct.xmf synth_med_fxd_bet3f22.xmf userdct_hei.xmf
   Hindi: hii_lekha_*.xmf + ve_pipeline_hii_lekha_*.jet, hii_neel_*.xmf + ve_pipeline_hii_neel_*.jet
   Marathi: mai_ananya_*.xmf + ve_pipeline_mai_ananya_*.jet (Nuance code "mai" is Marathi, voice Ananya)
   English (India): eni_rishi_*.xmf + ve_pipeline_eni_rishi_*.jet
   Also assets/emoji/*.json. English default voice in the code is "ETI Eloquence", so libeloqiumjni.so is required.
9. base/apktool.yml: add `xmf` and `jet` to doNotCompress. The Nuance asset manager calls AssetManager.openFd, which
   fails on compressed entries. Without this Hindi/Marathi/Rishi are silent while Eloquence still works.
10. `apktool b -f base -o merged_unsigned.apk`, then `java -jar uber-apk-signer.jar -a merged_unsigned.apk --allowResign`.
    The signer's debug key differs from the repo's CI key, so the merged APK does not update over a CI build
    (uninstall first). Re-merging with the same signer keeps the same key, so merged builds update each other.

## Behaviour (from the aio code)
- Languages enabled by default: en, hi, mr, bn, gu, pa, ur. Text is split by script, so English words use Eloquence and
  Devanagari uses Lekha (Hindi) or Ananya (Marathi). A built-in detector decides Hindi vs Marathi.
- One Voice per language is exposed to Android (names like aiotts-mr).
- No settings screens from aio exist in the merged app, so there is no mode switch like Auto TTS (Dual / Mixed / Multilingual).

## Open items
- v7 (fdsan level 0, synth exception wrapper, filtered logging) is untested; check the Error log after a few hours of use.
- Possible option: "Devanagari always Marathi" for short Marathi sentences that the detector reads as Hindi.
- Possible: Auto TTS-style mode settings inside our own settings.
- Process death LOW_MEMORY entries after the crash are mostly a consequence; recheck after v7.
