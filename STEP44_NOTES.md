# Step 44 — Restore from a Jieshuo backup, part 2: labels, aliases, favorites, extensions, tools (2026-10-13)

Builds on STEP43_NOTES.md (same screen: Advanced settings > Backup and restore > "Restore from Jieshuo backup").
Code: `backup/JieshuoRestore.java` (all logic), `backup/JieshuoRestoreActivity.java` (text only changed).

## Done
- **Labels** (`Labels/*.json`): Jieshuo names each file after the app. The app name is matched to an INSTALLED app's
  package name (PackageManager label, case-insensitive; QUERY_ALL_PACKAGES is already in the manifest) and every
  `view id -> spoken name` becomes a line `package:id/view id=spoken name` in MS's custom node aliases
  (`MsNodeAlias.K_TEXT`). "Use custom node aliases" (`K_USE`) is switched on. Lines already in MS are kept.
  Version-named files (`WhatsApp_2.24.8.85.json`) are only used when the app has no plain file (they are subsets).
  Left out and counted: labels keyed by screen position (contain `:` or `;`), and empty labels (Jieshuo's "say nothing").
  Apps that are not installed are listed in the result (first 12 names).
- **Aliases** (`Aliases/*.json`, text -> name): added as text lines (MS matches text in every app).
- **Extensions** (`Plugins/*` -> MS Extensions) and **Tools** (`Tools/*` -> MS Tools): whole folders copied; folder names are
  trimmed of spaces; two folders with the same trimmed name both stay (second gets " 2"); a folder that already existed in
  MS before the restore is kept untouched. Jieshuo's `history.json` files are skipped.
- Also copied (never overwriting): `GeminiUI_Logic.lua`, `GeminiLangConfig.txt` (to the MS base folder), `Resources/`, `Notes/`, `Timer/`.
  `GeminiVisionKey.txt` (an API key) is NOT copied.
- **Favorites** (Jieshuo favorites `.dat`): become clipboard items marked as favorite, added under the newest clipboard items.
  Anything that looks like a key, token or password is dropped: GitHub / Google / OpenAI / AWS key patterns, one long word with
  letters+digits, and short items (<= 600 chars) containing apikey / authDomain / password / secret / token / bearer.
  On the real file: 14 kept, 6 left out.
- The three `.dat` files are staged under ASCII names in the cache folder (no dependence on Chinese file names).

## Verified (on a computer, with stand-ins for the Android classes, on the real backup)
- Whole restore runs: 2 themes (CSR with MK active), 61 sound assignments, 31 clipboard items, 14 favorites (6 skipped),
  39 extensions, 17 tools, 108 other files (Resources 3, Notes 4, Timer 99, Gemini files 2). The key file is not copied.
- Labels logic run with only 5 pretend installed apps: 4 apps matched, 207 aliases written, switch on.
- `JieshuoRestore.java` compiles with stubs.

## NOT verified
- NOT compiled with the real Android SDK and NOT run on a phone (run the GitHub Actions build first).
- How many labels apply depends on which of the apps are installed; an app whose name in Jieshuo differs from its name on
  the phone (e.g. "Package installer") is skipped.
- Jieshuo scripts that call Jieshuo-only functions may still fail in MS (Lua gaps listed in STEP41/42 notes).
- Gemini API key must be entered again.

## Next
- Step 3 (agreed): the 426 Jieshuo settings + gestures (`Gestures/*/config`) with a list of what had no MS match; sound volume (60).
- Not restored on purpose: `Download/` (user files), `fonts/`, `Backups/*.cbk` (older Jieshuo clipboard backups, 1.6-1.9 MB each,
  bigger than the current clipboard) — can be added if wanted.
