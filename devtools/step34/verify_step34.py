#!/usr/bin/env python3
"""Source-level checks for Step 34. Does not compile Android or native code."""
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
checks = {
    "cjson Lua compatibility shim exists": (ROOT / "talkback/src/main/assets/lua/cjson.lua", "function cjson.safe_decode(value)"),
    "cjson delegates to bundled JSON module": (ROOT / "talkback/src/main/assets/lua/cjson.lua", 'require("json")'),
    "safe encoder reports errors": (ROOT / "talkback/src/main/assets/lua/cjson.lua", "return nil, result"),
    "core copy API": (ROOT / "talkback/src/main/java/com/google/android/accessibility/talkback/editor/LuaServiceApi.java", "public boolean appendCopy(CharSequence text)"),
    "core paste API": (ROOT / "talkback/src/main/java/com/google/android/accessibility/talkback/editor/LuaServiceApi.java", "public boolean paste(AccessibilityNodeInfo n)"),
    "gesture command API": (ROOT / "talkback/src/main/java/com/google/android/accessibility/talkback/editor/LuaServiceApi.java", "public boolean click(com.luajava.LuaTable list)"),
    "service forwarding": (ROOT / "talkback/src/main/java/com/google/android/accessibility/talkback/editor/LuaServiceApi.java", "public AccessibilityNodeInfo getRootInActiveWindow()"),
    "native build remains explicitly limited": (ROOT / "STEP34_NOTES.md", "Native `bson`, `cjson`, `crypt`, and `regex` libraries are still not integrated"),
}
failed = []
for label, (path, marker) in checks.items():
    ok = path.exists() and marker in path.read_text(errors="replace")
    print(("PASS" if ok else "FAIL") + ": " + label)
    if not ok: failed.append(label)
if failed: raise SystemExit("Failed: " + ", ".join(failed))
print("LIMITATION: marker checks only; no Android/NDK build or runtime test.")
