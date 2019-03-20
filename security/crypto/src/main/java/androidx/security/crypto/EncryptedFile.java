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

package androidx.security.crypto;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.security.GeneralSecurityException;

/**
 * Class used to create and read encrypted files. Provides implementations
 * of EncryptedFileInput/Output Streams.
 */
public class EncryptedFile {

    private String mFileName;
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;
    private Context mContext;

    MasterKey mMasterKey;
    EncryptedFileKeyset mEncryptedFileKeyset;

    private EncryptedFile(@NonNull String fileName,
            @NonNull MasterKey masterKey,
            @NonNull EncryptedFileKeyset fileKeyset,
            @NonNull Context context)
            throws GeneralSecurityException, IOException {
        mContext = context;
        mFileName = fileName;
        mMasterKey = masterKey;
        mEncryptedFileKeyset = fileKeyset;
        mFileInputStream = new EncryptedFileInputStream(mFileName,
                mContext.openFileInput(mFileName), mContext);
    }

    private EncryptedFile(@NonNull String fileName, int mode,
            @NonNull MasterKey masterKey,
            @NonNull EncryptedFileKeyset fileKeyset,
            @NonNull Context context)
            throws GeneralSecurityException, IOException {
        mContext = context;
        mFileName = fileName;
        mMasterKey = masterKey;
        mEncryptedFileKeyset = fileKeyset;
        mFileOutputStream = new EncryptedFileOutputStream(mFileName,
                mContext.openFileOutput(mFileName, mode), mContext);
    }

    /**
     * Opens a FileOutputStream for writing that automatically encrypts the data based on the
     * provided settings.
     *
     * @param fileName The name of the file to open; can not contain path separators.
     * @param mode Operating mode.
     * @param masterKey The MasterKey
     * @param fileKeyset The File Keyset
     * @param context The Context
     * @return The FileOutputStream that encrypts all data.
     * @throws GeneralSecurityException
     * @throws IOException
     */
    @NonNull
    public static FileOutputStream openEncryptedFileOutput(@NonNull String fileName, int mode,
            @NonNull MasterKey masterKey,
            @NonNull EncryptedFileKeyset fileKeyset,
            @NonNull Context context) throws GeneralSecurityException, IOException {
        return new EncryptedFile(fileName, mode, masterKey, fileKeyset, context)
                .getFileOutputStream();
    }

    /**
     * Opens a FileInputStream that reads encrypted files based on the previous settings.
     *
     * Please ensure that the same MasterKey and EncryptedFileKeyset are  used to decrypt or it
     * will cause failures.
     *
     * @param fileName The name of the file to open; can not contain path separators.
     * @param masterKey The MasterKey
     * @param fileKeyset The File Keyset
     * @param context The Context
     * @return The input stream to read previously encrypted data.
     * @throws GeneralSecurityException
     * @throws IOException
     */
    @NonNull
    public static FileInputStream openEncryptedFileInput(@NonNull String fileName,
            @NonNull MasterKey masterKey,
            @NonNull EncryptedFileKeyset fileKeyset,
            @NonNull Context context) throws GeneralSecurityException, IOException {
        return new EncryptedFile(fileName, masterKey, fileKeyset, context)
                .getFileInputStream();
    }

    /**
     * @return  the file output stream
     */
    @NonNull
    public FileOutputStream getFileOutputStream() {
        return mFileOutputStream;
    }

    /**
     * @return the file input stream
     */
    @NonNull
    public FileInputStream getFileInputStream() {
        return mFileInputStream;
    }

    /**
     * Encrypted file output stream
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    class EncryptedFileOutputStream extends FileOutputStream {

        private static final String TAG = "EncryptedFOS";

        private FileOutputStream mFileOutputStream;
        private String mFileName;
        private OutputStream mEncryptedOutputStream;
        private Context mContext;

        EncryptedFileOutputStream(String fileName,
                FileOutputStream fileOutputStream, Context context)
                throws GeneralSecurityException, IOException {
            super(fileOutputStream.getFD());
            mFileOutputStream = fileOutputStream;
            mFileName = fileName;
            mContext = context;
            setupEncryptedStream();
        }

        private void setupEncryptedStream() throws GeneralSecurityException, IOException {
            TinkCipher tinkCipher = TinkCipher.getFileCipher(mMasterKey, mContext,
                    mEncryptedFileKeyset);
            mEncryptedOutputStream = tinkCipher.createEncryptedStream(mFileOutputStream,
                    mFileName);
        }

        @Override
        public void write(@NonNull byte[] b) throws IOException {
            mEncryptedOutputStream.write(b);
        }

        @Override
        public void write(int b) throws IOException {
            mEncryptedOutputStream.write(b);
        }

        @Override
        public void write(@NonNull byte[] b, int off, int len) throws IOException {
            mEncryptedOutputStream.write(b, off, len);
        }

        @Override
        public void close() throws IOException {
            mEncryptedOutputStream.close();
        }

        @NonNull
        @Override
        public FileChannel getChannel() {
            throw new UnsupportedOperationException("For encrypted files, please open the "
                    + "relevant FileInput/FileOutputStream.");
        }

        @Override
        protected void finalize() throws IOException {
            super.finalize();
        }

        @Override
        public void flush() throws IOException {
            mEncryptedOutputStream.flush();
        }

    }


    /**
     * Encrypted file input stream
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    class EncryptedFileInputStream extends FileInputStream {

        // Was 25 characters, truncating to fix compile error
        private static final String TAG = "EncryptedFIS";

        private FileInputStream mFileInputStream;
        private String mFileName;
        private InputStream mEncryptedInputStream;
        private Context mContext;

        EncryptedFileInputStream(String fileName,
                FileInputStream fileInputStream, Context context)
                throws GeneralSecurityException, IOException {
            super(fileInputStream.getFD());
            mFileInputStream = fileInputStream;
            mFileName = fileName;
            mContext = context;
            setupDecryptedStream();
        }

        private void setupDecryptedStream() throws  GeneralSecurityException, IOException {
            TinkCipher tinkCipher = TinkCipher.getFileCipher(mMasterKey, mContext,
                    mEncryptedFileKeyset);
            mEncryptedInputStream = tinkCipher.createDecryptionStream(mFileInputStream,
                    mFileName);
        }

        @Override
        public int read() throws IOException {
            return mEncryptedInputStream.read();
        }

        @Override
        public int read(@NonNull byte[] b) throws IOException {
            return mEncryptedInputStream.read(b);
        }

        @Override
        public int read(@NonNull byte[] b, int off, int len) throws IOException {
            return mEncryptedInputStream.read(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            return mEncryptedInputStream.skip(n);
        }

        @Override
        public int available() throws IOException {
            return mEncryptedInputStream.available();
        }

        @Override
        public void close() throws IOException {
            mEncryptedInputStream.close();
        }

        @Override
        public FileChannel getChannel() {
            throw new UnsupportedOperationException("For encrypted files, please open the "
                    + "relevant FileInput/FileOutputStream.");
        }

        @Override
        protected void finalize() throws IOException {
            super.finalize();
        }

        @Override
        public synchronized void mark(int readlimit) {
            mEncryptedInputStream.mark(readlimit);
        }

        @Override
        public synchronized void reset() throws IOException {
            mEncryptedInputStream.reset();
        }

        @Override
        public boolean markSupported() {
            return mEncryptedInputStream.markSupported();
        }

    }

}
