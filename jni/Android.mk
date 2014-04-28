LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

OPENCV_LIB_TYPE        := SHARED
OPENCV_INSTALL_MODULES := on
OPENCV_CAMERA_MODULES  := on

OPENCV_MK_PATH := ~/Developer/OpenCV-2.4.8-android-sdk/sdk/native/jni

include $(OPENCV_MK_PATH)/OpenCV.mk

#LOCAL_MODULE    := SD
#LOCAL_SRC_FILES := SimpsonDetection.cpp
#LOCAL_LDLIBS    +=  -llog -ldl

#include $(BUILD_SHARED_LIBRARY)
