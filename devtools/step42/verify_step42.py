#!/usr/bin/env python3
"""Step 42 static checks: Lua `service` is a real Context; setSharedData/getSharedData are persistent."""
import os, re, sys
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
E = "talkback/src/main/java/com/google/android/accessibility/talkback/editor/"
s = open(os.path.join(ROOT, E + "LuaServiceApi.java"), encoding="utf-8", errors="replace").read()
res = []
def chk(n, ok): res.append(bool(ok)); print(("PASS: " if ok else "FAIL: ") + n)
chk("LuaServiceApi extends ContextWrapper and still implements LuaContext",
    "public class LuaServiceApi extends android.content.ContextWrapper implements LuaContext" in s)
m = re.search(r"private LuaServiceApi\(Context c, File dir\) throws Exception \{(.*?)super\(c\);", s, re.S)
chk("super(c) is the first statement of the constructor (only comments before it)",
    m and not re.sub(r"//[^\n]*", "", m.group(1)).strip())
chk("setSharedData writes to SharedPreferences lua_shared_data", 'SHARED_DATA_PREFS = "lua_shared_data"' in s and "e.apply();" in s)
chk("getSharedData reads the saved value before the in-memory map",
    s.index("sharedDataPrefs().getAll().get(key)") < s.index("Object v = globalData.get(key);"))
chk("non-integral numbers are saved exactly (marker + Double)", "DOUBLE_MARK" in s and "Double.valueOf(" in s)
chk("setSharedData(key, null) removes the saved value", "e.remove(key);" in s)
t = re.sub(r'"(\\.|[^"\\])*"', '""', s); t = re.sub(r"//.*", "", t); t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
chk("braces and parentheses balance", t.count("{") == t.count("}") and t.count("(") == t.count(")"))
print(f"\n{sum(res)}/{len(res)} static checks passed"); sys.exit(0 if all(res) else 1)
