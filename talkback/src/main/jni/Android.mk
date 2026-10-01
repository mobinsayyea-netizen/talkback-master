# AndroLua engine (Lua 5.3 + LuaJava) for the MS Screen Reader. lua must come before luajava.
LOCAL_PATH := $(call my-dir)
include $(LOCAL_PATH)/lua/Android.mk
include $(LOCAL_PATH)/luajava/Android.mk
