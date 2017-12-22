LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := persistence-db
LOCAL_MODULE_TAGS := optional
LOCAL_SDK_VERSION := current

LOCAL_SRC_FILES := $(call all-java-files-under, db/src/main/)

LOCAL_STATIC_ANDROID_LIBRARIES := android-support-v4 \

include $(BUILD_STATIC_JAVA_LIBRARY)
include $(CLEAR_VARS)

LOCAL_MODULE := persistence-db-framework
LOCAL_MODULE_TAGS := optional
LOCAL_SDK_VERSION := current

LOCAL_SRC_FILES := $(call all-java-files-under, db-framework/src/main/)

LOCAL_STATIC_ANDROID_LIBRARIES := android-support-v4 \
  persistence-db \

include $(BUILD_STATIC_JAVA_LIBRARY)
include $(CLEAR_VARS)
