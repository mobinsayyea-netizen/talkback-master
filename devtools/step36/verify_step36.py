#!/usr/bin/env python3
"""Step 36 source-level checks for bulk Jieshuo setter API compatibility."""
from pathlib import Path
import re
root = Path(__file__).resolve().parents[2]
api = (root / "talkback/src/main/java/com/google/android/accessibility/talkback/editor/LuaServiceApi.java").read_text()
ref = (root / "JX_SERVICE_SIGNATURES.txt").read_text()
assert "jx_compat_settings" in api
assert 'MsExtras.K_AUTO_OCR' in api and 'MsExtras.K_NO_EDGE' in api and 'MsExtras.K_NO_MULTI' in api
# Count the generated scalar setter declarations in the marked Step 36 section.
section = api.split("Jieshuo setter compatibility (Step 36)", 1)[1].split("// ---------------------------------------------------------------- LuaContext", 1)[0]
generated = re.findall(r'public void (set\w+)\(([^)]*)\)', section)
assert len(generated) == 226, f"expected 226 generated setters, found {len(generated)}"
assert sum(1 for n, a in generated if n.startswith("setUse") and a.strip() == "boolean arg0") == 112
# Signature reference check: every generated method name exists in the reference.
ref_names = set(re.findall(r'\.method public (set\w+)\(', ref))
assert all(n in ref_names for n, _ in generated)
print(f"PASS: {len(generated)} compatibility setter overloads generated")
print("PASS: setUseXxx(boolean) surface and four confirmed preference mappings present")
print("LIMITATION: stored-only setters do not implement unsupported Jieshuo features; no Android build/device test here")
