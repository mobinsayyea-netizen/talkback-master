# Step 38 — real AndroLua classes, native modules, build-breaking fixes (2026-10-11)

## Source
AndroLua_pro (https://github.com/nirenr/AndroLua_pro, MIT; license already in assets/licenses/androlua-LICENSE.txt) downloaded this time. Native sources copied from app/src/main/jni/{bson,cjson,crypt,regex}.

## Done
1. Native modules: talkback/src/main/jni/{bson,cjson,crypt,regex} with rewritten Android.mk (gnu99, shared luajava, 16KB page size). Top Android.mk includes them after luajava. luajava now uses LOCAL_WHOLE_STATIC_LIBRARIES := lua so the full Lua C API is exported to the modules.
2. Removed assets/lua/cjson.lua (moved to reference/legacy_cjson_shim.lua); it shadowed the native module on package.path.
3. Replaced the small compatibility classes with the real upstream LuaAsyncTask, LuaThread, LuaTimer, LuaTimerTask, Ticker, Http, Download, LuaPrint and util/{AsyncTaskX,TimerX,TimerTaskX,VolatileBox}.
   Adaptations: no LuaActivity/LuaService branches; the Lua global "service" in task/thread/timer states is the LuaContext (LuaServiceApi); async callbacks run under synchronized(getLuaState()); registration lifecycle (regGc/unregGc) added on completion, cancel, thread end, one-shot timer; LuaThread LuaObject constructors now register; LuaTimerTask guards a null state.
   Behaviour change: task/thread/timer now run in their own Lua state (as in real AndroLua/Jieshuo), no longer on the shared state.
4. LuaServiceApi: the LuaState itself is the lock (luaLock removed); getLuaLpath() returns bundled lua dir + script dir (needed for require "import" inside task/thread/timer states).
5. Build-breaking bugs fixed (both already present in the Step 27 zip): focusedNode() defined twice (second renamed focusedOrInputNode(), used by textEdge/listEdge); missing import android.os.Bundle.

## Verified
- devtools/step38/verify_step38.py: 24/24 (with ANDROID_JAR set, includes a javac compile of the ported classes against android.jar API 33).
- javac of editor/*.java, androlua/*.java: no errors in our own code; remaining errors are only generated R classes and the external library supertype AccessibilityServiceCompat (not in repo).
- Host Linux test (devtools/step38/HOST_NATIVE_TEST.md): real libluajava.so + modules loaded via the project's LuaState: cjson, bson, crypt (sha1/base64), regex all PASS.

## NOT verified
- No NDK/Gradle/GitHub Actions build was run. No phone test. Task/thread/timer runtime behaviour untested.
- Real Jieshuo .ppk never opened.

## Still open
- Step 36 setters: most only store a value (real feature only for the 8 from Step 37).
- ~60 execute() commands still return false (camera, voice dictation, cloud clipboard, virtual screen ...).
- Remaining upstream natives (lfs, zip, socket, lsqlite3, ...) not ported; only the four requested.
- Old verifiers for steps 32/34/35 are superseded (see devtools/SUPERSEDED_BY_STEP38.md).
- Plan C3 says 230 setters; the correct number is 226.

## Next
Run the Actions build, send the log; fix compile/link errors first (most likely spots: new Android.mk modules, upstream Http/Download on newer Android, LuaThread Handler use).
