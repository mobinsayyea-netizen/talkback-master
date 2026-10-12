# Lua module compatibility status (Step 34)

- `cjson.lua`: common `encode`, `decode`, `null`, `safe_encode`, `safe_decode`, and `new` calls are bridged to the existing bundled JSON4Lua module.
- This is a **Lua compatibility shim**, not the native lua-cjson implementation. Native cjson configuration setters are accepted as no-ops and do not provide native tuning semantics.
- `crypt`, `regex`, and `bson` native modules are not supplied by this shim. Do not assume they are available until their licensed source is integrated and built for every supported ABI.
- `require("cjson")` resolves through the existing `package.path` because the runtime copies files from `assets/lua` to its Lua module directory.
