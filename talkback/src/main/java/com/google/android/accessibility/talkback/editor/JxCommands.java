/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.InputMethodManager;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.TalkBackService;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * service.execute(name): the 173 commands of Jieshuo. Every command is known by its string id
 * (command_click), its English name ("Click") and its Chinese name; lookups ignore case.
 * Commands this app has no feature for return false and never throw.
 */
final class JxCommands {
  private JxCommands() {}

  /** {id, English name, Chinese name}; order = Jieshuo's own list (JX_EXECUTE_COMMANDS.txt). */
  static final String[][] TABLE = {
      {"command_nothing", "No command", "无操作"},
      {"directory_plugins", "Plugins", "插件"},
      {"directory_cmd", "Custom voice commands", "指令"},
      {"directory_tools", "Tools", "工具"},
      {"directory_timer", "Timer", "计时"},
      {"app", "Application", "程序"},
      {"profiles_title", "Profiles", "情景模式"},
      {"plugin_title", "Extensions", "扩展插件"},
      {"tool_title", "Tools", "扩展工具"},
      {"main_menu", "Main menu", "主菜单"},
      {"advanced_menu_title", "Recognition menu", "高级菜单"},
      {"quick_menu_title", "Current per-app gestures menu", "快捷菜单"},
      {"action_menu_title", "Functions menu", "操作菜单"},
      {"main_setting", "Screen reader settings", "程序设置"},
      {"command_start_log", "Enable logging", "开始记录日志"},
      {"command_gesture_help", "Gesture scheme description", "手势说明"},
      {"command_disable_touch", "Suspend browse by touch", "暂停触摸浏览"},
      {"command_disable_feedback", "Suspend speech feedback and browse by touch", "暂停语音反馈"},
      {"command_disable_hotkey", "Disable hotkeys", "暂停快捷键"},
      {"command_false_touch_prevention", "Lock touch", "防误触模式"},
      {"command_enable_dim_screen", "Enable curtain", "黑屏模式"},
      {"command_disable_edge_gesture_title", "Suspend edge gestures", "暂停边缘手势"},
      {"command_disable_main_tts", "Suspend main TTS engine", "暂停主语音合成"},
      {"command_disable_async_tts", "Suspend secondary TTS engine", "暂停副语音合成"},
      {"command_incall_mode", "Earpiece mode", "听筒模式"},
      {"switch_multi_finger_gestures", "Toggle multi-finger gestures", "多指手势"},
      {"command_set_tts_engine", "Select TTS engine", "选择语音库"},
      {"command_toggle_tts_engine", "Switch the currently used TTS engine", "切换语音库"},
      {"command_raise_scr_volume", "Increase accessibility volume", "增大读屏音量"},
      {"command_lower_scr_volume", "Decrease accessibility volume", "减小读屏音量"},
      {"command_speed_to_fast", "Increase speech rate", "加快语速"},
      {"command_speed_to_slow", "Decrease speech rate", "减慢语速"},
      {"command_raise_volume", "Increase media volume", "增大音量"},
      {"command_lower_volume", "Decrease media volume", "减小音量"},
      {"command_voice_helper", "Voice assistant", "语音助手"},
      {"command_voice_input", "Voice dictation", "语音输入"},
      {"actions", "Actions", "操作"},
      {"url_menu_title", "Open an URL", "链接"},
      {"add_node_changed_list", "Monitor the currently focused element", "焦点监控"},
      {"node_changed_list_manager", "Focus monitor manager", "焦点监控管理"},
      {"edit_dict", "Edit dictionary", "编辑词典"},
      {"command_edit_node_name", "Edit node alias", "编辑焦点别名"},
      {"command_add_content_blacklist", "Add content blacklist", "添加内容黑名单"},
      {"command_verbatim_speak2", "Spell current focus", "逐字朗读"},
      {"split_menu_title", "Granular browsing mode", "拆分浏览"},
      {"command_gestures_edit", "Edit current Per-App gesture scheme", "编辑手势"},
      {"command_auto_trans", "Automatic translation", "自动翻译"},
      {"command_recognition_caption", "Recognize subtitles", "朗读字幕"},
      {"use_ocr_title", "Automatically use OCR", "自动OCR识别"},
      {"command_virtual_screen", "Virtual screen", "虚拟屏幕"},
      {"command_virtual_table", "Identify form", "表格识别"},
      {"command_ocr_text", "Recognize text on the current screen", "文字识别"},
      {"command_ocr_focus", "Recognize text of the current focused element", "焦点识别"},
      {"command_verification", "Get CAPTCHA", "验证码"},
      {"command_trans", "Translation", "翻译"},
      {"camera_title", "Jieshuo Camera", "解说相机"},
      {"command_now_time", "Current time", "现在时间"},
      {"command_timer_mode", "Timer", "计时器"},
      {"command_pasue_timer", "Pause timer", "暂停计时"},
      {"command_current_location", "Current location", "当前位置"},
      {"command_speak_light", "Speak current lighting", "当前亮度"},
      {"command_speak_time", "Speak time and battery level", "时间电量"},
      {"command_speak_net", "Speak network information", "网络信息"},
      {"command_to_head_set_key", "Play or pause", "播放暂停"},
      {"command_media_previous", "Previous media", "上一曲"},
      {"command_media_next", "Next media", "下一曲"},
      {"clipboard_menu_title", "Clipboard history", "剪切板"},
      {"favorites_menu_title", "Favorites", "收藏夹"},
      {"command_paste", "Paste", "粘贴"},
      {"command_copy", "Copy", "复制"},
      {"command_append_copy", "Append", "追加复制"},
      {"command_long_copy", "List copying", "长复制"},
      {"command_clean", "Clear", "清空"},
      {"split_edit_title", "Granular editing mode", "拆分编辑"},
      {"command_long_edit", "edit current text", "长文本编辑"},
      {"command_all_select", "Full text selection", "全文选取"},
      {"command_to_recents", "Recent applications", "最近任务"},
      {"command_to_home", "Home screen", "主屏幕"},
      {"command_to_back", "Go back", "返回"},
      {"command_to_notifications", "Notification bar", "打开通知栏"},
      {"command_quick_settings", "Quick settings", "控制中心"},
      {"command_take_screenshot", "Take a screenshot", "截屏"},
      {"command_off_screen2", "Screen lock", "关屏"},
      {"command_power_dialog", "Power menu", "关机菜单"},
      {"command_to_split_screen", "Split screen view", "分屏"},
      {"command_notification_box", "Notification box", "通知盒子"},
      {"command_click", "Click", "点击"},
      {"command_long_click", "Long press", "长按"},
      {"raw_click", "Direct click", "模拟点击"},
      {"raw_long_click", "Direct long press", "模拟长按"},
      {"command_raw_double_tap", "[Double tap]", "模拟双击"},
      {"raw_swipe_left", "Direct swipe left", "模拟左滑"},
      {"raw_swipe_right", "Direct swipe right", "模拟右滑"},
      {"raw_swipe_up", "Direct swipe up", "模拟上滑"},
      {"raw_swipe_down", "direct swipe down", "模拟下滑"},
      {"command_virtual_navigation", "Virtual navigation", "虚拟导航"},
      {"char_mode", "Character by character browsing mode", "逐字浏览"},
      {"list_mode_title", "List browsing", "列表浏览"},
      {"all_text_title", "Full text editing", "全文浏览"},
      {"command_node_mode", "Node browsing mode", "节点浏览"},
      {"command_all_speak", "Read the whole screen", "全屏朗读"},
      {"command_supper_mode", "Enhanced detailed focusing mode", "强力模式"},
      {"command_raw_tap", "Passthrough mode", "指点模式"},
      {"command_up_tap", "Lift to activate", "抬手激活"},
      {"command_tap_mode", "Tapping to move focus mode", "点击浏览"},
      {"command_read_mode", "Reading mode", "阅读模式"},
      {"command_auto_next", "Automatic browsing", "自动浏览"},
      {"auto_previous", "Backwards automatic browsing", "向上浏览"},
      {"input_method", "Input", "输入法"},
      {"command_show_input_method_picker", "Change input method", "选择输入法"},
      {"command_speak_all_notification", "Read all notifications", "朗读全部通知"},
      {"command_left_shortcut", "Left shortcut", "左快捷功能"},
      {"command_right_shortcut", "Right shortcut", "右快捷功能"},
      {"command_start", "To top", "开头"},
      {"command_end", "To end", "结尾"},
      {"select_granularity_mode", "Select navigation type", "选择导航类型"},
      {"to_previous_granularity", "Previous navigation type", "上个导航类型"},
      {"to_next_granularity", "Next navigation type", "下个导航类型"},
      {"to_previous_granularity_item", "Previous element of the specified type", "上个导航项目"},
      {"to_next_granularity_item", "Next element of the specified type", "下个导航项目"},
      {"command_previous_window", "Previous screen area", "上个窗口"},
      {"command_next_window", "Next screen area", "下个窗口"},
      {"command_to_previous_focus", "Previous focusable element", "上个焦点"},
      {"command_to_next_focus", "Next focusable element", "下个焦点"},
      {"command_to_previous_scroll", "Scroll back", "向上滚动"},
      {"command_to_next_scroll", "Scroll forward", "向下滚动"},
      {"command_to_previous_progress", "Decrease slider value", "减少进度"},
      {"command_to_next_progress", "Increase slider value", "增加进度"},
      {"command_to_previous_section", "Previous heading", "上个标题"},
      {"command_to_next_section", "Next heading", "下个标题"},
      {"command_to_previous_link", "Previous link", "上个链接"},
      {"command_to_next_link", "Next link", "下个链接"},
      {"command_to_previous_control", "Previous control", "上个控件"},
      {"command_to_next_control", "Next control", "下个控件"},
      {"command_to_previous_char", "Previous character", "上个字符"},
      {"command_to_next_char", "Next character", "下个字符"},
      {"command_to_previous_word", "Previous word", "上个字词"},
      {"command_to_next_word", "Next word", "下个字词"},
      {"command_to_previous_row", "Previous sentence", "上个句子"},
      {"command_to_next_row", "Next sentence", "下个句子"},
      {"command_to_previous_paragraph", "Previous paragraph", "上个段落"},
      {"command_to_next_paragraph", "Next paragraph", "下个段落"},
      {"command_copy_to_cloud", "Copy to cloud clipboard", "复制到云剪切板"},
      {"command_paste_from_cloud", "Paste from cloud clipboard", "从云剪切板粘贴"},
      {"command_up_page", "Direct scroll up", "上滑翻页"},
      {"command_down_page", "Direct scroll down", "下滑翻页"},
      {"command_left_page", "Direct scroll left", "左滑翻页"},
      {"command_right_page", "Direct scroll right", "右滑翻页"},
      {"command_media_fast_forward", "Fast forward", "快进"},
      {"command_media_rewind", "Rewind", "快退"},
      {"command_selection_mode", "Selection mode", "选择模式"},
      {"command_image_to_text", "Describe currently focused item", "图片描述"},
      {"command_image_to_text2", "Describe entire screen", "图片描述全屏"},
      {"picture_description", "Detailed description of focused item", "图像理解"},
      {"picture_description2", "Detailed description of entire screen", "图像理解全屏"},
      {"command_auto_list_speak", "Automatically read chat messages", "自动朗读消息"},
      {"command_auto_list_speak_history", "Automatic chat messages history", "自动朗读消息记录"},
      {"command_quick_mode", "Quick browsing", "快速浏览"},
      {"command_accept_call", "Answer call", "接听电话"},
      {"command_end_call", "End call", "挂断电话"},
      {"command_pause_a11y_volume", "Suspend the use of accessibility volume", "暂停无障碍音量"},
      {"chat_history", "Chat history", "对话记录"},
      {"command_long_hold", "Long press and hold", "点击按住"},
      {"command_recognition_icon", "Recognize the focused icon", "图标识别"},
      {"voice_picture_description", "Inquire by voice about current focus", "语音询问"},
      {"voice_picture_description2", "Inquire by Voice About the Entire Screen", "语音询问全屏"},
      {"video_description", "Video description", "视频描述"},
      {"command_all_node", "Show all nodes", "全部节点"},
      {"command_node_menu", "Focus menu", "焦点菜单"},
      {"command_take_screenshot2", "Take a screenshot of current focus", "焦点截屏"},
      {"voice_input_mode", "continuous voice dictation", "听写模式"},
      {"command_pause_gesture", "Suspend screen reader gestures", "暂停读屏手势"},
      {"command_grid_mode", "Screen Grid mode", "网格模式"}
  };

  /** Names used by earlier versions of our own scripts. */
  private static final String[][] LEGACY = {
    {"back", "command_to_back"},
    {"home", "command_to_home"},
    {"recents", "command_to_recents"},
    {"copy", "command_copy"},
    {"paste", "command_paste"},
  };

  private static final Map<String, String> BY_NAME = new HashMap<>();

  static {
    for (String[] r : TABLE) {
      put(r[0], r[0]);
      put(r[1], r[0]);
      put(r[2], r[0]);
    }
    for (String[] r : LEGACY) {
      put(r[0], r[1]);
    }
  }

  private static void put(String name, String id) {
    String k = norm(name);
    if (!BY_NAME.containsKey(k)) {
      BY_NAME.put(k, id);
    }
  }

  private static String norm(String s) {
    return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
  }

  /** Returns the command id for any accepted spelling, or null. */
  static String idOf(String name) {
    return BY_NAME.get(norm(name));
  }

  private static boolean global(TalkBackService s, int action) {
    return s != null && s.performGlobalAction(action);
  }

  private static boolean media(Context ctx, int keyCode) {
    AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
    if (am == null) {
      return false;
    }
    am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
    am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
    return true;
  }

  private static boolean volume(Context ctx, int stream, int direction) {
    AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
    if (am == null) {
      return false;
    }
    am.adjustStreamVolume(stream, direction, AudioManager.FLAG_SHOW_UI);
    return true;
  }

  private static boolean part(final LuaServiceApi api, final Feedback.FocusDirection.Action a) {
    final TalkBackService s = TalkBackService.getInstance();
    if (s == null) {
      return false;
    }
    api.postToUi(() -> s.msPartFeedback(Feedback.focusDirection(a)));
    return true;
  }

  private static boolean swipeScreen(LuaServiceApi api, double x1, double y1, double x2, double y2) {
    return api.swipe(x1, y1, x2, y2, 300);
  }

  private static boolean scrollDir(AccessibilityNodeInfo from, AccessibilityNodeInfo root, int actionId) {
    AccessibilityNodeInfo p = from;
    int guard = 0;
    while (p != null && guard++ < 30) {
      if (p.performAction(actionId)) {
        return true;
      }
      p = p.getParent();
    }
    return false;
  }

  /** Runs one command. {@code node} is the target item or null for the focused item. */
  static boolean run(LuaServiceApi api, Context ctx, String name, AccessibilityNodeInfo node) {
    String id = idOf(name);
    if (id == null) {
      return false;
    }
    try {
      return dispatch(api, ctx, id, node);
    } catch (Throwable t) {
      return false;
    }
  }

  private static boolean dispatch(
      LuaServiceApi api, Context ctx, String id, AccessibilityNodeInfo given) {
    TalkBackService s = TalkBackService.getInstance();
    if (s == null) {
      return false;
    }
    AccessibilityNodeInfo n = given != null ? given : api.focusedNode();
    switch (id) {
      case "command_nothing":
        return true;
      case "main_setting":
        Intent i = new Intent(s, com.android.talkback.TalkBackPreferencesActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        s.startActivity(i);
        return true;

      // ---- time and status
      case "command_speak_time":
        BatteryManager bm = (BatteryManager) ctx.getSystemService(Context.BATTERY_SERVICE);
        int pct = bm == null ? -1 : bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        String time = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date());
        api.speak(time + (pct >= 0 ? ", battery " + pct + "%" : ""));
        return true;
      case "command_now_time":
        api.speak(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date()));
        return true;

      // ---- system actions
      case "command_to_recents":
        return global(s, AccessibilityService.GLOBAL_ACTION_RECENTS);
      case "command_to_home":
        return global(s, AccessibilityService.GLOBAL_ACTION_HOME);
      case "command_to_back":
        return global(s, AccessibilityService.GLOBAL_ACTION_BACK);
      case "command_to_notifications":
        return global(s, AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS);
      case "command_quick_settings":
        return global(s, AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS);
      case "command_take_screenshot":
        return Build.VERSION.SDK_INT >= 28
            && global(s, AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT);
      case "command_off_screen2":
        return Build.VERSION.SDK_INT >= 28
            && global(s, AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN);
      case "command_power_dialog":
        return global(s, AccessibilityService.GLOBAL_ACTION_POWER_DIALOG);
      case "command_to_split_screen":
        return global(s, AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN);
      case "command_show_input_method_picker":
        InputMethodManager imm =
            (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) {
          return false;
        }
        imm.showInputMethodPicker();
        return true;
      case "command_voice_helper":
        Intent v = new Intent(Intent.ACTION_VOICE_COMMAND);
        v.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(v);
        return true;
      case "command_accept_call":
        android.telecom.TelecomManager tm =
            (android.telecom.TelecomManager) ctx.getSystemService(Context.TELECOM_SERVICE);
        if (tm == null || Build.VERSION.SDK_INT < 26) {
          return false;
        }
        tm.acceptRingingCall();
        return true;
      case "command_end_call":
        android.telecom.TelecomManager tm2 =
            (android.telecom.TelecomManager) ctx.getSystemService(Context.TELECOM_SERVICE);
        if (tm2 == null || Build.VERSION.SDK_INT < 28) {
          return false;
        }
        return tm2.endCall();

      // ---- click family
      case "command_click":
        return api.toClick(n);
      case "command_long_click":
        return api.toLongClick(n);
      case "raw_click":
        return n != null && api.tapNode(n, 50);
      case "raw_long_click":
        return n != null
            && api.tapNode(n, android.view.ViewConfiguration.getLongPressTimeout() + 150L);
      case "command_raw_double_tap":
        return n != null && api.tapNode(n, 50) && api.tapNode(n, 50);
      case "raw_swipe_left":
        return swipeScreen(api, 0.8, 0.5, 0.2, 0.5);
      case "raw_swipe_right":
        return swipeScreen(api, 0.2, 0.5, 0.8, 0.5);
      case "raw_swipe_up":
        return swipeScreen(api, 0.5, 0.8, 0.5, 0.2);
      case "raw_swipe_down":
        return swipeScreen(api, 0.5, 0.2, 0.5, 0.8);

      // ---- clipboard and text
      case "command_copy":
        return given != null ? api.copy(api.getText(given)) : api.copy();
      case "command_append_copy":
        return given != null ? api.appendCopy(api.getText(given)) : api.appendCopy();
      case "command_paste":
        return given != null ? api.paste(given) : api.paste();
      case "command_all_select":
        AccessibilityNodeInfo e = given != null ? given : api.inputNode();
        if (e == null || e.getText() == null) {
          return false;
        }
        Bundle b = new Bundle();
        b.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0);
        b.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, e.getText().length());
        return e.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, b);

      // ---- moving the screen reader focus
      case "command_to_next_focus":
        return api.toNext();
      case "command_to_previous_focus":
        return api.toPrevious();
      case "command_start":
        return part(api, Feedback.FocusDirection.Action.TOP);
      case "command_end":
        return part(api, Feedback.FocusDirection.Action.BOTTOM);
      case "to_next_granularity":
        return part(api, Feedback.FocusDirection.Action.NEXT_GRANULARITY);
      case "to_previous_granularity":
        return part(api, Feedback.FocusDirection.Action.PREVIOUS_GRANULARITY);
      case "command_all_speak":
        s.msStartReading(true);
        return true;

      // ---- scrolling and sliders
      case "command_to_next_scroll":
        return api.scrollForward(n);
      case "command_to_previous_scroll":
        return api.scrollBackward(n);
      case "command_to_next_progress":
        return n != null && n.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
      case "command_to_previous_progress":
        return n != null && n.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
      case "command_up_page":
        return scrollDir(n, null, AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP.getId());
      case "command_down_page":
        return scrollDir(n, null, AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN.getId());
      case "command_left_page":
        return scrollDir(n, null, AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_LEFT.getId());
      case "command_right_page":
        return scrollDir(n, null, AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_RIGHT.getId());

      // ---- media and volume
      case "command_to_head_set_key":
        return media(ctx, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
      case "command_media_previous":
        return media(ctx, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
      case "command_media_next":
        return media(ctx, KeyEvent.KEYCODE_MEDIA_NEXT);
      case "command_media_fast_forward":
        return media(ctx, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD);
      case "command_media_rewind":
        return media(ctx, KeyEvent.KEYCODE_MEDIA_REWIND);
      case "command_raise_volume":
        return volume(ctx, AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE);
      case "command_lower_volume":
        return volume(ctx, AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER);
      case "command_raise_scr_volume":
        return Build.VERSION.SDK_INT >= 26
            && volume(ctx, AudioManager.STREAM_ACCESSIBILITY, AudioManager.ADJUST_RAISE);
      case "command_lower_scr_volume":
        return Build.VERSION.SDK_INT >= 26
            && volume(ctx, AudioManager.STREAM_ACCESSIBILITY, AudioManager.ADJUST_LOWER);

      default:
        return false; // known Jieshuo command, but this app has no matching feature yet
    }
  }
}
