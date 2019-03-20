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
import androidx.security.SecureConfig;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

/**
 * Class used to create and read encrypted files. Provides implementations
 * of EncryptedFileInput/Output Streams.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class FileCipher {

    private String mFileName;
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;
    private Context mContext;

    SecureConfig mSecureConfig;

    public FileCipher(@NonNull String fileName, @NonNull FileInputStream fileInputStream,
                      @NonNull SecureConfig secureConfig, @NonNull Context context)
            throws IOException {
        mFileName = fileName;
        mSecureConfig = secureConfig;
        mContext = context;
        mFileInputStream = new EncryptedFileInputStream(mFileName, fileInputStream, mContext);
    }

    public FileCipher(@NonNull String fileName, @NonNull FileOutputStream fileOutputStream,
                      @NonNull SecureConfig secureConfig, @NonNull Context context)
            throws IOException {
        mFileName = fileName;
        mSecureConfig = secureConfig;
        mContext = context;
        mFileOutputStream = new EncryptedFileOutputStream(mFileName,
                fileOutputStream, mContext);
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
                FileOutputStream fileOutputStream, Context context) throws IOException {
            super(fileOutputStream.getFD());
            mFileOutputStream = fileOutputStream;
            mFileName = fileName;
            mContext = context;
            setupEncryptedStream();
        }

        private void setupEncryptedStream() {
            SecureCipher secureCipher = new SecureCipher(mSecureConfig, mContext);
            mEncryptedOutputStream = secureCipher.createEncryptedStream(mFileOutputStream,
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
            mFileOutputStream.close();
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
                FileInputStream fileInputStream, Context context) throws IOException {
            super(fileInputStream.getFD());
            mFileInputStream = fileInputStream;
            mFileName = fileName;
            mContext = context;
            setupDecryptedStream();
        }

        private void setupDecryptedStream() {
            SecureCipher secureCipher = new SecureCipher(mSecureConfig, mContext);
            mEncryptedInputStream = secureCipher.createDecryptionStream(mFileInputStream,
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
