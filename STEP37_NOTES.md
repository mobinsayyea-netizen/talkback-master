# Step 37 — real setter mapping + HTTP failure handling (2026-10-11)

## Three substantial changes

1. **Fixed the preference-store bug in real setter mappings.** Step 36's compatibility values are stored in `jx_compat_settings`, but the real mapped feature flags must be stored in TalkBack's normal `SharedPreferencesUtils` store. All mapped feature writes now use the actual feature preference store. This corrects the previously ineffective real mappings for OCR, edge gestures, multi-finger gestures, and accessibility volume.
2. **Expanded confirmed behavior mappings.** `setUseLog` maps to MobinSR's logging flag; `setAutoTrans` maps to automatic translation; `setUseHotkey` maps to the inverse `ms_cmd_disable_hotkey` flag; and `setIncallMode(boolean)` applies earpiece mode through a shared helper. The no-argument `setIncallMode()` enables earpiece mode. The earpiece command and API setter now use the same helper. These mappings rely on existing MobinSR features; they do not imply other setters are implemented.
3. **Made HTTP calls fail more gracefully.** `http.get`, `http.post`, `http.download`, and multipart `http.upload` now guard network/file exceptions at their public entry points, return a nil result plus an error detail rather than immediately throwing into the caller, and attempt to disconnect completed connections. Existing success return values are preserved.

## Verification

- Added `devtools/step37/verify_step37.py`.
- All static verification scripts from Steps 30–37 passed in this workspace.
- No Android Gradle build was run; no Lua interpreter or phone runtime test was available. HTTP return behavior on Java/Lua bridge and earpiece routing remain unverified on-device.
- Native `bson`, `cjson`, `crypt`, and `regex` libraries are still not integrated as native libraries. `cjson.lua` remains a Lua compatibility shim, not native cjson.
- The remaining stored-only setter methods remain compatibility placeholders, not actual feature implementations.
- Package ID, signing configuration, and keys were not changed.
