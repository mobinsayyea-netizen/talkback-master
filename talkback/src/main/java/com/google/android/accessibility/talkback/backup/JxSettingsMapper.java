package com.google.android.accessibility.talkback.backup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Pure Java (no Android): decides which Jieshuo setting becomes which MS setting and with which value.
 * Nothing is guessed: a Jieshuo key is used only when (a) MS has a setting with the same name behind
 * "ms_jx_", or (b) it is in the hand-checked table below. A value is written only when it has the type
 * of the MS setting and, for a fixed list, is one of the list's values.
 *
 * <p>Keys, tokens, passwords and account/identity values are never mapped, whatever their name.
 */
final class JxSettingsMapper {
  private JxSettingsMapper() {}

  /** What the restore decided. */
  static final class Plan {
    /** MS key -> value (Boolean, String or Set of String), in the order they were decided. */
    final Map<String, Object> apply = new LinkedHashMap<>();
    /** Jieshuo key -> MS key it was applied to (for the report). */
    final Map<String, String> from = new LinkedHashMap<>();
    /** Jieshuo keys that have no MS setting (and were not private). */
    final List<String> noMatch = new ArrayList<>();
    /** Jieshuo key -> reason (wrong type, value not in the MS list, no equal action). */
    final Map<String, String> rejected = new LinkedHashMap<>();
    /** Count of keys never read because they are keys, tokens or account values. */
    int privateSkipped;
    /** Jieshuo keys that are handled by the gesture restore or have no equal. */
    final List<String> elsewhere = new ArrayList<>();
  }

  /** Names that are never read: secrets, accounts, devices, update/log state of Jieshuo itself. */
  private static final Pattern PRIVATE =
      Pattern.compile(
          "(?i).*(token|api_?key|apikey|secret|password|passwd|_sid$|^uuid$|user_id|last_user|git_user|"
              + "gemini_.*key|radio_final|_key_final|^base_url$|^download_url$|^changelog|^versionName$|"
              + "^lastUpdateTime$|^crash_time$|^welcome$|^onStart$|^setup_|^sp_credits$|^no_storage$|"
              + "^check_permissions$|^use_auto_update$|^bg_startup).*");

  /** Same meaning in MS under another name: Jieshuo key -> MS key (type and values are still checked). */
  private static final String[][] SAME_MEANING = {
    // Operation: clicking and navigation
    {"use_single_tap", "ms_op_single_tap"},
    {"use_up_tap", "ms_op_up_tap"},
    {"use_up_tap_prompt", "ms_op_up_tap_prompt"},
    {"use_raw_click", "ms_op_raw_click"},
    {"use_up_long_click", "ms_op_up_long_click"},
    {"long_click", "ms_op_long_click"},
    {"use_loop_move", "ms_op_loop_move"},
    {"use_pager_scroll", "ms_op_pager_scroll"},
    {"use_move_window", "ms_op_move_window"},
    {"use_keep_type_move", "ms_op_keep_type_move"},
    {"use_auto_view_type", "ms_op_auto_view_type"},
    {"back_break_auto_mode", "ms_op_back_break_auto_mode"},
    {"use_fast_node", "ms_op_fast_node"},
    // Operation: volume keys, headset, shake
    {"use_key_shortcut", "ms_op_key_shortcut"},
    {"show_volume_ui", "ms_op_show_volume_ui"},
    {"use_volume_key_move_text", "ms_op_volume_key_move_text"},
    {"use_headset_key", "ms_op_headset_key"},
    {"use_multi_hot_key", "ms_op_multi_hot_key"},
    {"shake_action_sens", "ms_op_shake_action_sens"},
    {"shake_action_call", "ms_op_shake_action_call"},
    // Notifications and calls
    {"notification_toast_speak", "ms_notif_read_toast"},
    {"notification_summary", "ms_notif_summary_only"},
    {"notification_app_name_speak", "ms_notif_read_source"},
    {"notification_in_touch_no_speak", "ms_notif_no_read_touching"},
    {"notification_in_call_no_speak", "ms_notif_no_read_in_call"},
    {"use_speak_call_phone", "ms_rd_call_announce"},
    {"use_speak_call_time", "ms_rd_call_time"},
    {"use_ringtone_volume", "ms_rd_call_ringvol"},
    // Reading
    {"use_speak_changed_volume", "ms_rd_volume"},
    {"use_window_title_changed", "ms_rd_window_title"},
    {"use_scroll_list_content", "ms_rd_list_scroll_content"},
    {"use_list_item_index", "ms_rd_list_allow"},
    {"use_custom_dict_speak", "ms_rd_dict_use"},
    {"use_custom_dict_regex", "ms_rd_dict_regex"},
    {"use_small_filter", "ms_rd_filter_small"},
    // Second voice
    {"async_audio_focus", "ms_async_tts_audio_focus"},
    {"async_proixmity_sensor", "ms_async_tts_proximity"},
    {"async_touch_stop", "ms_async_tts_touch_stop"},
    // Timed announcement
    {"use_alarm", "ms_adv_timer_enabled"},
    {"alarm_interval", "ms_adv_timer_interval"},
    {"alarm_start_hour", "ms_adv_timer_from"},
    {"alarm_end_hour", "ms_adv_timer_to"},
  };

  /** Jieshuo action names (volume keys, shake) -> MS action values. Names without an equal are absent. */
  private static final Map<String, String> ACTIONS = new HashMap<>();

  static {
    ACTIONS.put("no command", "nothing");
    ACTIONS.put("voice assistant", "assistant");
    ACTIONS.put("phone assistant", "system_assistant");
    ACTIONS.put("open the notification panel", "notifications");
    ACTIONS.put("notification bar", "notifications");
    ACTIONS.put("notification panel", "notifications");
    ACTIONS.put("quick settings", "quick");
    ACTIONS.put("home screen", "home");
    ACTIONS.put("go back", "back");
    ACTIONS.put("recent apps", "recents");
  }

  /** Jieshuo key -> MS key whose value is an action name that needs {@link #ACTIONS}. */
  private static final String[][] ACTION_KEYS = {
    {"up_volume_key", "ms_op_up_volume_key"},
    {"down_volume_key", "ms_op_down_volume_key"},
    {"up_volume_key_off", "ms_op_up_volume_key_off"},
    {"down_volume_key_off", "ms_op_down_volume_key_off"},
    {"up_volume_2_key", "ms_op_up_volume_2_key"},
    {"up_volume_3_key", "ms_op_up_volume_3_key"},
    {"down_volume_2_key", "ms_op_down_volume_2_key"},
    {"down_volume_3_key", "ms_op_down_volume_3_key"},
    {"up_down_volume_short_key", "ms_op_up_down_volume_short_key"},
    {"shake_action", "ms_op_shake_action"},
  };

  /** Text engine settings: the value is the package name of the engine (checked for being installed). */
  static final String[][] ENGINE_KEYS = {
    {"tts_engine", "ms_tts_engine"},
    {"async_tts_engine", "ms_async_tts_engine"},
  };

  /** Lets the Android side say which engine packages are installed. */
  interface Installed {
    boolean has(String packageName);
  }

  /** Turns a Jieshuo function name ("Go back") into MS's function id ("command_to_back"), or null. */
  interface Commands {
    String idOf(String name);
  }

  /** Jieshuo settings that are not settings of MS at all: they are handled by the gesture restore or
   *  have no equal; listed in the report as "handled elsewhere". */
  private static final Set<String> ELSEWHERE =
      new HashSet<>(
          java.util.Arrays.asList(
              "gesture_package", "app_gesture_package", "hot_key_package", "app_hot_key_package",
              "default_gesture_type"));

  static boolean isPrivate(String jxKey) {
    return PRIVATE.matcher(jxKey).matches();
  }

  static Plan plan(Map<String, Object> jx, Installed installed, Commands commands) {
    Plan p = new Plan();
    Set<String> handled = new HashSet<>();
    for (String[] r : SAME_MEANING) {
      handled.add(r[0]);
      if (jx.containsKey(r[0])) {
        put(p, r[0], r[1], jx.get(r[0]), commands);
      }
    }
    for (String[] r : ACTION_KEYS) {
      handled.add(r[0]);
      Object v = jx.get(r[0]);
      if (v == null) {
        continue;
      }
      String ms = ACTIONS.get(String.valueOf(v).trim().toLowerCase(java.util.Locale.ROOT));
      if (ms == null) {
        p.rejected.put(r[0], "\"" + v + "\" has no equal action in MS; MS keeps its own");
      } else {
        put(p, r[0], r[1], ms, commands);
      }
    }
    for (String[] r : ENGINE_KEYS) {
      handled.add(r[0]);
      Object v = jx.get(r[0]);
      if (v instanceof String && !((String) v).isEmpty()) {
        if (installed != null && !installed.has((String) v)) {
          p.rejected.put(r[0], "the voice engine " + v + " is not installed on this phone");
        } else {
          put(p, r[0], r[1], v, commands);
        }
      }
    }
    for (Map.Entry<String, Object> e : jx.entrySet()) {
      String k = e.getKey();
      if (handled.contains(k)) {
        continue;
      }
      if (isPrivate(k)) {
        p.privateSkipped++;
        continue;
      }
      if (ELSEWHERE.contains(k)) {
        p.elsewhere.add(k);
        continue;
      }
      String ms = "ms_jx_" + k;
      if (MsPrefCatalog.has(ms)) {
        put(p, k, ms, e.getValue(), commands);
      } else {
        p.noMatch.add(k);
      }
    }
    return p;
  }

  /** Checks type and list values; records the value or the reason. */
  private static void put(
      Plan p, String jxKey, String msKey, Object value, Commands commands) {
    String type = MsPrefCatalog.type(msKey);
    if (type == null) {
      p.noMatch.add(jxKey);
      return;
    }
    Object out;
    if (type.equals("b")) {
      if (!(value instanceof Boolean)) {
        p.rejected.put(jxKey, "is not an on/off value");
        return;
      }
      out = value;
    } else if (type.equals("s")) {
      if (value instanceof Boolean || value instanceof Set) {
        p.rejected.put(jxKey, "is not a text/number value");
        return;
      }
      out = String.valueOf(value);
    } else {
      if (!(value instanceof Set)) {
        p.rejected.put(jxKey, "is not a list of choices");
        return;
      }
      Set<String> s = new java.util.LinkedHashSet<>();
      for (Object o : (Set<?>) value) {
        s.add(String.valueOf(o));
      }
      out = s;
    }
    Set<String> allowed = MsPrefCatalog.allowed(msKey);
    if (allowed != null && out instanceof String && allowed.contains("command_nothing")
        && !allowed.contains(out) && commands != null) {
      // A list of MS functions: Jieshuo saved the function by its name.
      String name = ((String) out).trim();
      String id = commands.idOf(name);
      if (id == null && name.startsWith("[") && name.endsWith("]")) {
        id = commands.idOf(name.substring(1, name.length() - 1));
      }
      if (id != null && allowed.contains(id)) {
        out = id;
      }
    }
    if (allowed != null) {
      if (out instanceof String && !allowed.contains(out)) {
        p.rejected.put(jxKey, "value \"" + out + "\" is not one of the choices in MS");
        return;
      }
      if (out instanceof Set) {
        Set<String> keep = new java.util.LinkedHashSet<>();
        for (Object o : (Set<?>) out) {
          if (allowed.contains(o)) {
            keep.add((String) o);
          }
        }
        if (keep.isEmpty() && !((Set<?>) out).isEmpty()) {
          p.rejected.put(jxKey, "none of its choices exist in MS");
          return;
        }
        out = keep;
      }
    }
    p.apply.put(msKey, out);
    p.from.put(jxKey, msKey);
  }
}
