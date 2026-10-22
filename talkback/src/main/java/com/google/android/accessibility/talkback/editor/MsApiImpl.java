package com.google.android.accessibility.talkback.editor;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.HashMap;
import java.util.List;
import java.io.File;
import ms.api.MsApi;

/** Implementation of the Java plugin API: every call goes to the same service the Lua scripts use. */
final class MsApiImpl implements MsApi {
  private final Context ctx;
  private final File folder;
  private final AccessibilityNodeInfo node;
  private LuaServiceApi lua;
  private boolean luaFailed;
  private final android.os.Handler ui = new android.os.Handler(android.os.Looper.getMainLooper());

  MsApiImpl(Context ctx, File folder, AccessibilityNodeInfo node) {
    this.ctx = ctx.getApplicationContext();
    this.folder = folder;
    this.node = node;
  }

  /** Called once the plugin has finished. */
  void finish() {
    LuaServiceApi l;
    synchronized (this) {
      l = lua;
      lua = null;
      luaFailed = true;
    }
    if (l != null) {
      try {
        l.finishMain();
      } catch (Throwable ignored) {
      }
    }
  }

  private synchronized LuaServiceApi lua() {
    if (lua == null && !luaFailed) {
      try {
        lua = new LuaServiceApi(ctx, folder);
      } catch (Throwable t) {
        luaFailed = true;
        com.google.android.accessibility.talkback.MsErrorLog.log("Java plugin helper", t);
      }
    }
    return lua;
  }

  @Override
  public File folder() {
    return folder;
  }

  @Override
  public AccessibilityNodeInfo node() {
    return node;
  }

  @Override
  public void sleep(int ms) {
    try {
      Thread.sleep(Math.max(0, Math.min(ms, 60000)));
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public String focusedText() {
    LuaServiceApi l = lua();
    if (l == null) return "";
    AccessibilityNodeInfo n = l.getFocusView();
    String t = n == null ? null : l.getText(n);
    return t == null ? "" : t;
  }

  @Override
  public String clipboardText() {
    try {
      ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
      ClipData d = cm.getPrimaryClip();
      if (d != null && d.getItemCount() > 0) {
        CharSequence c = d.getItemAt(0).coerceToText(ctx);
        return c == null ? "" : c.toString();
      }
    } catch (Throwable ignored) {
    }
    return "";
  }

  @Override
  public String gemini(String prompt) {
    try {
      return GeminiClient.ask(ctx, "", prompt);
    } catch (Exception e) {
      return "Gemini error: " + e.getMessage();
    }
  }

  private SharedPreferences prefs() {
    return ctx.getSharedPreferences("ms_plugin_" + folder.getName(), Context.MODE_PRIVATE);
  }

  @Override
  public String get(String key) {
    return prefs().getString(key, "");
  }

  @Override
  public void put(String key, String value) {
    prefs().edit().putString(key, value).apply();
  }

  @Override
  public void runOnUi(Runnable r) {
    ui.post(r);
  }

  @Override
  public void postDelayed(Runnable r, long ms) {
    ui.postDelayed(r, ms);
  }

  // ---- generated delegates to the Jieshuo service (LuaServiceApi) ----

  @Override
  public String getAllText() {
    LuaServiceApi l = lua();
    return l == null ? "" : l.getAllText();
  }

  @Override
  public String getAllText(int ignored) {
    LuaServiceApi l = lua();
    return l == null ? "" : l.getAllText(ignored);
  }

  @Override
  public AccessibilityNodeInfo getFocusView() {
    LuaServiceApi l = lua();
    return l == null ? null : l.getFocusView();
  }

  @Override
  public String getNodeInfoLabel(AccessibilityNodeInfo n) {
    LuaServiceApi l = lua();
    return l == null ? "" : l.getNodeInfoLabel(n);
  }

  @Override
  public AccessibilityNodeInfo findAccessibilityNodeInfoByText(String text) {
    LuaServiceApi l = lua();
    return l == null ? null : l.findAccessibilityNodeInfoByText(text);
  }

  @Override
  public AccessibilityNodeInfo findAccessibilityNodeInfoByText(String text, int index) {
    LuaServiceApi l = lua();
    return l == null ? null : l.findAccessibilityNodeInfoByText(text, index);
  }

  @Override
  public boolean deleteApp(String packageName) {
    LuaServiceApi l = lua();
    return l == null ? false : l.deleteApp(packageName);
  }

  @Override
  public boolean installApp(String path) {
    LuaServiceApi l = lua();
    return l == null ? false : l.installApp(path);
  }

  @Override
  public boolean performGlobalAction(int action) {
    LuaServiceApi l = lua();
    return l == null ? false : l.performGlobalAction(action);
  }

  @Override
  public AccessibilityNodeInfo getRootInActiveWindow() {
    LuaServiceApi l = lua();
    return l == null ? null : l.getRootInActiveWindow();
  }

  @Override
  public List<android.view.accessibility.AccessibilityWindowInfo> getWindows() {
    LuaServiceApi l = lua();
    return l == null ? null : l.getWindows();
  }

  @Override
  public android.accessibilityservice.AccessibilityServiceInfo getServiceInfo() {
    LuaServiceApi l = lua();
    return l == null ? null : l.getServiceInfo();
  }

  @Override
  public void setServiceInfo(android.accessibilityservice.AccessibilityServiceInfo info) {
    LuaServiceApi l = lua();
    if (l != null) l.setServiceInfo(info);
  }

  @Override
  public void startActivity(Intent intent) {
    LuaServiceApi l = lua();
    if (l != null) l.startActivity(intent);
  }

  @Override
  public void startActivity(String action) {
    LuaServiceApi l = lua();
    if (l != null) l.startActivity(action);
  }

  @Override
  public Context getApplicationContext() {
    LuaServiceApi l = lua();
    return l == null ? null : l.getApplicationContext();
  }

  @Override
  public String getPackageName() {
    LuaServiceApi l = lua();
    return l == null ? "" : l.getPackageName();
  }

  @Override
  public PackageManager getPackageManager() {
    LuaServiceApi l = lua();
    return l == null ? null : l.getPackageManager();
  }

  @Override
  public android.content.res.Resources getResources() {
    LuaServiceApi l = lua();
    return l == null ? null : l.getResources();
  }

  @Override
  public Object getSystemService(String name) {
    LuaServiceApi l = lua();
    return l == null ? null : l.getSystemService(name);
  }

  @Override
  public android.content.ContentResolver getContentResolver() {
    LuaServiceApi l = lua();
    return l == null ? null : l.getContentResolver();
  }

  @Override
  public boolean execute(String name) {
    LuaServiceApi l = lua();
    return l == null ? false : l.execute(name);
  }

  @Override
  public boolean click(String text) {
    LuaServiceApi l = lua();
    return l == null ? false : l.click(text);
  }

  @Override
  public boolean startApp(String labelOrPackage) {
    LuaServiceApi l = lua();
    return l == null ? false : l.startApp(labelOrPackage);
  }

  @Override
  public boolean plugin(String name, Object node) {
    LuaServiceApi l = lua();
    return l == null ? false : l.plugin(name, node);
  }

  @Override
  public boolean tool(String name, Object node) {
    LuaServiceApi l = lua();
    return l == null ? false : l.tool(name, node);
  }

  @Override
  public boolean execute(String name, AccessibilityNodeInfo node) {
    LuaServiceApi l = lua();
    return l == null ? false : l.execute(name, node);
  }

  @Override
  public void postExecute(long ms, String name, AccessibilityNodeInfo node) {
    LuaServiceApi l = lua();
    if (l != null) l.postExecute(ms, name, node);
  }

  @Override
  public String getText(AccessibilityNodeInfo n) {
    LuaServiceApi l = lua();
    return l == null ? "" : l.getText(n);
  }

  @Override
  public boolean setText(AccessibilityNodeInfo n, String text) {
    LuaServiceApi l = lua();
    return l == null ? false : l.setText(n, text);
  }

  @Override
  public boolean setText(String text) {
    LuaServiceApi l = lua();
    return l == null ? false : l.setText(text);
  }

  @Override
  public boolean copy(CharSequence text) {
    LuaServiceApi l = lua();
    return l == null ? false : l.copy(text);
  }

  @Override
  public boolean copy() {
    LuaServiceApi l = lua();
    return l == null ? false : l.copy();
  }

  @Override
  public boolean appendCopy(CharSequence text) {
    LuaServiceApi l = lua();
    return l == null ? false : l.appendCopy(text);
  }

  @Override
  public boolean appendCopy() {
    LuaServiceApi l = lua();
    return l == null ? false : l.appendCopy();
  }

  @Override
  public boolean paste(AccessibilityNodeInfo n) {
    LuaServiceApi l = lua();
    return l == null ? false : l.paste(n);
  }

  @Override
  public boolean paste() {
    LuaServiceApi l = lua();
    return l == null ? false : l.paste();
  }

  @Override
  public boolean paste(CharSequence text) {
    LuaServiceApi l = lua();
    return l == null ? false : l.paste(text);
  }

  @Override
  public boolean paste(AccessibilityNodeInfo n, CharSequence text) {
    LuaServiceApi l = lua();
    return l == null ? false : l.paste(n, text);
  }

  @Override
  public boolean toNext() {
    LuaServiceApi l = lua();
    return l == null ? false : l.toNext();
  }

  @Override
  public boolean toPrevious() {
    LuaServiceApi l = lua();
    return l == null ? false : l.toPrevious();
  }

  @Override
  public boolean toNext(AccessibilityNodeInfo n, boolean withChildren) {
    LuaServiceApi l = lua();
    return l == null ? false : l.toNext(n, withChildren);
  }

  @Override
  public boolean toPrevious(AccessibilityNodeInfo n, boolean withChildren) {
    LuaServiceApi l = lua();
    return l == null ? false : l.toPrevious(n, withChildren);
  }

  @Override
  public boolean toNextChar() {
    LuaServiceApi l = lua();
    return l == null ? false : l.toNextChar();
  }

  @Override
  public boolean toPreviousChar() {
    LuaServiceApi l = lua();
    return l == null ? false : l.toPreviousChar();
  }

  @Override
  public boolean toNextLine() {
    LuaServiceApi l = lua();
    return l == null ? false : l.toNextLine();
  }

  @Override
  public boolean toPreviousLine() {
    LuaServiceApi l = lua();
    return l == null ? false : l.toPreviousLine();
  }

  @Override
  public boolean toTextStart() {
    LuaServiceApi l = lua();
    return l == null ? false : l.toTextStart();
  }

  @Override
  public boolean toTextEnd() {
    LuaServiceApi l = lua();
    return l == null ? false : l.toTextEnd();
  }

  @Override
  public boolean toListViewStart() {
    LuaServiceApi l = lua();
    return l == null ? false : l.toListViewStart();
  }

  @Override
  public boolean toListViewEnd() {
    LuaServiceApi l = lua();
    return l == null ? false : l.toListViewEnd();
  }

  @Override
  public String ocr(AccessibilityNodeInfo node) {
    LuaServiceApi l = lua();
    return l == null ? "" : l.ocr(node);
  }

  @Override
  public void setTimerMode(boolean on) {
    LuaServiceApi l = lua();
    if (l != null) l.setTimerMode(on);
  }

  @Override
  public void setAutoReadEnabled(boolean on) {
    LuaServiceApi l = lua();
    if (l != null) l.setAutoReadEnabled(on);
  }

  @Override
  public void setAutoNext(boolean on) {
    LuaServiceApi l = lua();
    if (l != null) l.setAutoNext(on);
  }

  @Override
  public boolean loadGesturePackage(String name) {
    LuaServiceApi l = lua();
    return l == null ? false : l.loadGesturePackage(name);
  }

  @Override
  public AccessibilityNodeInfo getRootNodeInfo() {
    LuaServiceApi l = lua();
    return l == null ? null : l.getRootNodeInfo();
  }

  @Override
  public boolean isEditView(AccessibilityNodeInfo n) {
    LuaServiceApi l = lua();
    return l == null ? false : l.isEditView(n);
  }

  @Override
  public boolean isSeekBar(AccessibilityNodeInfo n) {
    LuaServiceApi l = lua();
    return l == null ? false : l.isSeekBar(n);
  }

  @Override
  public boolean isListView(AccessibilityNodeInfo n) {
    LuaServiceApi l = lua();
    return l == null ? false : l.isListView(n);
  }

  @Override
  public boolean isInListView(AccessibilityNodeInfo n) {
    LuaServiceApi l = lua();
    return l == null ? false : l.isInListView(n);
  }

  @Override
  public boolean isInWebView(AccessibilityNodeInfo n) {
    LuaServiceApi l = lua();
    return l == null ? false : l.isInWebView(n);
  }

  @Override
  public boolean isClick(AccessibilityNodeInfo n) {
    LuaServiceApi l = lua();
    return l == null ? false : l.isClick(n);
  }

  @Override
  public boolean click(double x, double y) {
    LuaServiceApi l = lua();
    return l == null ? false : l.click(x, y);
  }

  @Override
  public boolean longClick(double x, double y) {
    LuaServiceApi l = lua();
    return l == null ? false : l.longClick(x, y);
  }

  @Override
  public boolean press(double x, double y, int ms) {
    LuaServiceApi l = lua();
    return l == null ? false : l.press(x, y, ms);
  }

  @Override
  public boolean swipe(double x1, double y1, double x2, double y2, int ms) {
    LuaServiceApi l = lua();
    return l == null ? false : l.swipe(x1, y1, x2, y2, ms);
  }

  @Override
  public boolean swipe(double x1, double y1, double x2, double y2, int ms, int times) {
    LuaServiceApi l = lua();
    return l == null ? false : l.swipe(x1, y1, x2, y2, ms, times);
  }

  @Override
  public boolean toClick(AccessibilityNodeInfo n) {
    LuaServiceApi l = lua();
    return l == null ? false : l.toClick(n);
  }

  @Override
  public boolean toClick() {
    LuaServiceApi l = lua();
    return l == null ? false : l.toClick();
  }

  @Override
  public boolean toClick(double x, double y) {
    LuaServiceApi l = lua();
    return l == null ? false : l.toClick(x, y);
  }

  @Override
  public boolean toLongClick(AccessibilityNodeInfo n) {
    LuaServiceApi l = lua();
    return l == null ? false : l.toLongClick(n);
  }

  @Override
  public boolean toLongClick() {
    LuaServiceApi l = lua();
    return l == null ? false : l.toLongClick();
  }

  @Override
  public boolean longClick(AccessibilityNodeInfo n) {
    LuaServiceApi l = lua();
    return l == null ? false : l.longClick(n);
  }

  @Override
  public boolean toBack() {
    LuaServiceApi l = lua();
    return l == null ? false : l.toBack();
  }

  @Override
  public boolean toHome() {
    LuaServiceApi l = lua();
    return l == null ? false : l.toHome();
  }

  @Override
  public boolean toNotification() {
    LuaServiceApi l = lua();
    return l == null ? false : l.toNotification();
  }

  @Override
  public boolean toRecents() {
    LuaServiceApi l = lua();
    return l == null ? false : l.toRecents();
  }

  @Override
  public boolean toParent() {
    LuaServiceApi l = lua();
    return l == null ? false : l.toParent();
  }

  @Override
  public boolean scrollForward(AccessibilityNodeInfo n) {
    LuaServiceApi l = lua();
    return l == null ? false : l.scrollForward(n);
  }

  @Override
  public boolean scrollBackward(AccessibilityNodeInfo n) {
    LuaServiceApi l = lua();
    return l == null ? false : l.scrollBackward(n);
  }

  @Override
  public void speak(String text) {
    LuaServiceApi l = lua();
    if (l != null) l.speak(text);
  }

  @Override
  public void asyncSpeak(String text) {
    LuaServiceApi l = lua();
    if (l != null) l.asyncSpeak(text);
  }

  @Override
  public AccessibilityNodeInfo getEditText() {
    LuaServiceApi l = lua();
    return l == null ? null : l.getEditText();
  }

  @Override
  public boolean insertText(AccessibilityNodeInfo node, String text) {
    LuaServiceApi l = lua();
    return l == null ? false : l.insertText(node, text);
  }

  @Override
  public void openUrl(String url) {
    LuaServiceApi l = lua();
    if (l != null) l.openUrl(url);
  }

  @Override
  public void playSoundClock() {
    LuaServiceApi l = lua();
    if (l != null) l.playSoundClock();
  }

  @Override
  public void playSoundClock(int ignored) {
    LuaServiceApi l = lua();
    if (l != null) l.playSoundClock(ignored);
  }

  @Override
  public void playSoundScroll() {
    LuaServiceApi l = lua();
    if (l != null) l.playSoundScroll();
  }

  @Override
  public void playSoundScroll(float ignored) {
    LuaServiceApi l = lua();
    if (l != null) l.playSoundScroll(ignored);
  }

  @Override
  public void playSoundGestureBegin() {
    LuaServiceApi l = lua();
    if (l != null) l.playSoundGestureBegin();
  }

  @Override
  public void playSoundGestureEnd() {
    LuaServiceApi l = lua();
    if (l != null) l.playSoundGestureEnd();
  }

  @Override
  public void playSoundActionItem() {
    LuaServiceApi l = lua();
    if (l != null) l.playSoundActionItem();
  }

  @Override
  public void playSoundTick() {
    LuaServiceApi l = lua();
    if (l != null) l.playSoundTick();
  }

  @Override
  public void playSoundTick(int ignored) {
    LuaServiceApi l = lua();
    if (l != null) l.playSoundTick(ignored);
  }

  @Override
  public void toast(String text) {
    LuaServiceApi l = lua();
    if (l != null) l.toast(text);
  }

  @Override
  public void log(String text) {
    LuaServiceApi l = lua();
    if (l != null) l.log(text);
  }

  @Override
  public ClassLoader loadDex(String path) {
    LuaServiceApi l = lua();
    return l == null ? null : l.loadDex(path);
  }

  @Override
  public HashMap<String, String> getLibrarys() {
    LuaServiceApi l = lua();
    return l == null ? null : l.getLibrarys();
  }

  @Override
  public Object getCompatibilitySetting(String name) {
    LuaServiceApi l = lua();
    return l == null ? null : l.getCompatibilitySetting(name);
  }

  @Override
  public void setAcquireWakeLock(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setAcquireWakeLock(arg0);
  }

  @Override
  public void setAlarmEndHour(java.lang.Integer arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setAlarmEndHour(arg0);
  }

  @Override
  public void setAlarmInterval(int arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setAlarmInterval(arg0);
  }

  @Override
  public void setAlarmStartHour(java.lang.Integer arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setAlarmStartHour(arg0);
  }

  @Override
  public void setAsyncAudioFocus(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setAsyncAudioFocus(arg0);
  }

  @Override
  public void setAsyncSpeakEnabled(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setAsyncSpeakEnabled(arg0);
  }

  @Override
  public void setAsyncTTSSpeed(int arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setAsyncTTSSpeed(arg0);
  }

  @Override
  public void setAsyncTTSVolume(float arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setAsyncTTSVolume(arg0);
  }

  @Override
  public void setAsyncTtsEngine() {
    LuaServiceApi l = lua();
    if (l != null) l.setAsyncTtsEngine();
  }

  @Override
  public void setAsyncTtsPitch(int arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setAsyncTtsPitch(arg0);
  }

  @Override
  public void setAsyncTtsScale(float arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setAsyncTtsScale(arg0);
  }

  @Override
  public void setAsyncUseAccessibilityVolume(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setAsyncUseAccessibilityVolume(arg0);
  }

  @Override
  public void setAsyncUseProixmitySensor(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setAsyncUseProixmitySensor(arg0);
  }

  @Override
  public void setAsyncUseTouchStop(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setAsyncUseTouchStop(arg0);
  }

  @Override
  public void setAsyncUseWakeLock(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setAsyncUseWakeLock(arg0);
  }

  @Override
  public void setAudioFocus(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setAudioFocus(arg0);
  }

  @Override
  public void setAutoPrevious(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setAutoPrevious(arg0);
  }

  @Override
  public void setAutoRead(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setAutoRead(arg0);
  }

  @Override
  public void setAutoShowVirtualScreen() {
    LuaServiceApi l = lua();
    if (l != null) l.setAutoShowVirtualScreen();
  }

  @Override
  public void setAutoSpeech(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setAutoSpeech(arg0);
  }

  @Override
  public void setAutoSpeechEnabled(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setAutoSpeechEnabled(arg0);
  }

  @Override
  public void setAutoTrans(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setAutoTrans(arg0);
  }

  @Override
  public void setCharMode(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setCharMode(arg0);
  }

  @Override
  public void setCharModeGesture(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setCharModeGesture(arg0);
  }

  @Override
  public void setClearNode(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setClearNode(arg0);
  }

  @Override
  public void setCloudNodeInfoLabel(java.lang.String arg0, java.lang.String arg1, java.lang.String arg2) {
    LuaServiceApi l = lua();
    if (l != null) l.setCloudNodeInfoLabel(arg0, arg1, arg2);
  }

  @Override
  public void setContentBlacklist(java.lang.String arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setContentBlacklist(arg0);
  }

  @Override
  public void setDimmed(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setDimmed(arg0);
  }

  @Override
  public void setDimmedEnabled(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setDimmedEnabled(arg0);
  }

  @Override
  public void setDoubleFling(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setDoubleFling(arg0);
  }

  @Override
  public void setDoubleFlingTime(int arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setDoubleFlingTime(arg0);
  }

  @Override
  public void setDoubleTapUserIntentTimeout(int arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setDoubleTapUserIntentTimeout(arg0);
  }

  @Override
  public void setEdgeGesture(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setEdgeGesture(arg0);
  }

  @Override
  public void setEdgeGestureWidth(java.lang.Integer arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setEdgeGestureWidth(arg0);
  }

  @Override
  public void setEnabled(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setEnabled(arg0);
  }

  @Override
  public void setFalseTouchPrevention(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setFalseTouchPrevention(arg0);
  }

  @Override
  public void setFalseTouchPreventionEnabled(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setFalseTouchPreventionEnabled(arg0);
  }

  @Override
  public void setForeground2(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setForeground2(arg0);
  }

  @Override
  public void setGameMode(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setGameMode(arg0);
  }

  @Override
  public void setInEdgeGesture(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setInEdgeGesture(arg0);
  }

  @Override
  public void setIncallMode() {
    LuaServiceApi l = lua();
    if (l != null) l.setIncallMode();
  }

  @Override
  public void setIncallMode(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setIncallMode(arg0);
  }

  @Override
  public void setInputMode(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setInputMode(arg0);
  }

  @Override
  public void setKeepFocusPosition(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setKeepFocusPosition(arg0);
  }

  @Override
  public void setLeftButton(java.lang.String arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setLeftButton(arg0);
  }

  @Override
  public void setLongClickTime(int arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setLongClickTime(arg0);
  }

  @Override
  public void setLongCopy(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setLongCopy(arg0);
  }

  @Override
  public void setMenuUpTap(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setMenuUpTap(arg0);
  }

  @Override
  public void setModeAudio(int arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setModeAudio(arg0);
  }

  @Override
  public void setMultiFingerGesturesEnabled(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setMultiFingerGesturesEnabled(arg0);
  }

  @Override
  public void setNaviUpTap(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setNaviUpTap(arg0);
  }

  @Override
  public void setNavigationGestureWidth(int arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setNavigationGestureWidth(arg0);
  }

  @Override
  public void setNoTouch(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setNoTouch(arg0);
  }

  @Override
  public void setNotificationAnnouncementSpeak(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setNotificationAnnouncementSpeak(arg0);
  }

  @Override
  public void setNotificationAppNameSpeak(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setNotificationAppNameSpeak(arg0);
  }

  @Override
  public void setNotificationBarSpeak(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setNotificationBarSpeak(arg0);
  }

  @Override
  public void setNotificationBlackList(java.lang.String arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setNotificationBlackList(arg0);
  }

  @Override
  public void setNotificationInAutoReadNoSpeak(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setNotificationInAutoReadNoSpeak(arg0);
  }

  @Override
  public void setNotificationInCallNoSpeak(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setNotificationInCallNoSpeak(arg0);
  }

  @Override
  public void setNotificationInTouchNoSpeak(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setNotificationInTouchNoSpeak(arg0);
  }

  @Override
  public void setNotificationSpeak(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setNotificationSpeak(arg0);
  }

  @Override
  public void setNotificationSummary(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setNotificationSummary(arg0);
  }

  @Override
  public void setNotificationToastSpeak(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setNotificationToastSpeak(arg0);
  }

  @Override
  public void setNotificationWhiteList(java.lang.String arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setNotificationWhiteList(arg0);
  }

  @Override
  public void setOnlyNotification(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setOnlyNotification(arg0);
  }

  @Override
  public void setPartialWakeLock(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setPartialWakeLock(arg0);
  }

  @Override
  public void setPauseGestureDetection(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setPauseGestureDetection(arg0);
  }

  @Override
  public void setProfiles() {
    LuaServiceApi l = lua();
    if (l != null) l.setProfiles();
  }

  @Override
  public void setProfiles(java.lang.String arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setProfiles(arg0);
  }

  @Override
  public void setRawSpeak(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setRawSpeak(arg0);
  }

  @Override
  public void setRightButton(java.lang.String arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setRightButton(arg0);
  }

  @Override
  public void setScreenOffSpeak(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setScreenOffSpeak(arg0);
  }

  @Override
  public void setSelectionMode(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setSelectionMode(arg0);
  }

  @Override
  public void setSensor(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setSensor(arg0);
  }

  @Override
  public void setServiceHandlesDoubleTap(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setServiceHandlesDoubleTap(arg0);
  }

  @Override
  public void setSingleTap(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setSingleTap(arg0);
  }

  @Override
  public void setSound(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setSound(arg0);
  }

  @Override
  public void setSoundVolume(float arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setSoundVolume(arg0);
  }

  @Override
  public void setSpeakListViewItem(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setSpeakListViewItem(arg0);
  }

  @Override
  public void setSpeaking(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setSpeaking(arg0);
  }

  @Override
  public void setSpeeching(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setSpeeching(arg0);
  }

  @Override
  public void setStreamVolume(int arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setStreamVolume(arg0);
  }

  @Override
  public void setSupperMode(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setSupperMode(arg0);
  }

  @Override
  public void setSupperModeAux(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setSupperModeAux(arg0);
  }

  @Override
  public void setTTSEnabled(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setTTSEnabled(arg0);
  }

  @Override
  public void setTTSSpeaker(java.lang.String arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setTTSSpeaker(arg0);
  }

  @Override
  public void setTTSSpeed(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setTTSSpeed(arg0);
  }

  @Override
  public void setTTSSpeed(int arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setTTSSpeed(arg0);
  }

  @Override
  public void setTTSVolume(float arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setTTSVolume(arg0);
  }

  @Override
  public void setTapMode(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setTapMode(arg0);
  }

  @Override
  public void setTimerInterval(int arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setTimerInterval(arg0);
  }

  @Override
  public void setTimerModeAux(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setTimerModeAux(arg0);
  }

  @Override
  public void setTimerModePause(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setTimerModePause(arg0);
  }

  @Override
  public void setTouchFocusUserIntentTimeout(int arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setTouchFocusUserIntentTimeout(arg0);
  }

  @Override
  public void setTouchMode(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setTouchMode(arg0);
  }

  @Override
  public void setTtsEngine() {
    LuaServiceApi l = lua();
    if (l != null) l.setTtsEngine();
  }

  @Override
  public void setTtsEngine(java.lang.String arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setTtsEngine(arg0);
  }

  @Override
  public void setTtsPitch(int arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setTtsPitch(arg0);
  }

  @Override
  public void setTtsScale(float arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setTtsScale(arg0);
  }

  @Override
  public void setTypingFocusUserIntentTimeout(int arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setTypingFocusUserIntentTimeout(arg0);
  }

  @Override
  public void setUnknownIndex(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUnknownIndex(arg0);
  }

  @Override
  public void setUnknownLabel(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUnknownLabel(arg0);
  }

  @Override
  public void setUpTap(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUpTap(arg0);
  }

  @Override
  public void setUpTapEnabled(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUpTapEnabled(arg0);
  }

  @Override
  public void setUsageHints(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUsageHints(arg0);
  }

  @Override
  public void setUseAccessibilityVolume(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseAccessibilityVolume(arg0);
  }

  @Override
  public void setUseAccessibilityVolumeFix(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseAccessibilityVolumeFix(arg0);
  }

  @Override
  public void setUseAlarm(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseAlarm(arg0);
  }

  @Override
  public void setUseAppTTS(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseAppTTS(arg0);
  }

  @Override
  public void setUseAutoFocus(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseAutoFocus(arg0);
  }

  @Override
  public void setUseAutoViewType(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseAutoViewType(arg0);
  }

  @Override
  public void setUseBottomMenuEdgeGesture(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseBottomMenuEdgeGesture(arg0);
  }

  @Override
  public void setUseCloudLabel(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseCloudLabel(arg0);
  }

  @Override
  public void setUseContentChanged(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseContentChanged(arg0);
  }

  @Override
  public void setUseContentTreeChanged(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseContentTreeChanged(arg0);
  }

  @Override
  public void setUseCustomDictRegex(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseCustomDictRegex(arg0);
  }

  @Override
  public void setUseCustomDictSpeak(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseCustomDictSpeak(arg0);
  }

  @Override
  public void setUseCustomNodeName(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseCustomNodeName(arg0);
  }

  @Override
  public void setUseCustomSpeakClear(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseCustomSpeakClear(arg0);
  }

  @Override
  public void setUseCustomSpeakSymbolNumber(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseCustomSpeakSymbolNumber(arg0);
  }

  @Override
  public void setUseCustomSymbolSpeak(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseCustomSymbolSpeak(arg0);
  }

  @Override
  public void setUseDpadKey(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseDpadKey(arg0);
  }

  @Override
  public void setUseEdgeGesture(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseEdgeGesture(arg0);
  }

  @Override
  public void setUseEditHint(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseEditHint(arg0);
  }

  @Override
  public void setUseEffectPro(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseEffectPro(arg0);
  }

  @Override
  public void setUseEmojiSpeak(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseEmojiSpeak(arg0);
  }

  @Override
  public void setUseEmojiSpeakClear(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseEmojiSpeakClear(arg0);
  }

  @Override
  public void setUseFastMode(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseFastMode(arg0);
  }

  @Override
  public void setUseFingerprintGesture(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseFingerprintGesture(arg0);
  }

  @Override
  public void setUseFloatMenu(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseFloatMenu(arg0);
  }

  @Override
  public void setUseForeground(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseForeground(arg0);
  }

  @Override
  public void setUseForegroundPro(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseForegroundPro(arg0);
  }

  @Override
  public void setUseGameModeNoEdge(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseGameModeNoEdge(arg0);
  }

  @Override
  public void setUseGameModeNoSpeak(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseGameModeNoSpeak(arg0);
  }

  @Override
  public void setUseGestureDetection(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseGestureDetection(arg0);
  }

  @Override
  public void setUseHeadSetKey(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseHeadSetKey(arg0);
  }

  @Override
  public void setUseHotkey(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseHotkey(arg0);
  }

  @Override
  public void setUseImageFilter(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseImageFilter(arg0);
  }

  @Override
  public void setUseImageToText(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseImageToText(arg0);
  }

  @Override
  public void setUseInputMethodDoubleTap(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseInputMethodDoubleTap(arg0);
  }

  @Override
  public void setUseInputMethodGesture(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseInputMethodGesture(arg0);
  }

  @Override
  public void setUseInputMethodNoGesture(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseInputMethodNoGesture(arg0);
  }

  @Override
  public void setUseInputMethodNoTouch(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseInputMethodNoTouch(arg0);
  }

  @Override
  public void setUseInputMethodSpeakCandidate(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseInputMethodSpeakCandidate(arg0);
  }

  @Override
  public void setUseInputMethodSpeakCommit(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseInputMethodSpeakCommit(arg0);
  }

  @Override
  public void setUseInputMethodSpeakCommitSpace(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseInputMethodSpeakCommitSpace(arg0);
  }

  @Override
  public void setUseInputMethodSpeakKey(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseInputMethodSpeakKey(arg0);
  }

  @Override
  public void setUseInputMethodSpeakPassword(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseInputMethodSpeakPassword(arg0);
  }

  @Override
  public void setUseInputMethodSpeakWord(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseInputMethodSpeakWord(arg0);
  }

  @Override
  public void setUseKeepType(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseKeepType(arg0);
  }

  @Override
  public void setUseLeftRight(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseLeftRight(arg0);
  }

  @Override
  public void setUseListIndex(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseListIndex(arg0);
  }

  @Override
  public void setUseListItemIndex(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseListItemIndex(arg0);
  }

  @Override
  public void setUseListItemIndexRealTime(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseListItemIndexRealTime(arg0);
  }

  @Override
  public void setUseLog(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseLog(arg0);
  }

  @Override
  public void setUseLongClickVoiceImage(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseLongClickVoiceImage(arg0);
  }

  @Override
  public void setUseLongClickVoiceInput(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseLongClickVoiceInput(arg0);
  }

  @Override
  public void setUseLoopMove(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseLoopMove(arg0);
  }

  @Override
  public void setUseLoopMoveSpace(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseLoopMoveSpace(arg0);
  }

  @Override
  public void setUseMoreNode(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseMoreNode(arg0);
  }

  @Override
  public void setUseMoveListViewItem(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseMoveListViewItem(arg0);
  }

  @Override
  public void setUseMoveWindow(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseMoveWindow(arg0);
  }

  @Override
  public void setUseMultiTouchMode(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseMultiTouchMode(arg0);
  }

  @Override
  public void setUseNavigationGesture(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseNavigationGesture(arg0);
  }

  @Override
  public void setUseNewTextMoveType(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseNewTextMoveType(arg0);
  }

  @Override
  public void setUseNewWebMove(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseNewWebMove(arg0);
  }

  @Override
  public void setUseNoBluetooth(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseNoBluetooth(arg0);
  }

  @Override
  public void setUseNode(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseNode(arg0);
  }

  @Override
  public void setUseNodeInfo(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseNodeInfo(arg0);
  }

  @Override
  public void setUseOCR(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseOCR(arg0);
  }

  @Override
  public void setUseOfflineTTS(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseOfflineTTS(arg0);
  }

  @Override
  public void setUseOldPrevious(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseOldPrevious(arg0);
  }

  @Override
  public void setUsePagerScroll(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUsePagerScroll(arg0);
  }

  @Override
  public void setUsePinyinSpeak(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUsePinyinSpeak(arg0);
  }

  @Override
  public void setUsePowerSave(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUsePowerSave(arg0);
  }

  @Override
  public void setUseProgress(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseProgress(arg0);
  }

  @Override
  public void setUseProixmitySensor(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseProixmitySensor(arg0);
  }

  @Override
  public void setUseProximityFalseTouchPrevention(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseProximityFalseTouchPrevention(arg0);
  }

  @Override
  public void setUseProximityFalseTouchPreventionOnlyLock(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseProximityFalseTouchPreventionOnlyLock(arg0);
  }

  @Override
  public void setUseRawClick(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseRawClick(arg0);
  }

  @Override
  public void setUseRawTap(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseRawTap(arg0);
  }

  @Override
  public void setUseRingtoneVolume(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseRingtoneVolume(arg0);
  }

  @Override
  public void setUseScreenOffSpeak(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseScreenOffSpeak(arg0);
  }

  @Override
  public void setUseScreenOnSpeak(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseScreenOnSpeak(arg0);
  }

  @Override
  public void setUseScreenOnSpeakBattery(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseScreenOnSpeakBattery(arg0);
  }

  @Override
  public void setUseScreenOnSpeakNotification(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseScreenOnSpeakNotification(arg0);
  }

  @Override
  public void setUseScrollListContent(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseScrollListContent(arg0);
  }

  @Override
  public void setUseScrollListIndex(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseScrollListIndex(arg0);
  }

  @Override
  public void setUseScrollListKeepPosition(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseScrollListKeepPosition(arg0);
  }

  @Override
  public void setUseShowDisabledQuickMenu(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseShowDisabledQuickMenu(arg0);
  }

  @Override
  public void setUseShowText(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseShowText(arg0);
  }

  @Override
  public void setUseSingleTTS(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseSingleTTS(arg0);
  }

  @Override
  public void setUseSlowSpeed(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseSlowSpeed(arg0);
  }

  @Override
  public void setUseSmallFilter(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseSmallFilter(arg0);
  }

  @Override
  public void setUseSpeakKey(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseSpeakKey(arg0);
  }

  @Override
  public void setUseSpeakQqChanged(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseSpeakQqChanged(arg0);
  }

  @Override
  public void setUseSpeakState(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseSpeakState(arg0);
  }

  @Override
  public void setUseSpeakWindowAllChanged(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseSpeakWindowAllChanged(arg0);
  }

  @Override
  public void setUseSupperFilter(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseSupperFilter(arg0);
  }

  @Override
  public void setUseTextFormatCharNumber(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseTextFormatCharNumber(arg0);
  }

  @Override
  public void setUseTextFormatCharUpper(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseTextFormatCharUpper(arg0);
  }

  @Override
  public void setUseTextFormatFirst(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseTextFormatFirst(arg0);
  }

  @Override
  public void setUseTextFormatReadWord(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseTextFormatReadWord(arg0);
  }

  @Override
  public void setUseTouchMode(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseTouchMode(arg0);
  }

  @Override
  public void setUseTouchStop(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseTouchStop(arg0);
  }

  @Override
  public void setUseUpLongClick(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseUpLongClick(arg0);
  }

  @Override
  public void setUseUpLongQuickMenu(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseUpLongQuickMenu(arg0);
  }

  @Override
  public void setUseViewMove(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseViewMove(arg0);
  }

  @Override
  public void setUseViewName(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseViewName(arg0);
  }

  @Override
  public void setUseViewTypeMoveMode(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseViewTypeMoveMode(arg0);
  }

  @Override
  public void setUseVolumeKeyEnabled(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseVolumeKeyEnabled(arg0);
  }

  @Override
  public void setUseVolumeKeyMoveText(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseVolumeKeyMoveText(arg0);
  }

  @Override
  public void setUseWakeLock(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseWakeLock(arg0);
  }

  @Override
  public void setUseWakeLockPro(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseWakeLockPro(arg0);
  }

  @Override
  public void setUseWebMove(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseWebMove(arg0);
  }

  @Override
  public void setUseWebViewName(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseWebViewName(arg0);
  }

  @Override
  public void setUseWindowTitleChanged(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setUseWindowTitleChanged(arg0);
  }

  @Override
  public void setVibrate(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setVibrate(arg0);
  }

  @Override
  public void setVibrateIntensity(int arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setVibrateIntensity(arg0);
  }

  @Override
  public void setVoiceHelperMediaMuteMode(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setVoiceHelperMediaMuteMode(arg0);
  }

  @Override
  public void setVoiceInputAppendMode(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setVoiceInputAppendMode(arg0);
  }

  @Override
  public void setVolumeKeyEnabled(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setVolumeKeyEnabled(arg0);
  }

  @Override
  public void setWakeLock(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setWakeLock(arg0);
  }

  @Override
  public void setWakeLock(long arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setWakeLock(arg0);
  }

  @Override
  public void setWakeLock_aux(boolean arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setWakeLock_aux(arg0);
  }

  @Override
  public void setup(java.lang.String arg0) {
    LuaServiceApi l = lua();
    if (l != null) l.setup(arg0);
  }
}
