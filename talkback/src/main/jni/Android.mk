# AndroLua engine (Lua 5.3 + LuaJava) for the MS Screen Reader. lua must come before luajava.
# Each included makefile resets LOCAL_PATH, so remember the folder of this file first.
JNI_ROOT := $(call my-dir)
include $(JNI_ROOT)/lua/Android.mk
include $(JNI_ROOT)/luajava/Android.mk
