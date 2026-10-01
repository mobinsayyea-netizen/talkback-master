package com.google.android.accessibility.talkback.soundtheme;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import java.util.List;

/**
 * Plays the theme sound for occasions that have no built-in TalkBack sound (Jieshuo's extra sound
 * events): toast, dialog, edit box / check box / seek bar / progress focus, scrolling to the top
 * or bottom, keyboard shown/hidden, charger removed, battery low, unlock, volume up/down, copy,
 * paste, go back, screenshot, OCR, translation, and the screen reader turning on/off/pausing.
 * An occasion with no sound assigned (and no default) stays silent. Apps ticked in "Apps where
 * additional sound effects are not used" are skipped while they are in front.
 */
public final class ExtraEventSounds extends BroadcastReceiver {

  private static final String VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION";
  private static final String EXTRA_VOLUME_VALUE = "android.media.EXTRA_VOLUME_STREAM_VALUE";
  private static final String EXTRA_PREV_VOLUME_VALUE =
      "android.media.EXTRA_PREV_VOLUME_STREAM_VALUE";
  private static final int FIRST_APP_ACTION_ID = 0x7f000000;

  private static volatile @Nullable ExtraEventSounds instance;
  private static volatile @Nullable String currentPackage;

  private final Context context;
  private final SoundThemeManager manager;
  private boolean started;
  private boolean keyboardShown;
  private long lastScrollSoundMs;

  public ExtraEventSounds(Context context, SoundThemeManager manager) {
    this.context = context.getApplicationContext();
    this.manager = manager;
  }

  /** Plays a slot's sound from anywhere in the app; does nothing if the service is not running. */
  public static void fire(String slotKey) {
    ExtraEventSounds sounds = instance;
    if (sounds != null) {
      sounds.play(slotKey);
    }
  }

  /** Starts listening and plays the "turned on" sound. */
  public void start() {
    if (started) {
      return;
    }
    instance = this;
    IntentFilter filter = new IntentFilter();
    filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
    filter.addAction(Intent.ACTION_BATTERY_LOW);
    filter.addAction(Intent.ACTION_USER_PRESENT);
    filter.addAction(VOLUME_CHANGED_ACTION);
    ContextCompat.registerReceiver(context, this, filter, ContextCompat.RECEIVER_EXPORTED);
    started = true;
    play("talkman_start");
  }

  /** Plays the "turned off" sound and stops listening. */
  public void stop() {
    if (!started) {
      return;
    }
    play("talkman_stop");
    try {
      context.unregisterReceiver(this);
    } catch (IllegalArgumentException ignored) {
      // Already unregistered.
    }
    started = false;
    if (instance == this) {
      instance = null;
    }
  }

  /** Plays a slot's sound by key; silent if nothing is assigned. */
  public void play(String slotKey) {
    manager.playSlot(slotKey, currentPackage);
  }

  /** Looks at every accessibility event and plays the matching extra sound, if any. */
  public void onAccessibilityEvent(AccessibilityService service, AccessibilityEvent event) {
    if (event == null || !started) {
      return;
    }
    try {
      handleEvent(service, event);
    } catch (RuntimeException e) {
      // A sound problem must never break the screen reader.
    }
  }

  private void handleEvent(AccessibilityService service, AccessibilityEvent event) {
    CharSequence cls = event.getClassName();
    String className = cls == null ? "" : cls.toString();
    switch (event.getEventType()) {
      case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
        CharSequence pkg = event.getPackageName();
        if (pkg != null && !"android".equals(pkg.toString())) {
          currentPackage = pkg.toString();
        }
        if (className.contains("Dialog")) {
          play("dialog");
        }
        break;
      case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
        if (className.contains("Toast")) {
          play("toast");
        }
        break;
      case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED:
        playForFocusedView(event, className);
        break;
      case AccessibilityEvent.TYPE_VIEW_SCROLLED:
        playForScroll(event);
        break;
      case AccessibilityEvent.TYPE_WINDOWS_CHANGED:
        checkKeyboard(service);
        break;
      default:
        break;
    }
  }

  private void playForFocusedView(AccessibilityEvent event, String className) {
    if (className.contains("EditText")) {
      play("edit_box");
    } else if (className.contains("CheckBox")
        || className.contains("Switch")
        || className.contains("ToggleButton")
        || className.contains("RadioButton")
        || className.contains("CompoundButton")) {
      play("check_box");
    } else if (className.contains("SeekBar")) {
      play("seek_bar");
    } else if (className.contains("ProgressBar")) {
      play("progress");
    }
    AccessibilityNodeInfo source = event.getSource();
    if (source != null) {
      try {
        boolean hasCustomAction = false;
        for (AccessibilityNodeInfo.AccessibilityAction action : source.getActionList()) {
          if (action.getId() >= FIRST_APP_ACTION_ID) {
            hasCustomAction = true;
            break;
          }
        }
        if (hasCustomAction) {
          play("has_action");
        }
      } finally {
        source.recycle();
      }
    }
  }

  private void playForScroll(AccessibilityEvent event) {
    long now = android.os.SystemClock.uptimeMillis();
    if (now - lastScrollSoundMs < 400) {
      return;
    }
    boolean top = false;
    boolean bottom = false;
    if (event.getMaxScrollY() > 0 || event.getMaxScrollX() > 0) {
      top = event.getScrollY() <= 0 && event.getScrollX() <= 0;
      bottom =
          (event.getMaxScrollY() > 0 && event.getScrollY() >= event.getMaxScrollY())
              || (event.getMaxScrollX() > 0 && event.getScrollX() >= event.getMaxScrollX());
    } else if (event.getItemCount() > 0) {
      top = event.getFromIndex() <= 0;
      bottom = event.getToIndex() >= event.getItemCount() - 1;
    }
    lastScrollSoundMs = now;
    if (top) {
      play("scroll_top");
    } else if (bottom) {
      play("scroll_bottom");
    } else {
      play("scroll_page");
    }
  }

  private void checkKeyboard(AccessibilityService service) {
    boolean shown = false;
    List<AccessibilityWindowInfo> windows = service.getWindows();
    if (windows != null) {
      for (AccessibilityWindowInfo window : windows) {
        if (window.getType() == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
          shown = true;
        }
        window.recycle();
      }
    }
    if (shown != keyboardShown) {
      keyboardShown = shown;
      play(shown ? "inputmethod_show" : "inputmethod_hide");
    }
  }

  @Override
  public void onReceive(Context ctx, Intent intent) {
    if (intent == null || intent.getAction() == null) {
      return;
    }
    switch (intent.getAction()) {
      case Intent.ACTION_POWER_DISCONNECTED:
        play("power_disconnected");
        break;
      case Intent.ACTION_BATTERY_LOW:
        play("power_low");
        break;
      case Intent.ACTION_USER_PRESENT:
        play("unlock");
        break;
      case VOLUME_CHANGED_ACTION:
        int now = intent.getIntExtra(EXTRA_VOLUME_VALUE, -1);
        int prev = intent.getIntExtra(EXTRA_PREV_VOLUME_VALUE, -1);
        if (now >= 0 && prev >= 0 && now != prev) {
          play(now > prev ? "raise_volume" : "lower_volume");
        }
        break;
      default:
        break;
    }
  }
}
