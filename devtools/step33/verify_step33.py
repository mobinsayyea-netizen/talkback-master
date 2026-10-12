#!/usr/bin/env python3
"""Static checks for Step 33 hook, forwarding, and path-alias additions.
These checks do not compile Android code or prove runtime behavior.
"""
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
checks = {
    "boolean hook runner exists": (ROOT / "talkback/src/main/java/com/google/android/accessibility/talkback/editor/LuaServiceApi.java", "runHookBoolean("),
    "Accessibility global action forwarding": (ROOT / "talkback/src/main/java/com/google/android/accessibility/talkback/editor/LuaServiceApi.java", "performGlobalAction(int action)"),
    "active window forwarding": (ROOT / "talkback/src/main/java/com/google/android/accessibility/talkback/editor/LuaServiceApi.java", "getRootInActiveWindow()"),
    "activity forwarding": (ROOT / "talkback/src/main/java/com/google/android/accessibility/talkback/editor/LuaServiceApi.java", "startActivity(Intent intent)"),
    "Jieshuo folder aliases": (ROOT / "talkback/src/main/java/com/google/android/accessibility/talkback/editor/LuaPaths.java", "public static File alias(Context c, String name)"),
    "key hook integrated": (ROOT / "talkback/src/main/java/com/google/android/accessibility/talkback/TalkBackService.java", "LuaExtensionManager.handleKeyEvent(this, keyEvent)"),
    "gesture hook integrated": (ROOT / "talkback/src/main/java/com/google/android/accessibility/talkback/TalkBackService.java", "LuaExtensionManager.handleGesture(this, gestureId)"),
    "volume hook integrated": (ROOT / "talkback/src/main/java/com/google/android/accessibility/talkback/monitor/VolumeMonitor.java", "LuaExtensionManager.onVolumeChanged("),
    "hook consumption rule documented": (ROOT / "talkback/src/main/java/com/google/android/accessibility/talkback/editor/LuaExtensionManager.java", "Returning true from Lua consumes the gesture"),
}
failed = []
for label, (path, marker) in checks.items():
    ok = path.exists() and marker in path.read_text(errors="replace")
    print(("PASS" if ok else "FAIL") + ": " + label)
    if not ok: failed.append(label)
if failed:
    raise SystemExit("Failed: " + ", ".join(failed))
print("LIMITATION: source-marker checks only; no Android build/device/runtime verification.")
