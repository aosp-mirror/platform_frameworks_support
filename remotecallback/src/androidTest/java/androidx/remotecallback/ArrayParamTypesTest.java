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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ArrayParamTypesTest {

    private final Context mContext = InstrumentationRegistry.getContext();

    private static Object sParam;
    private static CountDownLatch sLatch;

    @Test
    public void testByte() throws Exception {
        run(new ArrayTypesReceiver().createRemoteCallback().setByte(new byte[] {(byte) 5})
                .toRemoteCallback(mContext));
        assertArrayEquals(new byte[]{(byte) 5}, (byte[]) sParam);

        run(new ArrayTypesReceiver().createRemoteCallback().setByte(null)
                .toRemoteCallback(mContext));
        assertEquals(null, sParam);
    }

    @Test
    public void testChar() throws Exception {
        run(new ArrayTypesReceiver().createRemoteCallback().setChar(new char[] {(char) 5})
                .toRemoteCallback(mContext));
        assertArrayEquals(new char[]{(char) 5}, (char[]) sParam);

        run(new ArrayTypesReceiver().createRemoteCallback().setChar(null)
                .toRemoteCallback(mContext));
        assertEquals(null, sParam);
    }

    @Test
    public void testShort() throws Exception {
        run(new ArrayTypesReceiver().createRemoteCallback().setShort(new short[] {(short) 5})
                .toRemoteCallback(mContext));
        assertArrayEquals(new short[]{(short) 5}, (short[]) sParam);

        run(new ArrayTypesReceiver().createRemoteCallback().setShort(null)
                .toRemoteCallback(mContext));
        assertEquals(null, sParam);
    }

    @Test
    public void testInt() throws Exception {
        run(new ArrayTypesReceiver().createRemoteCallback().setInt(new int[] {(int) 5})
                .toRemoteCallback(mContext));
        assertArrayEquals(new int[]{(int) 5}, (int[]) sParam);

        run(new ArrayTypesReceiver().createRemoteCallback().setInt(null)
                .toRemoteCallback(mContext));
        assertEquals(null, sParam);
    }

    @Test
    public void testLong() throws Exception {
        run(new ArrayTypesReceiver().createRemoteCallback().setLong(new long[] {(long) 5})
                .toRemoteCallback(mContext));
        assertArrayEquals(new long[]{(long) 5}, (long[]) sParam);

        run(new ArrayTypesReceiver().createRemoteCallback().setLong(null)
                .toRemoteCallback(mContext));
        assertEquals(null, sParam);
    }

    @Test
    public void testFloat() throws Exception {
        run(new ArrayTypesReceiver().createRemoteCallback().setFloat(new float[] {(float) 5.5f})
                .toRemoteCallback(mContext));
        assertArrayEquals(new float[]{(float) 5.5f}, (float[]) sParam, .1f);

        run(new ArrayTypesReceiver().createRemoteCallback().setFloat(null)
                .toRemoteCallback(mContext));
        assertEquals(null, sParam);
    }

    @Test
    public void testDouble() throws Exception {
        run(new ArrayTypesReceiver().createRemoteCallback().setDouble(new double[] {(double) 5.5})
                .toRemoteCallback(mContext));
        assertArrayEquals(new double[]{(double) 5.5}, (double[]) sParam, .1);

        run(new ArrayTypesReceiver().createRemoteCallback().setDouble(null)
                .toRemoteCallback(mContext));
        assertEquals(null, sParam);
    }

    @Test
    public void testBoolean() throws Exception {
        run(new ArrayTypesReceiver().createRemoteCallback().setBoolean(new boolean[] {true, false})
                .toRemoteCallback(mContext));
        assertArrayEquals(new boolean[]{true, false}, (boolean[]) sParam);

        run(new ArrayTypesReceiver().createRemoteCallback().setBoolean(null)
                .toRemoteCallback(mContext));
        assertEquals(null, sParam);
    }

    @Test
    public void testString() throws Exception {
        run(new ArrayTypesReceiver().createRemoteCallback().setString(new String[] {
                "Some string", null, "other"})
                .toRemoteCallback(mContext));
        assertArrayEquals(new String[]{"Some string", null, "other"}, (String[]) sParam);

        run(new ArrayTypesReceiver().createRemoteCallback().setString(null)
                .toRemoteCallback(mContext));
        assertEquals(null, sParam);
    }

    private void run(RemoteCallback callback)
            throws PendingIntent.CanceledException, InterruptedException {
        sLatch = new CountDownLatch(1);
        sParam = null;
        callback.toPendingIntent().send();
        sLatch.await(2, TimeUnit.SECONDS);
    }

    public static class ArrayTypesReceiver extends
            BroadcastReceiverWithCallbacks<ArrayTypesReceiver> {

        @Override
        public void onReceive(Context context, Intent intent) {
            super.onReceive(context, intent);
            if (sLatch != null) sLatch.countDown();
        }

        @RemoteCallable
        public ArrayTypesReceiver setByte(byte[] i) {
            sParam = i;
            return this;
        }

        @RemoteCallable
        public ArrayTypesReceiver setChar(char[] i) {
            sParam = i;
            return this;
        }

        @RemoteCallable
        public ArrayTypesReceiver setShort(short[] i) {
            sParam = i;
            return this;
        }

        @RemoteCallable
        public ArrayTypesReceiver setInt(int[] i) {
            sParam = i;
            return this;
        }

        @RemoteCallable
        public ArrayTypesReceiver setLong(long[] i) {
            sParam = i;
            return this;
        }

        @RemoteCallable
        public ArrayTypesReceiver setFloat(float[] i) {
            sParam = i;
            return this;
        }

        @RemoteCallable
        public ArrayTypesReceiver setDouble(double[] i) {
            sParam = i;
            return this;
        }

        @RemoteCallable
        public ArrayTypesReceiver setBoolean(boolean[] i) {
            sParam = i;
            return this;
        }

        @RemoteCallable
        public ArrayTypesReceiver setString(String[] i) {
            sParam = i;
            return this;
        }
    }
}
