# Step 35 — Lua asynchronous lifecycle and combined audit (2026-10-11)

## Implemented in this combined version
- Added `LuaContext.unregGc(LuaGcable)` and implemented a tracked identity set in `LuaServiceApi`.
- The Lua state now remains alive after the top-level script returns while registered `LuaAsyncTask`, `LuaThread`, or `LuaTimerTask` objects still depend on it; it closes only after scheduled tasks and registered objects are idle.
- `LuaAsyncTask` unregisters itself after completion or cancellation, after allowing its completion callback to run.
- `LuaThread` unregisters itself after its run/Looper loop exits.
- One-shot `LuaTimerTask` unregisters itself after its callback; repeating timers remain registered until cancelled/stopped.
- Added `devtools/step35/verify_step35.py` to guard these lifecycle invariants.
- Preserved all prior files and features from Step 34. Package ID, signing configuration, and keys were not changed.

## Why this matters
Previously, `LuaServiceApi.regGc()` was empty. The engine could close its Lua state as soon as the top-level script returned even though an asynchronous task or timer still needed that state. This step fixes that lifecycle gap at source level.

## Verification
- Static checks from Steps 30–35 are run before packaging.
- These checks do not prove Android compilation or runtime correctness. GitHub Actions build and phone testing remain required.

## Still open; not claimed complete
- Exact AndroLua/Jieshuo behavioral parity for `LuaAsyncTask`, `LuaThread`, `LuaTimer`, `Http`, and `Download`; the Java files are compatibility implementations, not verified upstream source ports.
- Native `bson`, `cjson`, `crypt`, and `regex` shared libraries and ABI/build integration.
- Full `service.*` method and setter parity against the user's supplied list; names alone are insufficient to implement correct settings behavior without exact signatures and matching TalkBack settings.
- Remaining commands requiring real features/design decisions (camera UI, voice dictation, cloud clipboard server, virtual screen/screen grid, focus monitor, suspend-mode switches).
- Real Jieshuo `.ppk` import, Android Gradle/NDK build, GitHub Actions build, and device tests.
