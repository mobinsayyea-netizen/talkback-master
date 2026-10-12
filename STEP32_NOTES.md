# Step 32 — combined Lua runtime completion pass (2026-10-11)

## Implemented in this source pass
- Added local Java compatibility implementations for `LuaAsyncTask`, `LuaThread`, `LuaTimer`, `LuaTimerTask`, and `Ticker`.
- Added small `com.androlua.util` helpers: `AsyncTaskX`, `TimerX`, `TimerTaskX`, and `VolatileBox`.
- Added a Java `Http` facade for common asynchronous GET/POST/PUT/DELETE/download calls, plus shared headers/cookie/referer/user-agent setters.
- Added a `DownloadManager`-backed `Download` class using app-scoped external files storage.
- Implemented `LuaServiceApi.call()` so it invokes a Lua global function under the same Lua-state lock instead of doing nothing; synchronized `set()` with the same lock.
- Added more Jieshuo Smali references for the corresponding runtime/helper classes under `reference/jieshuo_smali/`. These are reference-only and are not compiled into the APK.

## Important compatibility limits (do not mark as fully complete)
- The new Java classes are compact local compatibility implementations, **not verbatim upstream AndroLua source ports**. Some AndroLua overloads and edge cases are not implemented yet; in particular the full multipart/form overload matrix and exact cancellation/restart behavior need comparison against the Smali signatures.
- `LuaThread` uses MobinSR's shared Lua state and serializes calls; it does not create the independent Lua state used by upstream AndroLua. This avoids unsafe concurrent access but is not identical behavior.
- `LuaTimer.stop()` cancels its underlying Timer and is not guaranteed to be restartable; timer pause/resume semantics need device/runtime tests.
- `Download` interprets the destination as a subdirectory under app-scoped external files, not arbitrary public filesystem paths. This is deliberate for scoped-storage safety but differs from legacy behavior.
- `cjson`, `crypt`, and `regex` native `.so` files in `cSr.zip` are proprietary prebuilt binaries for a limited ABI; they were **not copied** into the project. `bson` is still missing. Native source/build integration remains open.
- The `Http` Java facade is common-case coverage, not the full Jieshuo overload matrix.
- The supplied Jieshuo package is decompiled Smali, not original Java source. Smali references are not included in the APK.

## Checks performed
- Re-ran `devtools/step30/verify_step30.py` before the final additions; it passed its five text checks and reported missing upstream runtime files. Re-run after the final additions is recorded in `devtools/step32/verify_step32.py`.
- Ran source-level consistency checks for required class names, callback methods, `LuaServiceApi.call`, and ZIP integrity.
- Compiled the newly added runtime/helper Java files with `javac` against temporary minimal Android/Lua API stubs; this checks Java syntax and internal type usage, **not** compatibility with the actual Android SDK or the full app.
- No Android Gradle build or phone test was performed in this environment. All newly added Android-dependent Java remains **unverified until GitHub Actions and a device test**.

## Next unresolved items
1. Compare the local classes against the complete Jieshuo Smali signatures and fix constructor/overload differences.
2. Integrate the native modules from legally usable source, including `bson`, for every supported ABI; do not copy proprietary prebuilt binaries blindly.
3. Complete the ~200 setting APIs and remaining hooks/commands from `JX_SERVICE_SIGNATURES.txt` and `REMAINING_COMMANDS.txt`.
4. Verify a real `.ppk`, build the APK in GitHub Actions, and test Android 13/15/16.
