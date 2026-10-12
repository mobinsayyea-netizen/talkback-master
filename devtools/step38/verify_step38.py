#!/usr/bin/env python3
"""Step 38 static checks (run from the repo root or devtools/step38). Static only: it does not prove the APK builds."""
import os, re, sys
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
def rd(p):
    with open(os.path.join(ROOT, p), encoding="utf-8", errors="replace") as f: return f.read()
def ex(p): return os.path.exists(os.path.join(ROOT, p))
J = "talkback/src/main/jni"; A = "talkback/src/main/java/com/androlua"
API = "talkback/src/main/java/com/google/android/accessibility/talkback/editor/LuaServiceApi.java"
res = []
def chk(name, ok): res.append((name, bool(ok))); print(("PASS: " if ok else "FAIL: ") + name)

for m, srcs in {"bson": ["lua-bson.c"], "cjson": ["lua_cjson.c", "fpconv.c", "strbuf.c"],
                "crypt": ["lcrypt.c", "lsha1.c"], "regex": ["lposix.c", "common.c"]}.items():
    chk(f"native module {m}: Android.mk and sources", ex(f"{J}/{m}/Android.mk") and all(ex(f"{J}/{m}/{s}") for s in srcs))
    mk = rd(f"{J}/{m}/Android.mk") if ex(f"{J}/{m}/Android.mk") else ""
    chk(f"native module {m}: shared luajava, 16KB page flag", "LOCAL_SHARED_LIBRARIES := luajava" in mk and "max-page-size=16384" in mk)
top = rd(f"{J}/Android.mk")
order = [top.find(x) for x in ("luajava/Android.mk", "bson/Android.mk", "cjson/Android.mk", "crypt/Android.mk", "regex/Android.mk")]
chk("top Android.mk includes modules after luajava", all(o >= 0 for o in order) and order == sorted(order))
chk("libluajava exports the whole Lua API", "LOCAL_WHOLE_STATIC_LIBRARIES := lua" in rd(f"{J}/luajava/Android.mk"))
chk("legacy cjson.lua shim no longer shadows native cjson", not ex("talkback/src/main/assets/lua/cjson.lua") and ex("reference/legacy_cjson_shim.lua"))

at, th, tt, tm = (rd(f"{A}/{n}.java") for n in ("LuaAsyncTask", "LuaThread", "LuaTimerTask", "LuaTimer"))
chk("upstream-size AndroLua classes present", all(os.path.getsize(os.path.join(ROOT, f"{A}/{n}.java")) > 3000 for n in ("LuaAsyncTask", "LuaThread", "LuaTimerTask", "Http", "Download")) and os.path.getsize(os.path.join(ROOT, f"{A}/util/AsyncTaskX.java")) > 10000)
chk("no LuaActivity/LuaService branches left", not any(re.search(r"instanceof\s+Lua(Activity|Service)", s) for s in (at, th, tt)))
chk("task/thread/timer states get the 'service' global", all('setGlobal("service")' in s for s in (at, th, tt)))
chk("async callbacks lock the main Lua state", at.count("synchronized (mLuaContext.getLuaState())") >= 2)
chk("async task unregisters (done and cancelled)", at.count("unregGc(this)") >= 3 and "onCancelled(Object" in at)
chk("LuaThread registers in every constructor path and unregisters", th.count("regGc(this)") >= 2 and th.count("unregGc(this)") >= 2)
chk("LuaTimerTask releases owner on cancel / one-shot end", "setOwner(" in tt and tt.count("release()") >= 3 and "if (L != null)" in tt)
chk("LuaTimer sets one-shot vs repeating", "setOwner(this, true)" in tm and "setOwner(this, false)" in tm)

api = rd(API)
chk("LuaServiceApi: LuaState is the lock (no luaLock)", "luaLock" not in api and "synchronized (L)" in api)
chk("LuaServiceApi: getLuaLpath includes bundled lua dir", "luaLibDir" in api and re.search(r"getLuaLpath\(\)\s*\{[^}]*luaLibDir", api, re.S))
chk("LuaServiceApi: android.os.Bundle imported", "import android.os.Bundle;" in api)
chk("LuaServiceApi: focusedNode() defined exactly once", len(re.findall(r"AccessibilityNodeInfo\s+focusedNode\s*\(\s*\)\s*\{", api)) == 1 and "focusedOrInputNode()" in api)

jar = os.environ.get("ANDROID_JAR")
if jar and os.path.exists(jar):
    import subprocess, tempfile, glob
    sp = ":".join(glob.glob(os.path.join(ROOT, "**/src/*/java"), recursive=True))
    out = tempfile.mkdtemp()
    files = [os.path.join(ROOT, A, n + ".java") for n in ("LuaAsyncTask", "LuaThread", "LuaTimer", "LuaTimerTask", "Ticker", "Http", "Download", "LuaPrint")]
    import shutil
    jc = ["javac"] if shutil.which("javac") else ["java", "-m", "jdk.compiler/com.sun.tools.javac.Main"]
    r = subprocess.run(jc + ["-proc:none", "-nowarn", "-cp", jar, "-d", out, "-sourcepath", sp] + files, capture_output=True, text=True)
    chk("javac: ported androlua classes compile against android.jar", r.returncode == 0)
else:
    print("SKIP: javac check (set ANDROID_JAR=/path/to/android.jar to enable)")

bad = [n for n, ok in res if not ok]
print(f"\n{len(res) - len(bad)}/{len(res)} static checks passed")
sys.exit(1 if bad else 0)
