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

package androidx.media2;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.ParcelImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Transfer a large list of {@link ParcelImpl} objects across an IPC. Splits into
 * multiple transactions if needed.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class BaseParceledListSlice implements Parcelable {
    private static final String TAG = "BaseParceledListSlice";
    private static final boolean DEBUG = false;

    /*
     * TODO get this number from somewhere else. For now set it to a quarter of the 1MB limit.
     */
    private static final int MAX_IPC_SIZE = 64 * 1024;

    final List<ParcelImpl> mList;

    private int mInlineCountLimit = Integer.MAX_VALUE;

    public BaseParceledListSlice(List<ParcelImpl> list) {
        mList = list;
    }

    BaseParceledListSlice(Parcel p) {
        final int itemCount = p.readInt();
        mList = new ArrayList<>(itemCount);
        if (DEBUG) {
            Log.d(TAG, "Retrieving " + itemCount + " items");
        }
        if (itemCount <= 0) {
            return;
        }

        int i = 0;
        while (i < itemCount) {
            if (p.readInt() == 0) {
                break;
            }

            final ParcelImpl parcelImpl = p.readParcelable(ParcelImpl.class.getClassLoader());
            mList.add(parcelImpl);

            if (DEBUG) {
                Log.d(TAG, "Read inline #" + i + ": " + mList.get(mList.size() - 1));
            }
            i++;
        }
        if (i >= itemCount) {
            return;
        }
        final IBinder retriever = p.readStrongBinder();
        while (i < itemCount) {
            if (DEBUG) {
                Log.d(TAG, "Reading more @" + i + " of " + itemCount + ": retriever=" + retriever);
            }
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            data.writeInt(i);
            try {
                retriever.transact(IBinder.FIRST_CALL_TRANSACTION, data, reply, 0);
            } catch (RemoteException e) {
                Log.w(TAG, "Failure retrieving array; only received " + i + " of " + itemCount, e);
                return;
            }
            while (i < itemCount && reply.readInt() != 0) {
                final ParcelImpl parcelImpl = reply.readParcelable(
                        ParcelImpl.class.getClassLoader());
                mList.add(parcelImpl);

                if (DEBUG) {
                    Log.d(TAG, "Read extra #" + i + ": " + mList.get(mList.size() - 1));
                }
                i++;
            }
            reply.recycle();
            data.recycle();
        }
    }

    public List<ParcelImpl> getList() {
        return mList;
    }

    /**
     * Set a limit on the maximum number of entries in the array that will be included
     * inline in the initial parcelling of this object.
     */
    public void setInlineCountLimit(int maxCount) {
        mInlineCountLimit = maxCount;
    }

    /**
     * Write this to another Parcel. Note that this discards the internal Parcel
     * and should not be used anymore. This is so we can pass this to a Binder
     * where we won't have a chance to call recycle on this.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        final int itemCount = mList.size();
        dest.writeInt(itemCount);
        if (DEBUG) {
            Log.d(TAG, "Writing " + itemCount + " items");
        }
        if (itemCount > 0) {
            int i = 0;
            while (i < itemCount && i < mInlineCountLimit && dest.dataSize() < MAX_IPC_SIZE) {
                dest.writeInt(1);

                final ParcelImpl parcelable = mList.get(i);
                dest.writeParcelable(parcelable, flags);

                if (DEBUG) {
                    Log.d(TAG, "Wrote inline #" + i + ": " + mList.get(i));
                }
                i++;
            }
            if (i < itemCount) {
                dest.writeInt(0);
                Binder retriever = new Binder() {
                    @Override
                    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                            throws RemoteException {
                        if (code != FIRST_CALL_TRANSACTION) {
                            return super.onTransact(code, data, reply, flags);
                        }
                        int i = data.readInt();
                        if (DEBUG) {
                            Log.d(TAG, "Writing more @" + i + " of " + itemCount);
                        }
                        while (i < itemCount && reply.dataSize() < MAX_IPC_SIZE) {
                            reply.writeInt(1);

                            final ParcelImpl parcelable = mList.get(i);
                            reply.writeParcelable(parcelable, flags);

                            if (DEBUG) {
                                Log.d(TAG, "Wrote extra #" + i + ": " + mList.get(i));
                            }
                            i++;
                        }
                        if (i < itemCount) {
                            if (DEBUG) {
                                Log.d(TAG, "Breaking @" + i + " of " + itemCount);
                            }
                            reply.writeInt(0);
                        }
                        return true;
                    }
                };
                if (DEBUG) {
                    Log.d(TAG, "Breaking @" + i + " of " + itemCount + ": retriever=" + retriever);
                }
                dest.writeStrongBinder(retriever);
            }
        }
    }

    @Override
    public int describeContents() {
        int contents = 0;
        final List<ParcelImpl> list = getList();
        for (int i = 0; i < list.size(); i++) {
            contents |= list.get(i).describeContents();
        }
        return contents;
    }

    public static final Parcelable.Creator<BaseParceledListSlice> CREATOR =
            new Parcelable.Creator<BaseParceledListSlice>() {
        @Override
        public BaseParceledListSlice createFromParcel(Parcel in) {
            return new BaseParceledListSlice(in);
        }

        @Override
        public BaseParceledListSlice[] newArray(int size) {
            return new BaseParceledListSlice[size];
        }
    };
}
