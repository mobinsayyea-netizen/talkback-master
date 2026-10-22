package ms.api;

import android.view.KeyEvent;

/**
 * Base class of every AI-made Java extension or tool. The plugin class is named Main and extends
 * this class. Override only what you need. Java 7 syntax only (no lambdas).
 *
 * <p>run() is called from the menu. onEvent() is called for events listed in the header line
 * "-- event:" (window_changed, app_opened, notification, service_started). The other methods are
 * hooks, called only when their name is listed in the header line "-- hook:", exactly like the
 * Lua hooks of Jieshuo.
 */
public abstract class MsPlugin {
  /** Run from the menu. text = text at the focus. */
  public void run(MsApi api, String text) {}

  /** Event run. */
  public void onEvent(MsApi api, String type, String pkg, String text) {}

  public void onWindowStateChanged(MsApi api, String pkg, String text) {}

  public void onNotification(MsApi api, String pkg, String text) {}

  public void onScrolled(MsApi api, String pkg, String text) {}

  public void onViewLongClicked(MsApi api, String pkg, String text) {}

  /** Return true to consume the key. */
  public boolean onKeyEvent(MsApi api, KeyEvent event) {
    return false;
  }

  /** Return true to consume the gesture. Must return within 400 ms. */
  public boolean onGestureDetected(MsApi api, int gestureId) {
    return false;
  }

  public void onVolumeChanged(MsApi api, int streamType, int value, int previous, int percent) {}
}
