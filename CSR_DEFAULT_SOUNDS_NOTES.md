# CSR with MK as default sounds

- Sounds are in `talkback/src/main/assets/csr_default_sounds/` with `mapping.json` (slot -> file).
- Mapping comes from the user's Jieshuo "CSR with MK" scheme config. Jieshuo names are renamed to the
  app's slot keys (focus1->focus, focus2->focus_actionable, focus3->keyboard_focus, long_click->long_press,
  scroll_up->chime_up, scroll_down->chime_down, gesture_start->gesture_begin, has_link->hyperlink).
- `SoundThemeManager.seedCsrDefaultOnce()` fills the Default theme once (first run). Slots the user already
  assigned are kept; slots not in the mapping keep the built-in sound.
- Not included on purpose: Clock folder (hour/minute/hourly sounds), extensions, tools, API keys, gestures.
