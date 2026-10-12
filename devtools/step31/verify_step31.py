#!/usr/bin/env python3
"""Static checks for MobinSR Step 31; does not compile or execute Android/Lua code."""
from pathlib import Path
import sys
root = Path(__file__).resolve().parents[2]
http = (root/'talkback/src/main/assets/lua/http.lua').read_text()
imp = (root/'talkback/src/main/assets/lua/import.lua').read_text()
checks = {
    'HTTP module does not require LuaSocket': 'require("socket")' not in http,
    'HTTP module exposes get/post/download/upload': all(('function _M.'+n+'(') in http for n in ('get','post','download','upload')),
    'HTTP module uses Java URLConnection': 'java.net.URL' in http and 'openConnection' in http,
    'import guards optional AndroLua classes': all(x in imp for x in ('pcall(luajava.bindClass, "com.androlua.LuaAsyncTask")','pcall(luajava.bindClass, "com.androlua.LuaThread")','pcall(luajava.bindClass, "com.androlua.LuaTimer")')),
    'Jieshuo smali references are present': all((root/'reference/jieshuo_smali/com/androlua'/f'{x}.smali').is_file() for x in ('LuaAsyncTask','LuaThread','LuaTimer','LuaTimerTask','Http','Download','Ticker')),
    'Signing handoff file remains present': (root/'HANDOFF.md').is_file(),
}
for label, ok in checks.items():
    print(('PASS' if ok else 'FAIL') + ': ' + label)
print(f'{sum(checks.values())}/{len(checks)} static checks passed')
sys.exit(0 if all(checks.values()) else 1)
