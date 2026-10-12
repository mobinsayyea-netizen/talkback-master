# Step 33 — service forwarding, opt-in hooks, and folder aliases (2026-10-11)

## Implemented in this source pass
- Added `LuaServiceApi.runHookBoolean(...)`; a hook's Lua return value is interpreted as `true` only when the function explicitly returns true.
- Added opt-in `onKeyEvent` and `onGestureDetected` dispatch from `TalkBackService`. A Lua hook returning true consumes that key/gesture. These hooks are synchronous because a consumed result is needed immediately.
- Added opt-in `onVolumeChanged` dispatch from `VolumeMonitor` on the Lua extension worker, passing `(streamType, currentValue, previousValue, percent)`.
- Added common `AccessibilityService` forwarding methods on `LuaServiceApi`: `performGlobalAction`, `getRootInActiveWindow`, `getWindows`, `getServiceInfo`, `setServiceInfo`, `startActivity`, plus common context/package/resource/system-service accessors.
- Added path aliases for `Plugins`/`Extensions`, `Tools`, `Custom voice commands`, `Timer`, and `Profiles` through `LuaPaths.alias`; `getLuaExtDir`/`getLuaExtPath` now resolve these aliases.
- Added `devtools/step33/verify_step33.py`; all nine source-marker checks passed.

## Important limits
- Key and gesture hooks execute synchronously per opted-in extension; scripts should keep their hook functions short. Android latency and event semantics are not verified.
- `onVolumeChanged` is asynchronous and its Lua return value is ignored; it is a notification hook, not a volume-event consumer.
- Folder aliases resolve storage paths but do not yet implement every Jieshuo profile/timer/voice-command data format.
- The full `setUseXxx`/`setXxx` API surface is not implemented; do not mark C3 complete. Methods without a real MobinSR setting should not be fabricated as working toggles.
- Several execute commands require new UI/system features, and real `.ppk` import, native library integration, GitHub Actions build, and device tests remain open.
- No Android Gradle build or phone test was performed. This is a source-level implementation pass only.

## Checks
- `python devtools/step32/verify_step32.py`: passed.
- `python devtools/step33/verify_step33.py`: 9/9 source-marker checks passed.
- ZIP integrity is checked after packaging.
