# Step 43 — Restore from a Jieshuo backup, part 1: sounds + clipboard (2026-10-13)

## Done
- New `backup/JieshuoRestore.java` + `backup/JieshuoRestoreActivity.java`. Reached from
  Advanced settings > Backup and restore > "Restore from Jieshuo backup" (registered in the manifest,
  row added to `res/xml/ms_jx_adv_backup_and_restore.xml`).
- The person picks the zip of the Jieshuo folder. Only `Sounds/**`, the clipboard `.dat` and the
  settings `.dat` are read (the settings file only for the one key `sound_package`). Nothing is bundled
  in the APK. Keys, tokens and passwords are never read. Zip path tricks (`..`) are skipped.
- Every Jieshuo sound package becomes a sound theme (same name, trimmed). All its audio files go to the theme
  pool and each event in the package `config` is assigned the same file. The theme named in
  `sound_package` is switched on. Importing twice replaces the theme instead of duplicating it.
- Name changes between Jieshuo and MS slots: focus1->focus, focus2->focus_actionable, focus3->keyboard_focus,
  long_click->long_press, scroll_up->chime_up, scroll_down->chime_down, gesture_start->gesture_begin,
  has_link->hyperlink. `value_default` entries (edit_box, seek_bar) keep MS's default sound.
- The package's `Clock/` folder (spoken hour/minute clips) is copied into the theme folder; MS has no spoken
  clock feature yet, so nothing plays it.
- Clipboard `.dat` (Java ArrayList of strings) is read with a whitelist-only ObjectInputStream (lists, maps,
  sets, numbers) and added to `ClipboardStore`, first item stays on top.
- `SoundThemeManager` got `createEmptyTheme`, `addPoolFileFromFile`, `themeFolder`.

## Verified (on the real backup, on a computer)
- Whitelist reader parses the real settings file (426 keys, `sound_package` = "CSR with MK", `sound_volume` = 60)
  and the real clipboard file (31 items).
- Entry selection on the real zip: 316 sound files, 1 clipboard file, 1 settings file picked; a `..` path is rejected.
- Mapping simulation: 47 events assigned for "CSR with MK", 0 without an MS event, 0 missing files.
- `JieshuoRestore.java` compiles against small stubs of the Android classes.

## NOT verified
- NOT compiled with the real Android SDK and NOT run on a phone. Run the GitHub Actions build first.
  `JieshuoRestoreActivity` and the manifest/xml rows are untested.
- Sound volume (Jieshuo `sound_volume` = 60) is not restored: no matching MS setting was found yet.

## Next (agreed order)
- Step 2: labels, Lua files (GeminiUI_Logic.lua, Tools, Aliases), favorites (without keys/tokens).
- Step 3: settings + gestures mapping (426 Jieshuo keys; list of keys with no MS match shown after restore),
  sound volume.
