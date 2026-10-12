LOCAL_PATH := $(call my-dir)

# Native Lua module "regex" (source: AndroLua_pro, MIT). Loaded by require "regex" via package.cpath = nativeLibraryDir/lib?.so
include $(CLEAR_VARS)
LOCAL_MODULE := regex
LOCAL_CFLAGS := -std=gnu99 -Wno-everything
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../lua
LOCAL_SRC_FILES := lposix.c common.c
LOCAL_LDLIBS := -llog -ldl
LOCAL_LDFLAGS += -Wl,-z,max-page-size=16384
LOCAL_SHARED_LIBRARIES := luajava
include $(BUILD_SHARED_LIBRARY)
