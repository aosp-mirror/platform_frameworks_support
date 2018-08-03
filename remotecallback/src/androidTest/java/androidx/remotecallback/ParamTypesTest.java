/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.remotecallback;

import static org.junit.Assert.assertEquals;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ParamTypesTest {

    private final Context mContext = InstrumentationRegistry.getContext();

    private static Object sParam;
    private static CountDownLatch sLatch;

    @Test
    public void testByte() throws Exception {
        run(new TypesReceiver().createRemoteCallback(mContext, "setByte", (byte) 5));
        assertEquals((byte) 5, sParam);
    }

    @Test
    public void testByte_Nullable() throws Exception {
        run(new TypesReceiver().createRemoteCallback(mContext, "setByteN", (byte) 5));
        assertEquals((byte) 5, sParam);

        run(new TypesReceiver().createRemoteCallback(mContext, "setByteN", (Byte) null));
        assertEquals(null, sParam);
    }

    private void run(RemoteCallback callback)
            throws PendingIntent.CanceledException, InterruptedException {
        sLatch = new CountDownLatch(1);
        sParam = null;
        callback.toPendingIntent().send();
        sLatch.await(2, TimeUnit.SECONDS);
    }

    public static class TypesReceiver extends BroadcastReceiverWithCallbacks {

        @Override
        public void onReceive(Context context, Intent intent) {
            super.onReceive(context, intent);
            if (sLatch != null) sLatch.countDown();
        }

        @RemoteCallable
        public void setByte(byte i) {
            sParam = i;
        }

        @RemoteCallable
        public void setByteN(Byte i) {
            sParam = i;
        }

        @RemoteCallable
        public void setChar(char i) {
            sParam = i;
        }

        @RemoteCallable
        public void setCharacter(Character i) {
            sParam = i;
        }

        @RemoteCallable
        public void setShort(short i) {
            sParam = i;
        }

        @RemoteCallable
        public void setShortN(Short i) {
            sParam = i;
        }

        @RemoteCallable
        public void setInt(int i) {
            sParam = i;
        }

        @RemoteCallable
        public void setInteger(Integer i) {
            sParam = i;
        }

        @RemoteCallable
        public void setLong(long i) {
            sParam = i;
        }

        @RemoteCallable
        public void setLongN(Long i) {
            sParam = i;
        }

        @RemoteCallable
        public void setFloat(float i) {
            sParam = i;
        }

        @RemoteCallable
        public void setFloatN(Float i) {
            sParam = i;
        }

        @RemoteCallable
        public void setBoolean(boolean i) {
            sParam = i;
        }

        @RemoteCallable
        public void setBooleanN(Boolean i) {
            sParam = i;
        }

        @RemoteCallable
        public void setString(String i) {
            sParam = i;
        }

        @RemoteCallable
        public void setUri(Uri i) {
            sParam = i;
        }
    }
}
