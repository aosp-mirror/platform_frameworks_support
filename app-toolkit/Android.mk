LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := app-toolkit-runtime
LOCAL_MODULE_TAGS := optional
LOCAL_SDK_VERSION := current

LOCAL_SRC_FILES := $(call all-java-files-under, runtime/src/main/)

LOCAL_STATIC_ANDROID_LIBRARIES := android-support-v4 \

include $(BUILD_STATIC_JAVA_LIBRARY)
include $(CLEAR_VARS)

LOCAL_MODULE := app-toolkit-common
LOCAL_MODULE_TAGS := optional
LOCAL_SDK_VERSION := current

LOCAL_SRC_FILES := $(call all-java-files-under, common/src/main/)

LOCAL_STATIC_ANDROID_LIBRARIES := android-support-v4 \

include $(BUILD_STATIC_JAVA_LIBRARY)
include $(CLEAR_VARS)
