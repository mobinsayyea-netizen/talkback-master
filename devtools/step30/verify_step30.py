#!/usr/bin/env python3
"""Static-only checks for the MobinSR Step 30 archive; no Android build/runtime test."""
from pathlib import Path
import sys
ROOT = Path(__file__).resolve().parents[2]
checks = {
    "Lua import guarded class binding": (ROOT / "talkback/src/main/assets/lua/import.lua", 'pcall(luajava.bindClass, "com.androlua.LuaThread")'),
    "Lua task fallback scheduler": (ROOT / "talkback/src/main/assets/lua/import.lua", "scheduleFallback"),
    "LuaServiceApi openUrl": (ROOT / "talkback/src/main/java/com/google/android/accessibility/talkback/editor/LuaServiceApi.java", "void openUrl(String url)"),
    "LuaServiceApi scheduleTask": (ROOT / "talkback/src/main/java/com/google/android/accessibility/talkback/editor/LuaServiceApi.java", "void scheduleTask(double id, double ms)"),
    "Editor automatic-click one-table form": (ROOT / "talkback/src/main/java/com/google/android/accessibility/talkback/editor/LuaEditorActivity.java", 'service.click{{'),
}
failed = False
for label, (path, needle) in checks.items():
    ok = path.exists() and needle in path.read_text(encoding="utf-8", errors="replace")
    print(("PASS" if ok else "FAIL") + ": " + label)
    failed |= not ok
missing = [name for name in ("LuaAsyncTask.java", "LuaThread.java", "LuaTimer.java", "LuaTimerTask.java", "Http.java", "Download.java") if not list((ROOT / "talkback/src/main/java/com/androlua").glob(name))]
print("SOURCE STATUS: missing upstream runtime files: " + (", ".join(missing) if missing else "none"))
print("LIMITATION: static text checks only; not a Java compile, Lua runtime test, Android build, or phone test.")
sys.exit(1 if failed else 0)
