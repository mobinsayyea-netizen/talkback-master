#!/usr/bin/env python3
"""Step 39 static checks (run from anywhere). Static only: does not prove the APK builds."""
import os, re, sys, glob
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
E = "talkback/src/main/java/com/google/android/accessibility/talkback/editor"
def rd(p):
    with open(os.path.join(ROOT, p), encoding="utf-8", errors="replace") as f: return f.read()
res = []
def chk(n, ok): res.append(bool(ok)); print(("PASS: " if ok else "FAIL: ") + n)

jc, jt, me = rd(f"{E}/JxCommands.java"), rd(f"{E}/JxCommandTable.java"), rd(f"{E}/MsExtras.java")
nav = rd(f"{E}/MsNavCommands.java")
tab = jc[jc.index("TABLE = {"):jc.index("};", jc.index("TABLE = {"))]
ids = re.findall(r'\{"([a-z0-9_]+)",\s*"', tab)
handled = set(re.findall(r'case "([a-z0-9_]+)"', jc[jc.index("private static boolean dispatch"):]))
for t in (jt, me, nav):
    handled |= set(re.findall(r'put\("([a-z0-9_]+)"', t))
    for blk in re.findall(r'new String\[\]\s*\{([^}]*)\}', t):
        handled |= set(re.findall(r'"([a-z0-9_]+)"', blk))
missing = [i for i in ids if i not in handled]
chk(f"every Jieshuo command id has a handler (missing: {missing})", not missing)
chk("MsNavCommands registered in JxCommandTable", "MsNavCommands.register(MAP);" in jt)
chk("no framework-only getRoleDescription() call", ".getRoleDescription()" not in nav)
chk("ACCESS_WIFI_STATE declared", "ACCESS_WIFI_STATE" in rd("talkback/src/main/AndroidManifest.xml"))
chk("segmentation test present", os.path.exists(os.path.join(ROOT, "devtools/step39/SegmentationTest.java")))
print(f"\n{sum(res)}/{len(res)} static checks passed"); sys.exit(0 if all(res) else 1)
