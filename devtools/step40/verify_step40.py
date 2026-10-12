#!/usr/bin/env python3
"""Step 40 static checks: gesture speed options. Static only: no build, no phone test."""
import os, re, sys
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
def rd(p):
    with open(os.path.join(ROOT, p), encoding="utf-8", errors="replace") as f: return f.read()
U = "utils/src/main/java/com/google/android/accessibility/utils/gestures/"
T = "talkback/src/main/java/com/google/android/accessibility/talkback/"
res = []
def chk(n, ok): res.append(bool(ok)); print(("PASS: " if ok else "FAIL: ") + n)

gc = rd(U + "GestureConfiguration.java")
chk("GestureConfiguration: tunable swipe cm and hold percent, defaults unchanged",
    "swipeConfirmCm = -1f" in gc and "holdPercent = 100" in gc and "longPressTimeoutMs()" in gc)
chk("hold gestures use the scaled hold time", "completeAfter(GestureConfiguration.longPressTimeoutMs()" in rd(U + "GestureMatcher.java"))
chk("Swipe uses the tunable distance", "GestureConfiguration.swipeConfirmCm > 0f" in rd(U + "Swipe.java"))
gm = rd(U + "GestureManifold.java")
chk("GestureManifold loads both preferences before creating matchers",
    gm.index("loadMsGestureSpeed(context);") < gm.index("getGestureMatcherList("))
tm = rd(T + "editor/MsGestureTiming.java")
chk("long-click margin and scroll delay defaults are the faster values (80 / 400)", "80, 30, 400" in tm and "400, 150, 1000" in tm)
chk("no hard-coded long-click +150L left", all("getLongPressTimeout() + 150L" not in rd(T + "editor/" + f) for f in ("LuaServiceApi.java", "JxCommands.java")))
chk("scroll read delay uses the preference", "scrollReadDelayMs(s)" in rd(T + "MsReadingExtras.java"))
xml = rd("talkback/src/main/res/xml/ms_jx_op_default_gesture_setting.xml")
arr = rd("talkback/src/main/res/values/ms_jx_arrays.xml")
for k in ("ms_gesture_swipe_cm", "ms_gesture_hold_percent", "ms_gesture_long_click_extra", "ms_scroll_read_delay"):
    chk(f"setting {k} present once", xml.count(f'android:key="{k}"') == 1)
for a in set(re.findall(r'@array/(ms_gesture_[a-z_]+|ms_scroll_[a-z_]+)', xml)):
    chk(f"array {a} defined", f'name="{a}"' in arr)
for k, v in (("ms_gesture_swipe_cm", "0.6"), ("ms_gesture_hold_percent", "65"), ("ms_gesture_long_click_extra", "80"), ("ms_scroll_read_delay", "400")):
    chk(f"{k} default is {v}", re.search(r'android:key="%s"[^>]*android:defaultValue="%s"' % (k, re.escape(v)), xml) is not None)
print(f"\n{sum(res)}/{len(res)} static checks passed"); sys.exit(0 if all(res) else 1)
