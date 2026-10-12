#!/usr/bin/env python3
"""Step 41 static checks: live gesture speed, faster defaults, 3 more real setters, compile_check tool."""
import os, re, subprocess, sys
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
def rd(p):
    with open(os.path.join(ROOT, p), encoding="utf-8", errors="replace") as f: return f.read()
U = "utils/src/main/java/com/google/android/accessibility/utils/gestures/"
E = "talkback/src/main/java/com/google/android/accessibility/talkback/editor/"
res = []
def chk(n, ok): res.append(bool(ok)); print(("PASS: " if ok else "FAIL: ") + n)
sw, gm, api = rd(U + "Swipe.java"), rd(U + "GestureManifold.java"), rd(E + "LuaServiceApi.java")
chk("Swipe thresholds recomputed live (applyConfirmDistance/refreshAll)", "applyConfirmDistance" in sw and "static void refreshAll()" in sw and "private int maxStartThreshold" in sw)
chk("GestureManifold listens for the two settings and refreshes swipes", "registerOnSharedPreferenceChangeListener" in gm and "Swipe.refreshAll()" in gm)
chk("faster defaults in code (0.6 cm, 65 %)", '"ms_gesture_swipe_cm", "0.6"' in gm and '"ms_gesture_hold_percent", "65"' in gm)
chk("setDoubleFlingTime writes the real setting", 'setDoubleFlingTime' in api and "MsMultiPartGestures.PREF_TIME" in api)
chk("setUseFingerprintGesture and setUseFloatMenu write the real settings", '"ms_jx_use_fingerprint_gesture"' in api and '"ms_jx_use_float_menu"' in api)
chk("compile_check tool present", os.path.exists(os.path.join(ROOT, "devtools/compile_check.py")))
print(f"\n{sum(res)}/{len(res)} static checks passed"); sys.exit(0 if all(res) else 1)
