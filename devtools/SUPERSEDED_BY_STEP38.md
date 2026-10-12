# Superseded checks
Step 38 replaced the small compatibility classes with the real AndroLua_pro classes and removed the cjson.lua shim.
These checks look for markers of the OLD implementation and now fail on purpose; use devtools/step38/verify_step38.py instead:
- step32/verify_step32.py: LuaAsyncTask constructors/callbacks, LuaThread implements LuaMetaTable, LuaTimer APIs, HTTP common facade, Download API
- step34/verify_step34.py: cjson Lua compatibility shim (native cjson replaced it)
- step35/verify_step35.py: the four lifecycle markers (the same behaviour now lives in the ported classes; step38 checks it)
Steps 30, 31, 33, 36, 37 still pass unchanged.
