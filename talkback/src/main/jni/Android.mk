# AndroLua engine (Lua 5.3 + LuaJava) for the MS Screen Reader. lua must come before luajava.
# Native Lua modules (bson, cjson, crypt, regex) come after luajava because they link against it.
# Each included makefile resets LOCAL_PATH, so remember the folder of this file first.
JNI_ROOT := $(call my-dir)
include $(JNI_ROOT)/lua/Android.mk
include $(JNI_ROOT)/luajava/Android.mk
include $(JNI_ROOT)/bson/Android.mk
include $(JNI_ROOT)/cjson/Android.mk
include $(JNI_ROOT)/crypt/Android.mk
include $(JNI_ROOT)/regex/Android.mk
