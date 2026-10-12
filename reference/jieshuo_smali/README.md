# Jieshuo decompiled smali reference (not compiled into MobinSR)

Extracted from the user-provided `cSr.zip` only to inspect API signatures and dependencies while porting the Lua runtime. These `.smali` files are reference material, not Java source and are not included in the Android build. Do not copy them into `src/main/java` or treat their presence as an implementation.

Included classes: `LuaAsyncTask`, `LuaThread`, `LuaTimer`, `LuaTimerTask`, `Http`, `Download`, and `Ticker`.

Next porting steps: compare constructor/method descriptors with `talkback/src/main/assets/lua/import.lua`, port against MobinSR's `LuaContext` (no `LuaActivity`), and verify Android SDK/Java compatibility in GitHub Actions. Upstream Java source is available at https://github.com/nirenr/AndroLua_pro; preserve its applicable licenses when porting.
