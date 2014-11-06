#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>

#include <rsEnv.h>
#include "rsDispatch.h"
#define LOG_API(...)

void rsdAllocationSetSurface(RsContext rsc, RsAllocation alloc, ANativeWindow *nw);
void rsdAllocationIoSend(RsContext rsc, RsAllocation alloc);

extern "C" void AllocationSetSurface(JNIEnv *_env, jobject _this, RsContext con, RsAllocation alloc, jobject sur, bool useNative, dispatchTable dispatchTab)
{
    LOG_API("nAllocationSetSurface, con(%p), alloc(%p), surface(%p)",
            con, alloc, sur);

    ANativeWindow* s;
    if (sur != 0) {
        s = ANativeWindow_fromSurface(_env, sur);
    }
    if (useNative) {
        dispatchTab.AllocationSetSurface(con, alloc, s);
    } else {
        rsdAllocationSetSurface(con, alloc, s);
    }

}

extern "C" void AllocationIoSend(RsContext con, RsAllocation alloc, bool useNative, dispatchTable dispatchTab)
{
    LOG_API("nAllocationIoSend, con(%p), alloc(%p)", con, alloc);
    if (useNative) {
        dispatchTab.AllocationIoSend(con, alloc);
    } else {
        rsdAllocationIoSend(con, alloc);
    }
}

