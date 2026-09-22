package com.google.android.accessibility.talkback.soundtheme;

/** One sound TalkBack plays for a specific occasion, e.g. a click or gaining focus. */
public final class SoundSlot {
  /** Stable key saved to disk — never rename an existing one, only add new ones. */
  public final String key;
  /** Name spoken and shown to the user. */
  public final String label;
  /** The raw resource id TalkBack plays for this occasion when nothing overrides it. */
  public final int defaultResId;

  public SoundSlot(String key, String label, int defaultResId) {
    this.key = key;
    this.label = label;
    this.defaultResId = defaultResId;
  }
}
