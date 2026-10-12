#!/usr/bin/env python3
"""Step 37 regression checks for real setting mappings and guarded HTTP APIs."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
api_path = ROOT / "talkback/src/main/java/com/google/android/accessibility/talkback/editor/LuaServiceApi.java"
extras_path = ROOT / "talkback/src/main/java/com/google/android/accessibility/talkback/editor/MsExtras.java"
http_path = ROOT / "talkback/src/main/assets/lua/http.lua"
api = api_path.read_text()
extras = extras_path.read_text()
http = http_path.read_text()

# Real feature mappings must use TalkBack's actual preference store, not the separate
# compatibility-value store. This caught a bug where mapped settings were saved in the wrong file.
assert 'SharedPreferencesUtils.getSharedPreferences(ctx)' in api
mapping = api.split('// Apply only settings with an existing MobinSR behavior.', 1)[1].split('/** Returns the stored compatibility value', 1)[0]
for token in [
    'MsExtras.K_AUTO_OCR', 'MsExtras.K_NO_EDGE', 'MsExtras.K_NO_MULTI',
    'MsExtras.K_LOG', 'MsExtras.K_AUTO_TRANS', '"ms_cmd_disable_hotkey"',
    '"ms_async_tts_accessibility_volume"', 'MsExtras.setEarpieceMode(ctx, enabled)',
]:
    assert token in mapping, f'missing live mapping: {token}'
assert 'p.edit().putBoolean(MsExtras.K_AUTO_OCR' not in mapping, 'feature preference must not use compatibility prefs'

# Earpiece command and setter now share one implementation, avoiding toggle-only behavior in API calls.
assert 'static boolean setEarpieceMode(Context c, boolean enabled)' in extras
assert 'if (!setEarpieceMode(c, now))' in extras
assert 'MsExtras.setEarpieceMode(ctx, arg0)' in api

# HTTP public entry points catch URL/network/file exceptions and preserve normal return shapes.
for name in ('get', 'post', 'download', 'upload'):
    assert re.search(r'function _M\.' + name + r'\(', http), f'missing http.{name}'
assert 'pcall(getImpl, url, cookie)' in http
assert 'pcall(postImpl, url, data, cookie)' in http
assert 'pcall(downloadImpl, url, path, cookie)' in http
assert 'pcall(uploadImpl, url, fields, files, cookie)' in http
assert 'conn:disconnect()' in http

# Confirm the setter compatibility surface has not accidentally shrunk or duplicated names.
section = api.split('Jieshuo setter compatibility (Step 36)', 1)[1].split('// ---------------------------------------------------------------- LuaContext', 1)[0]
methods = re.findall(r'public void (set\w+)\(([^)]*)\)', section)
sigs = [(name, re.sub(r'\s+', ' ', args.strip())) for name, args in methods]
assert len(sigs) == 226, f'expected 226 generated signatures, got {len(sigs)}'
assert len(set(sigs)) == len(sigs), 'duplicate generated setter signature'
print('PASS: real setters use TalkBack preference storage')
print('PASS: OCR, edge, multi-touch, accessibility volume, logging, translation, hotkey and earpiece mappings present')
print('PASS: earpiece setter and command share one direct-mode implementation')
print('PASS: HTTP get/post/download/upload guard runtime errors')
print(f'PASS: {len(sigs)} generated setter signatures retained without duplicates')
print('LIMITATION: static checks only; no Android build, Lua runtime, or device test')
