#include <jni.h>

extern "C" JNIEXPORT
jint JNICALL Java_androidx_os_MemoryFile_getOne(JNIEnv* env, jobject clazz) {
    return (jint) 1;
}
