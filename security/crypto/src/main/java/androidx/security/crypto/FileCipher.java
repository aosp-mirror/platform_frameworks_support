package androidx.security.crypto;

import androidx.annotation.NonNull;
import android.util.Log;
import android.util.Pair;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.concurrent.Executor;

import androidx.security.SecureConfig;
import androidx.security.context.ContextCompat;

public class FileCipher {

    private String mFileName;
    private String mKeyPairAlias;
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;

    private Executor mExecutor;
    private ContextCompat.EncryptedFileInputStreamListener mListener;

    private SecureConfig mSecureConfig;

    public FileCipher(String fileName, FileInputStream fileInputStream,
                      SecureConfig secureConfig, Executor executor,
                      ContextCompat.EncryptedFileInputStreamListener listener) throws IOException {
        mFileName = fileName;
        mFileInputStream = fileInputStream;
        mSecureConfig = secureConfig;
        EncryptedFileInputStream encryptedFileInputStream = new EncryptedFileInputStream(mFileInputStream);
        setEncryptedFileInputStreamListener(executor, listener);
        encryptedFileInputStream.decrypt(listener);
    }

    public FileCipher(String keyPairAlias, FileOutputStream fileOutputStream,
                      SecureConfig secureConfig) {
        mKeyPairAlias = keyPairAlias;
        mFileOutputStream = new EncryptedFileOutputStream(mFileName, mKeyPairAlias, fileOutputStream);
        mSecureConfig = secureConfig;
    }

    public void setEncryptedFileInputStreamListener(@NonNull Executor executor,
                                                    @NonNull ContextCompat.EncryptedFileInputStreamListener listener) {
        mExecutor = executor;
        mListener = listener;
    }

    public FileOutputStream getFileOutputStream() {
        return mFileOutputStream;
    }

    public FileInputStream getFileInputStream() {
        return mFileInputStream;
    }

    class EncryptedFileOutputStream extends FileOutputStream {

        private static final String TAG = "EncryptedFOS";

        private FileOutputStream fileOutputStream;
        private String keyPairAlias;

        EncryptedFileOutputStream(String name, String keyPairAlias, FileOutputStream fileOutputStream) {
            super(new FileDescriptor());
            this.keyPairAlias = keyPairAlias;
            this.fileOutputStream = fileOutputStream;
        }

        private String getAsymKeyPairAlias() {
            return this.keyPairAlias;
        }

        @Override
        public void write(@NonNull byte[] b) {
            KeyStoreCompat secureKeyStore = KeyStoreCompat.getDefault();
            if (!secureKeyStore.keyExists(getAsymKeyPairAlias())) {
                KeyGeneratorCompat keyGenerator = KeyGeneratorCompat.getDefault();
                keyGenerator.generateAsymmetricKeyPair(getAsymKeyPairAlias());
            }
            KeyGeneratorCompat secureKeyGenerator = KeyGeneratorCompat.getDefault();
            final EphemeralSecretKey secretKey = secureKeyGenerator.generateEphemeralDataKey();
            final CipherCompat secureCipher = CipherCompat.getDefault(mSecureConfig.getBiometricSupport());
            final Pair<byte[], byte[]> encryptedData = secureCipher.encryptEphemeralData(secretKey, b);
            secureCipher.encryptSensitiveDataAsymmetric(getAsymKeyPairAlias(), secretKey.getEncoded(), new CipherCompat.SecureAsymmetricEncryptionCallback() {
                public void encryptionComplete(byte[] encryptedEphemeralKey) {
                    byte[] encodedData = secureCipher.encodeEphemeralData(getAsymKeyPairAlias().getBytes(), encryptedEphemeralKey, encryptedData.first, encryptedData.second);
                    secretKey.destroy();
                    try {
                        fileOutputStream.write(encodedData);
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to write secure file.");
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void write(int b) throws IOException {
            throw new UnsupportedOperationException("For encrypted files, you must write all data simultaneously. Call #write(byte[]).");
        }

        @Override
        public void write(@NonNull byte[] b, int off, int len) throws IOException {
            throw new UnsupportedOperationException("For encrypted files, you must write all data simultaneously. Call #write(byte[]).");
        }

        @Override
        public void close() throws IOException {
            fileOutputStream.close();
        }

        @NonNull
        @Override
        public FileChannel getChannel() {
            throw new UnsupportedOperationException("For encrypted files, you must write all data simultaneously. Call #write(byte[]).");
        }

        @Override
        protected void finalize() throws IOException {
            super.finalize();
        }

        @Override
        public void flush() throws IOException {
            fileOutputStream.flush();
        }
    }

    class EncryptedFileInputStream extends FileInputStream {

        // Was 25 characters, truncating to fix compile error
        private static final String TAG = "EncryptedFIS";

        private FileInputStream fileInputStream;
        private byte[] decryptedData;
        private int readStatus = 0;

        EncryptedFileInputStream(FileInputStream fileInputStream) {
            super(new FileDescriptor());
            this.fileInputStream = fileInputStream;
        }

        @Override
        public int read() throws IOException {
            throw new UnsupportedOperationException("For encrypted files, you must read all data simultaneously. Call #read(byte[]).");
        }

        void decrypt(final ContextCompat.EncryptedFileInputStreamListener listener) throws IOException {
            final EncryptedFileInputStream thisStream = this;
            if (this.decryptedData == null) {
                try {
                    byte[] encodedData = new byte[fileInputStream.available()];
                    readStatus = fileInputStream.read(encodedData);
                    CipherCompat secureCipher = CipherCompat.getDefault(mSecureConfig.getBiometricSupport());
                    secureCipher.decryptEncodedData(encodedData, new CipherCompat.SecureDecryptionCallback() {
                        public void decryptionComplete(byte[] clearText) {
                            thisStream.decryptedData = clearText;
                            //Binder.clearCallingIdentity();
                            listener.onEncryptedFileInput(thisStream);
                        }
                    });
                } catch (IOException ex) {
                    throw ex;
                }
            }
        }

        private void destroyCache() {
            if (decryptedData != null) {
                Arrays.fill(decryptedData, (byte) 0);
                decryptedData = null;
            }
        }

        @Override
        public int read(@NonNull byte[] b) {
            System.arraycopy(decryptedData, 0, b, 0, decryptedData.length);
            return readStatus;
        }

        @Override
        public int read(@NonNull byte[] b, int off, int len) throws IOException {
            throw new UnsupportedOperationException("For encrypted files, you must read all data simultaneously. Call #read(byte[]).");
        }

        @Override
        public long skip(long n) throws IOException {
            throw new UnsupportedOperationException("For encrypted files, you must read all data simultaneously. Call #read(byte[]).");
        }

        @Override
        public int available() {
            return decryptedData.length;
        }

        @Override
        public void close() throws IOException {
            destroyCache();
            fileInputStream.close();
        }

        @Override
        public FileChannel getChannel() {
            throw new UnsupportedOperationException("For encrypted files, you must read all data simultaneously. Call #read(byte[]).");
        }

        @Override
        protected void finalize() throws IOException {
            destroyCache();
            super.finalize();
        }

        @Override
        public synchronized void mark(int readlimit) {
            throw new UnsupportedOperationException("For encrypted files, you must read all data simultaneously. Call #read(byte[]).");
        }

        @Override
        public synchronized void reset() throws IOException {
            throw new UnsupportedOperationException("For encrypted files, you must read all data simultaneously. Call #read(byte[]).");
        }

        @Override
        public boolean markSupported() {
            return false;
        }

    }

}
