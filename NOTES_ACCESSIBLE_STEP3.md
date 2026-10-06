# Accessible MobinSR — progress notes (2026-10-04)

**Read NEXT_STEPS_ACCESSIBLE.md for the full plan (what is left, how to add, how to work).**

**हिंदी सार:** यह zip17 रेपो + execute() कमांड का पहला समूह है। नीचे लिखा है क्या हुआ, क्या नहीं हुआ, और अगला कदम।
Nothing here was compiled or run on a phone. Build on GitHub Actions and send the error log.

## Base
- MSScreenReader_zip17.zip (repo from the other account) + step 3b changes. Folder name unchanged.
- Settings work (zips 1-12) is all inside: see MS_SETTINGS_NOTES.md. Lua/IDE status: LUA_IDE_PROGRESS.md. Handoff: HANDOFF.md.

## Done in this session
- service.execute(): 72 of 173 Jieshuo commands now implemented (was 54).
  - editor/JxCommands.java : default branch now calls JxCommandsNav.
  - editor/JxCommandsNav.java (new): next/previous heading, link, control (Feedback.focusDirection + setHtmlTargetType); next/previous window; next/previous character, word, sentence(line), paragraph (ACTION_*_AT_MOVEMENT_GRANULARITY); Clear clipboard; Speak network info.
- editor/JxCommandTable.java (new): one line per command (id -> lambda). Added: speech faster/slower, select TTS engine (opens system TTS settings), spell current focus, curtain (dim screen), translate and OCR of focused item (same code as main menu), video description, automatic browsing (read from focus), speak current lighting.
- TalkBackService: new helpers msChangeSpeechRate, msDimScreen, msStartVideoDescription.
- LuaServiceApi: new asyncSpeak(text), getEditText(), insertText(node,text), playSoundTick(), and global `this` (= service) for Jieshuo-style scripts.
- Remaining names: see REMAINING_COMMANDS.txt (they return false, never throw). To add one, add a line in JxCommandTable.

## User's example scripts: what is still missing
- thread(fn,...), task(ms,fn): not built. Needs the Lua state to stay alive while tasks are pending (now one fresh state per run, closed after).
- LuaDialog, SingleLineAdapter, global `dialog`/UI loadlayout with a service context: not built.
- Missing `then` after `if service.click({...})` in the pasted scripts is a Lua syntax error; add `then`.
- Suspend commands (touch / feedback / hotkeys / edge gestures / TTS engines): TalkBack source here has no real suspend, MsOperationController 'suspend' only stops speech. Needs design.

## NOT done / unverified
- Heading/link/control navigation relies on Actors.java honouring htmlTargetType for native nodes: check on phone.
- 101 commands still missing. Planned groups: (1) suspend/switch commands (touch, feedback, hotkeys, curtain, edge gestures, TTS engines, speech rate, multi-finger), (2) OCR / translation / picture description / virtual screen / captions, (3) menus (main, recognition, functions, actions, clipboard history, favorites), (4) modes (char, list, node, quick, reading, grid, selection, auto next), (5) location, light, timer, cloud clipboard, chat history.
- Step 2 files (AutoClickParser/Step/Runner) are NOT merged: this repo already has JxClickScript/JxClickEnv/JxCommands from a parallel line. Decide which click engine to keep before merging. Step 2's ParserTest passed 28 checks on a computer.
- Auto-click gaps (from step 2 notes): colour search [x1,y1,x2,y2,r,g,b,d], custom # labels, "Actions" menu command, step-builder UI, call to this.speak(...) inside {{">Files", ...}}.
- Settings still saved-only: see MS_SETTINGS_NOTES.md (fast node, auto page scroll, auto window switch, most Clicking options, cloud labels).
- Lua engine (NDK) and everything since never compiled: first GitHub Actions build + error fixing is still step 7.

## Rules from the user
- Reply Hindi, short. Do not touch key/package/signing. Online features (translation, image recognition) must keep working. All Jieshuo/Lua service.* names must match Jieshuo.
