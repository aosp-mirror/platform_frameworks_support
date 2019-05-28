/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.versionedparcelable;

import android.os.Parcel;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

abstract class Backend {
    public abstract VersionedParcel openForWriting();
    public abstract VersionedParcel openForReading(VersionedParcel in);
    public abstract <T extends VersionedParcelable> T writeThenRead(T versionedParcelable);

    public VersionedParcel write(VersionedParcelable versionedParcelable) {
        final VersionedParcel versionedParcel = openForWriting();
        versionedParcel.writeVersionedParcelable(versionedParcelable);
        versionedParcel.closeField();
        return versionedParcel;
    }

    public VersionedParcelable read(VersionedParcel versionedParcel) {
        return openForReading(versionedParcel).readVersionedParcelable();
    }

    static final class ParcelNotFromBackendException extends RuntimeException {
        ParcelNotFromBackendException() {
            super("The supplied VersionedParcel was not created from this backend.");
        }
    }

    static final class Rule extends Backend implements TestRule {
        private Backend mBackend;
        private boolean mUsedThisTest;

        @Override
        public VersionedParcel openForWriting() {
            mUsedThisTest = true;
            return mBackend.openForWriting();
        }

        @Override
        public VersionedParcel openForReading(VersionedParcel in) {
            mUsedThisTest = true;
            return mBackend.openForReading(in);
        }

        @Override
        public VersionedParcel write(VersionedParcelable versionedParcelable) {
            mUsedThisTest = true;
            return mBackend.write(versionedParcelable);
        }

        @Override
        public VersionedParcelable read(VersionedParcel versionedParcel) {
            mUsedThisTest = true;
            return mBackend.read(versionedParcel);
        }

        @Override
        public <T extends VersionedParcelable> T writeThenRead(T versionedParcelable) {
            mUsedThisTest = true;
            return mBackend.writeThenRead(versionedParcelable);
        }

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    mUsedThisTest = false;
                    mBackend = new ParcelBackend();

                    try {
                        base.evaluate();
                    } finally {
                        ((ParcelBackend) mBackend).recycleParcels();
                    }

                    if (mUsedThisTest) {
                        mBackend = new StreamBackend();
                        base.evaluate();
                    }
                }
            };
        }

        private static final class ParcelBackend extends Backend {
            private final Map<VersionedParcel, Parcel> mParcels = new HashMap<>();

            @Override
            public VersionedParcel openForWriting() {
                final Parcel parcel = Parcel.obtain();
                final VersionedParcelParcel versionedParcel = new VersionedParcelParcel(parcel);
                mParcels.put(versionedParcel, parcel);
                return versionedParcel;
            }

            @Override
            public VersionedParcel openForReading(VersionedParcel in) {
                final Parcel parcel = mParcels.get(in);

                if (parcel == null) {
                    throw new ParcelNotFromBackendException();
                } else {
                    in.closeField();
                    parcel.setDataPosition(0);
                    return new VersionedParcelParcel(parcel);
                }
            }

            @Override
            public <T extends VersionedParcelable> T writeThenRead(T versionedParcelable) {
                final Parcel parcel = Parcel.obtain();

                parcel.writeParcelable(ParcelUtils.toParcelable(versionedParcelable), 0);
                parcel.setDataPosition(0);

                final T out = ParcelUtils.fromParcelable(
                        parcel.readParcelable(getClass().getClassLoader()));

                parcel.recycle();

                return out;
            }

            private void recycleParcels() {
                for (Parcel parcel : mParcels.values()) {
                    parcel.recycle();
                }
            }
        }

        private static final class StreamBackend extends Backend {
            private final Map<VersionedParcel, ByteArrayOutputStream> mStreams = new HashMap<>();

            @Override
            public VersionedParcel openForWriting() {
                final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                final VersionedParcelStream versionedParcel =
                        new VersionedParcelStream(null, outputStream);
                mStreams.put(versionedParcel, outputStream);
                return versionedParcel;
            }

            @Override
            public VersionedParcel openForReading(VersionedParcel in) {
                final ByteArrayOutputStream outputStream = mStreams.get(in);

                if (outputStream == null) {
                    throw new ParcelNotFromBackendException();
                } else {
                    in.closeField();
                    final ByteArrayInputStream inputStream =
                            new ByteArrayInputStream(outputStream.toByteArray());
                    return new VersionedParcelStream(inputStream, null);
                }
            }

            @Override
            public <T extends VersionedParcelable> T writeThenRead(T versionedParcelable) {
                final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ParcelUtils.toOutputStream(versionedParcelable, outputStream);

                final ByteArrayInputStream inputStream =
                        new ByteArrayInputStream(outputStream.toByteArray());
                return ParcelUtils.fromInputStream(inputStream);
            }
        }
    }
}
