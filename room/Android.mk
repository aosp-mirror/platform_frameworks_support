LOCAL_PATH := $(call my-dir)

# Create references to prebuilt libraries.
include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
    room-gson:../../../prebuilts/tools/common/m2/repository/com/google/code/gson/gson/2.3/gson-2.3.jar \
    room-jetbrains-annotations:../../../prebuilts/gradle-plugin/org/jetbrains/annotations/13.0/annotations-13.0.jar \
 
include $(BUILD_HOST_PREBUILT)

# Enumerate target prebuilts to avoid linker warnings like
# Dialer (java:sdk) should not link to dialer-guava (java:platform)
include $(CLEAR_VARS)

LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_MODULE := room-gson-target
LOCAL_SDK_VERSION := current
LOCAL_SRC_FILES := ../../../prebuilts/tools/common/m2/repository/com/google/code/gson/gson/2.3/gson-2.3.jar
LOCAL_UNINSTALLABLE_MODULE := true

include $(BUILD_PREBUILT)

include $(CLEAR_VARS)

LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_MODULE := room-jetbrains-annotations-target
LOCAL_SDK_VERSION := current
LOCAL_SRC_FILES := ../../../prebuilts/gradle-plugin/org/jetbrains/annotations/13.0/annotations-13.0.jar
LOCAL_UNINSTALLABLE_MODULE := true

include $(BUILD_PREBUILT)

# These are the targets we actually care about...
include $(CLEAR_VARS)

LOCAL_MODULE := room-common
LOCAL_MODULE_TAGS := optional
LOCAL_SDK_VERSION := current

LOCAL_SRC_FILES := $(call all-java-files-under, common/src/main)

LOCAL_STATIC_ANDROID_LIBRARIES := android-support-v4 \

include $(BUILD_STATIC_JAVA_LIBRARY)
include $(CLEAR_VARS)

LOCAL_MODULE := room-compiler
LOCAL_MODULE_TAGS := optional
LOCAL_SDK_VERSION := current

LOCAL_SRC_FILES := $(call all-java-files-under, compiler/src/main/)

LOCAL_STATIC_ANDROID_LIBRARIES := android-support-v4 \

include $(BUILD_STATIC_JAVA_LIBRARY)
include $(CLEAR_VARS)

LOCAL_MODULE := room-migration
LOCAL_MODULE_TAGS := optional
LOCAL_SDK_VERSION := current

LOCAL_SRC_FILES := $(call all-java-files-under, migration/src/main)

LOCAL_STATIC_ANDROID_LIBRARIES := android-support-v4 \

LOCAL_JAVA_LIBRARIES := room-gson-target \
  room-jetbrains-annotations-target \

include $(BUILD_STATIC_JAVA_LIBRARY)
include $(CLEAR_VARS)

LOCAL_MODULE := room-runtime
LOCAL_MODULE_TAGS := optional
LOCAL_SDK_VERSION := current

LOCAL_SRC_FILES := $(call all-java-files-under, runtime/src/main/)

LOCAL_STATIC_ANDROID_LIBRARIES := android-support-v4 \
  room-common \
  persistence-db \
  persistence-db-framework \
  paging-common \
  app-toolkit-runtime \
  app-toolkit-common \

include $(BUILD_STATIC_JAVA_LIBRARY)
include $(CLEAR_VARS)
