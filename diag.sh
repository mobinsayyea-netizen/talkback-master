#!/bin/bash
PKG=com.mobeen.msscreenreader
SVC=$PKG/com.google.android.marvin.talkback.TalkBackService
exec > diag_log.txt 2>&1
echo "== install"; adb install -r -g MSScreenReader-64bit.apk
adb shell appops set $PKG MANAGE_EXTERNAL_STORAGE allow
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS
adb logcat -c
echo "== launch"; adb shell monkey -p $PKG -c android.intent.category.LAUNCHER 1; sleep 8
echo "== enable service"
adb shell settings put secure enabled_accessibility_services $SVC
adb shell settings put secure accessibility_enabled 1
sleep 40
echo "== process alive?"; adb shell pidof $PKG
echo "== accessibility"; adb shell dumpsys accessibility | grep -E "Bound services|Binding services|Crashed" | head -4
echo "== crash buffer"; adb logcat -b crash -d | head -80
adb root >/dev/null 2>&1; sleep 3
echo "== tombstones"; adb shell 'ls -t /data/tombstones 2>/dev/null | head -5'
adb shell 'f=$(ls -t /data/tombstones/tombstone_* 2>/dev/null | grep -v pb | head -1); [ -n "$f" ] && head -90 $f'
echo "== logcat app/native"; adb logcat -d -v time | grep -E "msscreenreader|DEBUG|libc|Fatal signal|FATAL|UnsatisfiedLink|SIGSEGV|AndroidRuntime" | head -200
