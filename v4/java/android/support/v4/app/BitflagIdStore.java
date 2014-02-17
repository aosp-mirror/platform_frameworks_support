package android.support.v4.app;

import android.os.Parcel;
import android.os.Parcelable;

public class BitflagIdStore implements Parcelable {
    private int[] mBitFlags;

    public BitflagIdStore() {
        this(1024);
    }

    public BitflagIdStore(int bitCount) {
        int numberOfIntsRequired = bitCount / 32;

        if (bitCount % 32 != 0) {
            throw new IllegalArgumentException("Number of bits must be a multiple of 32");
        }

        mBitFlags = new int[numberOfIntsRequired];
    }

    public int getIdCount() {
        return mBitFlags.length * 32;
    }

    public boolean contains(int id) {
        if (!inRange(id)) {
            return false;
        }

        int flagSetIndex = id / 32;
        int bitIndex = id % 32;

        int flagSet = mBitFlags[flagSetIndex];

        return (flagSet & flagMask(bitIndex)) != 0;
    }

    private static int flagMask(int index) {
        return 1 << index;
    }

    public boolean inRange(int id) {
        return id < getIdCount() && id >= 0;
    }

    public void add(int id) {
        if (!inRange(id)) {
            return;
        }

        int flagSetIndex = id / 32;
        int bitIndex = id % 32;

        mBitFlags[flagSetIndex] |= flagMask(bitIndex);
    }

    public void remove(int id) {
        if (!inRange(id)) {
            return;
        }

        int flagSetIndex = id / 32;
        int bitIndex = id % 32;

        mBitFlags[flagSetIndex] &= ~flagMask(bitIndex);
    }

    public Integer getNextFreeId() {
        for (int flagSetIndex = 0; flagSetIndex < mBitFlags.length; flagSetIndex++) {
            int flagSet = mBitFlags[flagSetIndex];
            if (~flagSet == 0) {
                continue;
            }

            return flagSetIndex * 32 + getNextFreeId(flagSet);
        }

        return null;
    }

    private static int getNextFreeId(int flagSet) {
        int bitIndex = 0;

        if ((flagSet & 0xffff) == 0xffff) {
            bitIndex += 16;
            flagSet >>= 16;
        }

        if ((flagSet & 0xff) == 0xff) {
            bitIndex += 8;
            flagSet >>= 8;
        }

        if ((flagSet & 0xf) == 0xf) {
            bitIndex += 4;
            flagSet >>= 4;
        }

        if ((flagSet & 0x3) == 0x3) {
            bitIndex += 2;
            flagSet >>= 2;
        }

        if ((flagSet & 0x1) == 0x1) {
            bitIndex += 1;
        }

        return bitIndex;
    }

    public static final Parcelable.Creator<BitflagIdStore> CREATOR
            = new Parcelable.Creator<BitflagIdStore>() {
        public BitflagIdStore createFromParcel(Parcel in) {
            return new BitflagIdStore(in);
        }

        public BitflagIdStore[] newArray(int size) {
            return new BitflagIdStore[size];
        }
    };

    public BitflagIdStore(Parcel in) {
        mBitFlags = in.createIntArray();
    }

        @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeIntArray(mBitFlags);
    }
}
