LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := luajava
LOCAL_CFLAGS := -std=gnu99 -Wno-everything
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../lua
LOCAL_SRC_FILES := luajava.c
LOCAL_LDLIBS := -llog -ldl
LOCAL_LDFLAGS += -Wl,-z,max-page-size=16384
LOCAL_STATIC_LIBRARIES := lua
include $(BUILD_SHARED_LIBRARY)
