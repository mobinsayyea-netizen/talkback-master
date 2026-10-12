# Step 42 — Lua `service` is a real Context; shared data is saved (2026-10-12)

## Done (2 small changes, both in `editor/LuaServiceApi.java`)
1. `LuaServiceApi` now `extends android.content.ContextWrapper` (wrapping the accessibility service) and still
   implements `LuaContext`. Before, `service` was not an Android Context, so scripts that pass it where Android needs one
   (`LinearLayout(ctx)`, `Button(ctx)`, `AlertDialog.Builder(service)`, `ArrayAdapter(service, ...)`, and the bundled
   `loadlayout.lua`, which uses `activity or service`) were expected to fail at the first view. Methods the class already had
   (`getPackageManager`, `getResources`, `getSystemService`, `startActivity`) keep their behaviour.
2. `service.setSharedData / getSharedData` are now saved in SharedPreferences `lua_shared_data` (strings, booleans, numbers;
   non-integral numbers are stored exactly). Before they lived in a per-script-run in-memory map, so a saved value (for
   example the Gemini API key of the TTS extension) was lost on every run. Tables/Java objects cannot be saved and stay in
   memory for that run only. `setSharedData(key, nil)` deletes the saved value.

## Verified
- `devtools/step42/verify_step42.py` 7/7 static checks (text checks only); verify_step41 still 6/6.

## NOT verified
- NOT compiled: `devtools/compile_check.py` needs internet for android.jar, which was not available here. Run it, then the
  GitHub Actions build, before trusting this step. The one thing to watch in the compiler output: any method in
  LuaServiceApi whose name/return type clashes with a `Context`/`ContextWrapper` method (none found by reading).
- Not tested on a phone. Expected: the Gemini TTS extension and the floating app-launcher extension should get past
  their first view/dialog; the dialog window type (TYPE_ACCESSIBILITY_OVERLAY) and the rest of those scripts are untested.

## Still open (unchanged)
- Native modules (cjson, bson, crypt, regex) are not integrated; 226 `setUseXxx` setters are placeholders; 810 preference
  keys vs ~65 read by code (see STEP41_NOTES.md).
