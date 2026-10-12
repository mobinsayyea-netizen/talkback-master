# AndroLua_pro source map for MobinSR Step 30

Purpose: track the upstream sources required to implement the Lua helper runtime. This file is a map, **not a claim that these sources have already been copied**.

Upstream repository: https://github.com/nirenr/AndroLua_pro

## Source files identified
- `app/src/main/java/com/androlua/LuaAsyncTask.java` — depends on `com.androlua.util.AsyncTaskX`, LuaJava APIs, `LuaContext`, `LuaPrint`, and `LuaGcable`.
- `app/src/main/java/com/androlua/LuaThread.java` — depends on `LuaMetaTable`, `LuaState`, `LuaStateFactory`, `LuaContext`, `LuaGcable`, and Android Handler/Looper. It contains `LuaActivity`/`LuaService` branches that must be adapted because MobinSR does not ship `LuaActivity`.
- `app/src/main/java/com/androlua/LuaTimer.java` — depends on `TimerX` and `LuaTimerTask`.
- `app/src/main/java/com/androlua/LuaTimerTask.java` — depends on `TimerTaskX`, LuaJava APIs, `LuaContext`, and `LuaPrint`; `LuaActivity` branches require review.
- `app/src/main/java/com/androlua/Http.java` — depends on `AsyncTaskX` and LuaJava APIs. Review Android network/security behavior and actual callers before integrating.
- `app/src/main/java/com/androlua/Download.java` — uses Android `DownloadManager`, `LuaContext`, and an app context. Its external-storage behavior needs Android-version review.
- `app/src/main/java/com/androlua/util/AsyncTaskX.java`
- `app/src/main/java/com/androlua/util/TimerX.java`
- `app/src/main/java/com/androlua/util/TimerTaskX.java`
- `app/src/main/java/com/androlua/util/VolatileBox.java` and any helpers required by the selected files (confirm by checking imports).

## Native sources to inspect separately
- `app/src/main/jni/` in upstream. Do not copy its whole native tree blindly. Compare Lua version, JNI names, ABI filters, compiler flags, and MobinSR's existing `talkback/src/main/jni/Android.mk` first.
- The requested `bson`, `cjson`, `crypt`, and `regex` modules are not present in the supplied MobinSR ZIP as source/native outputs. Do not add empty `.so` placeholders.

## Porting rules
1. Preserve MobinSR's existing `LuaContext` and `LuaGcable` contracts.
2. Remove or adapt references to `LuaActivity`; keep the `LuaService`/`service` path only where it fits MobinSR.
3. Preserve upstream license/copyright headers and include the applicable license file.
4. Compile against the project's actual Android SDK and Java language level; do not infer success from source similarity.
5. Check every `import.lua` constructor/signature and test task/thread/timer lifecycle, cancellation, argument passing, and service shutdown.
6. Do not change package ID, signing configuration, or keys.

## Status
Source pages were inspected during Step 30, but a repository checkout/download was unavailable in the working container. No upstream Java implementation is included by this map.
