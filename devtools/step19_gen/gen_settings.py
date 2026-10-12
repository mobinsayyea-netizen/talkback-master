#!/usr/bin/env python3
"""Step 19: builds "Operation settings" and "Advanced settings" like Jieshuo's.
Reads Jieshuo's own res/xml/action_setting.xml + advanced_setting.xml (from the cSr apktool folder),
writes res/xml/ms_jx_*.xml, res/values/ms_jx_arrays.xml, one Java fragment per screen.
Usage: python3 gen_settings.py <Jieshuo res dir> <repo root>
Already-wired items keep their old ms_op_* / ms_adv_* keys so their code keeps working.
"""
import re, sys, os, glob
import xml.etree.ElementTree as ET
from xml.sax.saxutils import escape

JRES, REPO = sys.argv[1], sys.argv[2]
MAIN = os.path.join(REPO, 'talkback/src/main')
RES = os.path.join(MAIN, 'res')
JAVA = os.path.join(MAIN, 'java/com/google/android/accessibility/talkback/preference/base')
A = '{http://schemas.android.com/apk/res/android}'
PREF = 'com.google.android.accessibility.material.preference.AccessibilitySuitePreference'
SWITCH = 'com.google.android.accessibility.material.preference.AccessibilitySuiteSwitchPreference'
LIST = 'com.google.android.accessibility.material.preference.AccessibilitySuiteListPreference'
PKG = 'com.mobeen.msscreenreader'
FRAG = 'com.google.android.accessibility.talkback.preference.base.'
ACT = 'com.google.android.accessibility.talkback.'
NOTYET = 'Saved. Not active yet.'
NOTAV = 'Not available yet'

S = {}
for m in re.finditer(r'<string name="([^"]+)"[^>]*>(.*?)</string>', open(JRES + '/values/strings.xml', encoding='utf-8').read(), re.S):
    S[m.group(1)] = m.group(2).replace('\\n', ' ').replace("\\'", "'").replace('\\"', '"').strip()

def res(v):
    return S.get(v[8:], v) if v and v.startswith('@string/') else v

# ---- Jieshuo command list (index | id | english name)
CMDS = []
for l in open(os.path.join(REPO, 'JX_EXECUTE_COMMANDS.txt'), encoding='utf-8'):
    m = re.match(r'^(\d+) \| (\S+) \| (.*)$', l.rstrip('\n'))
    if m:
        CMDS.append((m.group(2), m.group(3).strip()))
NAME2ID = {n: i for i, n in CMDS}
ID_SET = {i for i, _ in CMDS}

# ---- repo's old operation prefs (entries/default of the ones already wired)
OLD = {}
old = open(os.path.join(RES, 'xml/ms_operation_preferences.xml'), encoding='utf-8').read()
for m in re.finditer(r'<[\w.]+ ([^>]*?)/?>', old):
    a = dict(re.findall(r'android:(\w+)="([^"]*)"', m.group(1)))
    if 'key' in a:
        OLD[a['key']] = a
WIRED = {'key_shortcut','show_volume_ui','headset_key','multi_hot_key','up_down_volume_short_key','up_volume_key','down_volume_key',
         'up_volume_key_off','down_volume_key_off','up_volume_2_key','up_volume_3_key','down_volume_2_key','down_volume_3_key',
         'shake_action','shake_action_sens','shake_action_call','loop_move','single_tap','up_tap','up_long_click','long_click'}
# Jieshuo key -> old key suffix (ms_op_<suffix>)
OPMAP = {'use_fast_node':'fast_node','use_loop_move':'loop_move','use_loop_move_space':'loop_move_warn','use_pager_scroll':'pager_scroll',
 'use_move_window':'move_window','use_keep_type_move':'keep_type_move','use_auto_view_type':'auto_view_type','use_single_tap':'single_tap',
 'use_up_tap':'up_tap','use_up_tap_prompt':'up_tap_prompt','use_raw_click':'raw_click','use_up_long_click':'up_long_click','long_click':'long_click',
 'use_key_shortcut':'key_shortcut','show_volume_ui':'show_volume_ui','use_volume_key_move_text':'volume_key_move_text','use_headset_key':'headset_key',
 'use_multi_hot_key':'multi_hot_key','up_down_volume_short_key':'up_down_volume_short_key','up_volume_key':'up_volume_key','down_volume_key':'down_volume_key',
 'up_volume_key_off':'up_volume_key_off','down_volume_key_off':'down_volume_key_off','up_volume_2_key':'up_volume_2_key','up_volume_3_key':'up_volume_3_key',
 'down_volume_2_key':'down_volume_2_key','down_volume_3_key':'down_volume_3_key','shake_action':'shake_action','shake_action_sens':'shake_action_sens',
 'shake_action_call':'shake_action_call','back_break_auto_mode':'back_break_auto_mode'}
WIRED_J = {k for k, v in OPMAP.items() if v in WIRED}

# ---- activities that exist in the repo: Jieshuo key -> (kind, target)
LINKS = {
 'custom_gesture_settings': ('act', 'editor.ExtensionGesturesActivity', 'Run an extension, tool or command with a swipe'),
 'gesture_settings': ('act', 'editor.AppGestureSchemesActivity', 'Create and manage gesture schemes of single apps'),
 'app_gesture_package': ('act', 'editor.AppGestureSchemesActivity', 'Long press a scheme to turn it on or off'),
 'manager': ('act', 'editor.ResourceManagerActivity', None),
 'main_menu_items': ('frag', 'CustomizeMenusFragment', None),
 'menu_items': ('frag', 'CustomizeMenusFragment', None),
 'accessibility_focus_appearance': ('frag', 'FocusIndicatorPrefFragment', None),
 'backup_settings': ('act', 'backup.SettingsBackupActivity', None),
 'restore_settings': ('act', 'backup.SettingsBackupActivity', None),
 'export_all_file': ('act', 'editor.ResourceManagerActivity', 'Save extensions and tools to a zip file'),
 'show_download': ('act', 'updater.CheckForUpdatesActivity', None),
}
TITLE_OVERRIDE = {'show_download': 'Check for updates'}
# Jieshuo key -> existing key reused (wired or not) with that pref's own widget
REUSE_SWITCH = {'setup_plugin': ('ms_ext_autostart', 'true', 'Run extensions that listen for service_started when the screen reader starts'),
                'use_show_text': ('@string/pref_tts_overlay_key', 'false', None)}
APP_LISTS = {'game_mode', 'app_no_key_shortcut', 'app_gesture_package_unused'}

ARRAYS = {}  # name -> (entries, values)
def add_array(name, entries, values=None):
    ARRAYS['ms_jx_' + name] = (entries, values if values is not None else entries)
add_array('cmd', [n for _, n in CMDS], [i for i, _ in CMDS])
add_array('touch_delay', ['150', '200', '250', '300'])
add_array('width4', ['1', '2', '3', '4'])
add_array('edge_width', ['Small', 'Medium', 'Big', 'Maximum'], ['8', '12', '16', '20'])
add_array('fling_time', ['300', '400', '500', '600', '800', '1000'])
add_array('one', ['Default'], ['default'])
add_array('ocr_engine', ['Tencent cloud OCR', 'Baidu OCR', 'Custom OCR extensions', 'Offline OCR'], ['6', '2', '3', '4'])
add_array('ocr_level', ['1', '2', '3', '4', '5'])
add_array('subtitle', ['Ask every time'], ['-1'])
lang = S.get('ocr_lang_enteries', '').split('|')
add_array('ocr_lang', lang, ['auto' if x == 'Automatic' else x.lower().replace(' ', '_') for x in lang])
add_array('web_types', [x for x in re.findall(r'<item>(.*?)</item>', re.search(r'name="web_type_title".*?</string-array>|name="web_type_title".*?</array>', open(JRES + '/values/arrays.xml', encoding='utf-8').read(), re.S).group(0))])
add_array('view_types', [x for x in re.findall(r'<item>(.*?)</item>', re.search(r'name="view_type_title".*?</string-array>|name="view_type_title".*?</array>', open(JRES + '/values/arrays.xml', encoding='utf-8').read(), re.S).group(0))])
for k in ('web_types', 'view_types'):
    e, v = ARRAYS['ms_jx_' + k]
    ARRAYS['ms_jx_' + k] = ([res(x) for x in e], [res(x) for x in e])

FILES = {}      # screen id -> (title, [xml lines])
FRAGS = {}      # screen id -> title
def ident(s):
    return re.sub(r'[^a-z0-9]+', '_', s.lower()).strip('_')
def cls(sid):
    return 'MsJx' + ''.join(p.capitalize() for p in sid.split('_')) + 'Fragment'
def attr(s):
    return escape(s, {'"': '&quot;'})
def line(tag, **kw):
    return '  <%s %s />' % (tag, ' '.join('android:%s="%s"' % (k, attr(str(v))) for k, v in kw.items()))
def intent_pref(key, title, target, summary=None, extra_fragment=None):
    s = '  <%s android:key="%s" android:title="%s"%s>\n    <intent android:targetPackage="%s" android:targetClass="%s"/>\n  </%s>' % (
        PREF, key, attr(title), (' android:summary="%s"' % attr(summary)) if summary else '', PKG, ACT + target, PREF)
    return s
def default_cmd(raw):
    v = res(raw) if raw else None
    if v in ID_SET: return v
    if v in NAME2ID: return NAME2ID[v]
    return 'command_nothing'

def build(el, sid, title):
    lines = []
    for c in el:
        tag = c.tag.split('.')[-1]
        if tag == 'intent': continue
        jkey = res(c.get(A + 'key')) or ''
        t = TITLE_OVERRIDE.get(jkey) or res(c.get(A + 'title')) or ''
        dv = c.get(A + 'defaultValue')
        if tag == 'PreferenceScreen':
            sub = sid + '_' + (ident(jkey) if jkey and jkey not in ('', '@string/') else ident(t))
            if jkey == 'double_fling': sub = sid + '_multipart'
            build_screen(c, sub, t)
            lines.append(line(PREF, key='ms_jx_scr_' + sub, title=t, fragment=FRAG + cls(sub)))
            continue
        if jkey in LINKS:
            kind, target, summ = LINKS[jkey]
            if kind == 'act':
                lines.append(intent_pref('ms_jx_' + jkey, t, target, summ))
            else:
                lines.append(line(PREF, key='ms_jx_' + jkey, title=t, fragment=FRAG + target))
            continue
        if jkey in REUSE_SWITCH:
            k, d, summ = REUSE_SWITCH[jkey]
            kw = dict(key=k, title=t, defaultValue=d)
            if summ: kw['summary'] = summ
            lines.append(line(SWITCH, **kw))
            continue
        if tag == 'CheckBoxPreference':
            key = ('ms_op_' + OPMAP[jkey]) if jkey in OPMAP else 'ms_jx_' + jkey
            kw = dict(key=key, title=t, defaultValue=(dv if dv in ('true', 'false') else 'false'))
            if jkey not in WIRED_J: kw['summary'] = NOTYET
            lines.append(line(SWITCH, **kw))
        elif tag == 'ListPreference':
            if jkey in OPMAP and ('ms_op_' + OPMAP[jkey]) in OLD:
                o = OLD['ms_op_' + OPMAP[jkey]]
                kw = dict(key=o['key'], title=t, dialogTitle=t, entries=o['entries'], entryValues=o['entryValues'], defaultValue=o.get('defaultValue', ''), summary='%s')
                lines.append(line(LIST, **kw)); continue
            if jkey in ('touch_focus_time_out', 'double_tap_time_out'): arr, d = 'touch_delay', (dv if dv in ('150','200','250','300') else '200')
            elif jkey == 'navigation_gesture_width': arr, d = 'width4', (dv or '2')
            elif jkey == 'edge_gesture_width': arr, d = 'edge_width', (dv or '8')
            elif jkey == 'double_fling_time': arr, d = 'fling_time', '500'
            elif jkey == 'ocr_engine': arr, d = 'ocr_engine', '6'
            elif jkey == 'ocr_level': arr, d = 'ocr_level', '5'
            elif jkey == 'ocr_lang': arr, d = 'ocr_lang', 'auto'
            elif jkey in ('ocr_portrait_mode', 'ocr_landscape_mode'): arr, d = 'subtitle', '-1'
            elif jkey in ('default_gesture_type', 'gesture_package', 'hot_key_package'): arr, d = 'one', 'default'
            elif jkey == 'long_click': arr, d = 'touch_delay', '200'
            else: arr, d = 'cmd', default_cmd(dv)
            kw = dict(key='ms_jx_' + jkey, title=t, dialogTitle=t, entries='@array/ms_jx_' + arr, entryValues='@array/ms_jx_' + arr + '_values', defaultValue=d, summary='%s')
            lines.append(line(LIST, **kw))
        elif tag == 'MultiSelectListPreference':
            arr = {'web_type_move': 'web_types', 'view_type_move': 'view_types'}.get(jkey)
            kw = dict(key='ms_jx_' + jkey, title=t, dialogTitle=t)
            if arr: kw.update(entries='@array/ms_jx_' + arr, entryValues='@array/ms_jx_' + arr + '_values')
            lines.append(line('MultiSelectListPreference', **kw))
        elif tag == 'EditTextPreference':
            lines.append(line('EditTextPreference', key='ms_jx_' + jkey, title=t, dialogTitle=t, defaultValue=dv or '', summary=NOTYET))
        else:  # plain Preference we cannot open yet, or a Jieshuo-only link
            if jkey in ('next_chapter', 'previous_chapter'):
                lines.append(line('EditTextPreference', key='ms_jx_' + jkey, title=t, dialogTitle=t, defaultValue='', summary=NOTYET))
            else:
                lines.append(line(PREF, key='ms_jx_' + (jkey or ident(t)), title=t, summary=NOTAV, enabled='false'))
    return lines

def build_screen(el, sid, title):
    FRAGS[sid] = title
    FILES[sid] = (title, build(el, sid, title))

# ============ Operation settings
op = ET.parse(os.path.join(JRES, 'xml/action_setting.xml')).getroot()
build_screen(op, 'op', 'Operation settings')
# ============ Advanced settings
adv = ET.parse(os.path.join(JRES, 'xml/advanced_setting.xml')).getroot()
build_screen(adv, 'adv', 'Advanced settings')

# ---- hand-made replacements for Jieshuo screens that map to MS features
def replace_line(sid, jtitle, newline):
    lines = FILES[sid][1]
    for i, l in enumerate(lines):
        if ('android:title="%s"' % attr(jtitle)) in l:
            lines[i] = newline; return
    raise SystemExit('not found: ' + jtitle)

# Voice assistant and translation settings (activity in Jieshuo)
FRAGS['adv_voice'] = 'Voice assistant and translation settings'
FILES['adv_voice'] = (FRAGS['adv_voice'], [
 intent_pref('ms_jx_gemini_key', 'Gemini: key and model', 'actor.gemini.GeminiSettingsActivity', 'Used for image, screen and video descriptions'),
 line(PREF, key='ms_jx_gemini_cmd', title='Gemini voice command', fragment=FRAG + 'GeminiSettingsFragment'),
 intent_pref('ms_jx_translate', 'Translate and OCR', 'translate.TranslateSettingsActivity', 'Text reading, languages and language packs')])
replace_line('adv', 'Voice assistant and translation settings', line(PREF, key='ms_jx_scr_adv_voice', title=FRAGS['adv_voice'], fragment=FRAG + cls('adv_voice')))
# Timer settings = our time announcements (already working)
FRAGS['adv_timer'] = 'Timer settings'
adv_old = open(os.path.join(RES, 'xml/ms_advanced_preferences.xml'), encoding='utf-8').read()
body = re.findall(r'(  <com\.google[^\n]*?PreferenceCategory.*?</com\.google[^\n]*?PreferenceCategory>)', adv_old, re.S)
FILES['adv_timer'] = ('Timer settings', ['\n'.join(b for b in body if 'ms_adv_cat_power' not in b)])
replace_line('adv', 'Timer settings', line(PREF, key='ms_jx_scr_adv_timer', title='Timer settings', fragment=FRAG + cls('adv_timer')))
# Backup and restore: the generated screen is fine (links patched through LINKS); cloud items stay "not available"
# Power management: keep the battery optimisation button we already had
pl = FILES['adv_power_management_settings'] if 'adv_power_management_settings' in FILES else None
for sid in list(FILES):
    if sid.endswith('power_management_settings'):
        FILES[sid][1].append('  <%s android:key="ms_jx_battery_opt" android:title="Battery optimization" android:summary="Ask Android not to stop the screen reader">\n    <intent android:action="android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS"/>\n  </%s>' % (PREF, PREF))
# Other settings screen of Advanced: add the old "check permissions" link at the end
for sid in list(FILES):
    if sid.startswith('adv_') and sid.endswith('other_settings') and 'adv_other' in sid:
        FILES[sid][1].append(intent_pref('ms_jx_check_permissions', 'Check permissions', 'permission.MsPermissionsActivity', 'See which permissions are granted and grant the missing ones'))

# ---- write files
os.makedirs(JAVA, exist_ok=True)
for sid, (title, lines) in FILES.items():
    xml = '<?xml version="1.0" encoding="utf-8"?>\n<!-- Generated by devtools/step19_gen/gen_settings.py from Jieshuo %s. -->\n<PreferenceScreen xmlns:android="http://schemas.android.com/apk/res/android">\n%s\n</PreferenceScreen>\n' % (
        'action_setting.xml' if sid.startswith('op') else 'advanced_setting.xml', '\n'.join(lines))
    open(os.path.join(RES, 'xml/ms_jx_%s.xml' % sid), 'w', encoding='utf-8').write(xml)
    open(os.path.join(JAVA, cls(sid) + '.java'), 'w', encoding='utf-8').write('''/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.preference.base;

import com.google.android.accessibility.talkback.R;

/** %s (generated by devtools/step19_gen/gen_settings.py). */
public class %s extends MsJxBaseFragment {
  public %s() {
    super(R.xml.ms_jx_%s);
  }

  @Override
  public CharSequence getTitle() {
    return "%s";
  }
}
''' % (title, cls(sid), cls(sid), sid, title.replace('"', '\\"')))
# arrays
def aesc(s): return s.replace('&', '&amp;').replace('<', '&lt;').replace("'", "\\'").replace('"', '\\"').replace('@', '\\@')
out = ['<?xml version="1.0" encoding="utf-8"?>\n<!-- Generated by devtools/step19_gen/gen_settings.py -->\n<resources>']
for name, (e, v) in ARRAYS.items():
    out.append('  <string-array name="%s">' % name + ''.join('<item>%s</item>' % aesc(x) for x in e) + '</string-array>')
    out.append('  <string-array name="%s_values">' % name + ''.join('<item>%s</item>' % aesc(x) for x in v) + '</string-array>')
out.append('</resources>\n')
open(os.path.join(RES, 'values/ms_jx_arrays.xml'), 'w', encoding='utf-8').write('\n'.join(out))
# search index: every generated screen goes into the settings search (block between the markers)
SEARCH = os.path.join(MAIN, 'java/com/google/android/accessibility/talkback/settingssearch/MsSettingsSearchActivity.java')
def path_of(sid):
    parts = []; cur = ''
    for seg in sid.split('_'):
        cur = seg if not cur else cur + '_' + seg
        if cur in FRAGS: parts.append(FRAGS[cur])
    return ' > '.join(parts)
block = ['    // BEGIN ms_jx index (generated by devtools/step19_gen/gen_settings.py)']
for sid in sorted(FILES, key=lambda x: (x.split('_')[0], x)):
    block.append('    add(R.xml.ms_jx_%s, "%s", "%s");' % (sid, path_of(sid).replace('"', '\\"'), cls(sid)))
block.append('    // END ms_jx index')
src = open(SEARCH, encoding='utf-8').read()
if '// BEGIN ms_jx index' in src:
    src = re.sub(r'    // BEGIN ms_jx index.*?// END ms_jx index', '\n'.join(block).replace('\\', '\\\\'), src, flags=re.S)
else:
    src = src.replace('    add(R.xml.ms_group_9_other, "Other Settings", "MsGroupOtherFragment");\n', '    add(R.xml.ms_group_9_other, "Other Settings", "MsGroupOtherFragment");\n' + '\n'.join(block) + '\n')
open(SEARCH, 'w', encoding='utf-8').write(src)
print(len(FILES), 'screens;', len(ARRAYS), 'arrays')
for sid in sorted(FILES): print(' ', sid, '->', FILES[sid][0], len(FILES[sid][1]), 'lines')
