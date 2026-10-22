# Step 54 notes: AI Create

## Files (new)
- `ms/api/MsApi.java`, `MsPlugin.java` (plugin-facing API; copies also in `assets/ms_compiler/`, refreshed by the workflow)
- `editor/MsApiImpl.java` (delegates to LuaServiceApi), `JavaPluginRunner.java`, `PluginCompiler.java`, `GeminiClient.java`
- `editor/AiCreateActivity.java`, `AiChatActivity.java`, `ApkBuilderActivity.java`
- `assets/ms_compiler/API.txt` (API list sent to Gemini), `assets/apk_builder.html`

## Files (changed)
- `LuaServiceApi.java`: constructor and finishMain are package-private; run/runHook/runHookBoolean check `JavaPluginRunner.isJava(script)` first.
- `context_menu.xml`, `ide_strings.xml`, `ContextMenuItemClickProcessor.java`, `AndroidManifest.xml`, `talkback/build.gradle` (ecj 3.12.3, dalvik-dx 9.0.0_r3), `.github/workflows/build.yml` (step "Prepare AI Create compiler files").

## Regenerating MsApi / MsApiImpl
Script idea: parse `  public <type> name(params) {` (two-space indent, instance methods) in LuaServiceApi.java, keep only methods whose types are android.*/java.*/javax.* or java.lang, skip stop/finishMain/run/scheduleTask, write one interface line and one delegate (`lua()` then call, default false/0/""/null) per method. Hand-written extras: folder, node, sleep, focusedText, clipboardText, gemini, get, put, runOnUi, postDelayed.

## Known limits
- Lua-only functions (those taking LuaFunction/LuaTable: timer, task, regEvent, click with a table) are not in the Java API; Java has threads and Runnable instead (`runOnUi`, `postDelayed`).
- Commands that have no feature in MS Screen Reader still return false, same as in Lua.
- Plugins are Java 7 syntax (no lambdas). Locked Lua scripts cannot be converted.
- Not built or run yet.
