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

package androidx.security.identity_credential;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.builder.ArrayBuilder;
import co.nstant.in.cbor.builder.MapBuilder;
import co.nstant.in.cbor.model.AbstractFloat;
import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.ByteString;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.DoublePrecisionFloat;
import co.nstant.in.cbor.model.Map;
import co.nstant.in.cbor.model.NegativeInteger;
import co.nstant.in.cbor.model.Number;
import co.nstant.in.cbor.model.SimpleValue;
import co.nstant.in.cbor.model.UnicodeString;
import co.nstant.in.cbor.model.UnsignedInteger;

class Util {
    private static final String TAG = "Util";

    static String cborPrettyPrint(byte[] encodedBytes) throws CborException {
        StringBuilder sb = new StringBuilder();

        ByteArrayInputStream bais = new ByteArrayInputStream(encodedBytes);
        List<DataItem> dataItems = new CborDecoder(bais).decode();
        int count = 0;
        for (DataItem dataItem : dataItems) {
            if (count > 0) {
                sb.append(",\n");
            }
            cborPrettyPrintDataItem(sb, 0, dataItem);
            count++;
        }

        return sb.toString();
    }

    // Returns true iff all elements in |items| are not compound (e.g. an array or a map).
    static boolean cborAreAllDataItemsNonCompound(List<DataItem> items) {
        for (DataItem item : items) {
            switch (item.getMajorType()) {
                case ARRAY:
                case MAP:
                    return false;
            }
        }
        return true;
    }

    static void cborPrettyPrintDataItem(StringBuilder sb, int indent, DataItem dataItem) {
        StringBuilder indentBuilder = new StringBuilder();
        for (int n = 0; n < indent; n++) {
            indentBuilder.append(' ');
        }
        String indentString = indentBuilder.toString();

        if (dataItem.hasTag()) {
            sb.append(String.format("tag %d ", dataItem.getTag().getValue()));
        }

        switch (dataItem.getMajorType()) {
            case INVALID:
                // TODO: throw
                sb.append("<invalid>");
                break;
            case UNSIGNED_INTEGER: {
                // Major type 0: an unsigned integer.
                BigInteger value = ((UnsignedInteger) dataItem).getValue();
                sb.append(value);
            }
            break;
            case NEGATIVE_INTEGER: {
                // Major type 1: a negative integer.
                BigInteger value = ((NegativeInteger) dataItem).getValue();
                sb.append(value);
            }
            break;
            case BYTE_STRING: {
                // Major type 2: a byte string.
                byte[] value = ((ByteString) dataItem).getBytes();
                sb.append("[");
                int count = 0;
                for (byte b : value) {
                    if (count > 0) {
                        sb.append(", ");
                    }
                    sb.append(String.format("0x%02x", b));
                    count++;
                }
                sb.append("]");
            }
            break;
            case UNICODE_STRING: {
                // Major type 3: string of Unicode characters that is encoded as UTF-8 [RFC3629].
                String value = ((UnicodeString) dataItem).getString();
                // TODO: escape ' in |value|
                sb.append("'" + value + "'");
            }
            break;
            case ARRAY: {
                // Major type 4: an array of data items.
                List<DataItem> items = ((co.nstant.in.cbor.model.Array) dataItem).getDataItems();
                if (items.size() == 0) {
                    sb.append("[]");
                } else if (cborAreAllDataItemsNonCompound(items)) {
                    // The case where everything fits on one line.
                    sb.append("[");
                    int count = 0;
                    for (DataItem item : items) {
                        cborPrettyPrintDataItem(sb, indent, item);
                        if (++count < items.size()) {
                            sb.append(", ");
                        }
                    }
                    sb.append("]");
                } else {
                    sb.append("[\n" + indentString);
                    int count = 0;
                    for (DataItem item : items) {
                        sb.append("  ");
                        cborPrettyPrintDataItem(sb, indent + 2, item);
                        if (++count < items.size()) {
                            sb.append(",");
                        }
                        sb.append("\n" + indentString);
                    }
                    sb.append("]");
                }
            }
            break;
            case MAP: {
                // Major type 5: a map of pairs of data items.
                Collection<DataItem> keys = ((co.nstant.in.cbor.model.Map) dataItem).getKeys();
                if (keys.size() == 0) {
                    sb.append("{}");
                } else {
                    sb.append("{\n" + indentString);
                    int count = 0;
                    for (DataItem key : keys) {
                        sb.append("  ");
                        DataItem value = ((co.nstant.in.cbor.model.Map) dataItem).get(key);
                        cborPrettyPrintDataItem(sb, indent + 2, key);
                        sb.append(" : ");
                        cborPrettyPrintDataItem(sb, indent + 2, value);
                        if (++count < keys.size()) {
                            sb.append(",");
                        }
                        sb.append("\n" + indentString);
                    }
                    sb.append("}");
                }
            }
            break;
            case TAG:
                // Major type 6: optional semantic tagging of other major types
                //
                // We never encounter this one since it's automatically handled via the
                // DataItem that is tagged.
                throw new RuntimeException("Semantic tag data item not expected");

            case SPECIAL:
                // Major type 7: floating point numbers and simple data types that need no
                // content, as well as the "break" stop code.
                if (dataItem instanceof SimpleValue) {
                    switch (((SimpleValue) dataItem).getSimpleValueType()) {
                        case FALSE:
                            sb.append("false");
                            break;
                        case TRUE:
                            sb.append("true");
                            break;
                        case NULL:
                            sb.append("null");
                            break;
                        case UNDEFINED:
                            sb.append("undefined");
                            break;
                        case RESERVED:
                            sb.append("reserved");
                            break;
                        case UNALLOCATED:
                            sb.append("unallocated");
                            break;
                    }
                } else if (dataItem instanceof DoublePrecisionFloat) {
                    DecimalFormat df = new DecimalFormat("0",
                            DecimalFormatSymbols.getInstance(Locale.ENGLISH));
                    df.setMaximumFractionDigits(340);
                    sb.append(df.format(((DoublePrecisionFloat) dataItem).getValue()));
                } else if (dataItem instanceof AbstractFloat) {
                    DecimalFormat df = new DecimalFormat("0",
                            DecimalFormatSymbols.getInstance(Locale.ENGLISH));
                    df.setMaximumFractionDigits(340);
                    sb.append(df.format(((AbstractFloat) dataItem).getValue()));
                } else {
                    sb.append("break");
                }
                break;
        }
    }

    public static String replaceLine(String text, int lineNumber, String replacementLine) {
        String[] lines = text.split("\n");
        int numLines = lines.length;
        if (lineNumber < 0) {
            lineNumber = numLines - (-lineNumber);
        }
        StringBuilder sb = new StringBuilder();
        for (int n = 0; n < numLines; n++) {
            if (n == lineNumber) {
                sb.append(replacementLine);
            } else {
                sb.append(lines[n]);
            }
            // Only add terminating newline if passed-in string ends in a newline.
            if (n == numLines - 1) {
                if (text.endsWith(("\n"))) {
                    sb.append('\n');
                }
            } else {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    static byte[] cborEncode(DataItem dataItem) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            new CborEncoder(baos).encode(dataItem);
        } catch (CborException e) {
            // This should never happen and we don't want cborEncode() to throw since that
            // would complicate all callers. Log it instead.
            e.printStackTrace();
            Log.e(TAG, "Error encoding DataItem");
        }
        return baos.toByteArray();
    }

    static byte[] cborEncodeBoolean(boolean value) {
        return cborEncode(new CborBuilder().add(value).build().get(0));
    }

    static byte[] cborEncodeString(@NonNull String value) {
        return cborEncode(new CborBuilder().add(value).build().get(0));
    }

    static byte[] cborEncodeInt(int value) {
        return cborEncode(new CborBuilder().add(value).build().get(0));
    }

    static byte[] cborEncodeBytestring(@NonNull byte[] value) {
        return cborEncode(new CborBuilder().add(value).build().get(0));
    }

    static byte[] cborEncodeCalendar(@NonNull Calendar calendar) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ");
        if (calendar.isSet(Calendar.MILLISECOND) && calendar.get(Calendar.MILLISECOND) != 0) {
            df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ");
        }
        df.setTimeZone(calendar.getTimeZone());
        Date val = calendar.getTime();
        String dateString = df.format(val);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            new CborEncoder(baos).encode(new CborBuilder()
                    .addTag(0)
                    .add(dateString)
                    .build());
        } catch (CborException e) {
            // Should never happen and we don't want to complicate callers by throwing.
            e.printStackTrace();
            Log.e(TAG, "Error encoding Calendar");
        }
        byte[] data = baos.toByteArray();
        return data;
    }

    static DataItem cborToDataItem(byte[] data) throws IdentityCredentialException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try {
            List<DataItem> dataItems = new CborDecoder(bais).decode();
            if (dataItems.size() != 1) {
                throw new IdentityCredentialException("Expected 1 item, found " + dataItems.size());
            }
            return dataItems.get(0);
        } catch (CborException e) {
            throw new IdentityCredentialException("Error decoding data", e);
        }
    }

    static boolean cborDecodeBoolean(@NonNull byte[] data) throws IdentityCredentialException {
        return cborToDataItem(data) == SimpleValue.TRUE;
    }

    static String cborDecodeString(@NonNull byte[] data) throws IdentityCredentialException {
        return ((co.nstant.in.cbor.model.UnicodeString) cborToDataItem(data)).getString();
    }

    static int cborDecodeInt(@NonNull byte[] data) throws IdentityCredentialException {
        return ((co.nstant.in.cbor.model.Number) cborToDataItem(data)).getValue().intValue();
    }

    static byte[] cborDecodeBytestring(@NonNull byte[] data) throws IdentityCredentialException {
        return ((co.nstant.in.cbor.model.ByteString) cborToDataItem(data)).getBytes();
    }

    static Calendar cborDecodeCalendar(@NonNull byte[] data) throws IdentityCredentialException {
        DataItem di = cborToDataItem(data);
        if (!(di instanceof co.nstant.in.cbor.model.UnicodeString)) {
            throw new IdentityCredentialException("Passed in data is not a Unicode-string");
        }
        if (!di.hasTag() || di.getTag().getValue() != 0) {
            throw new IdentityCredentialException("Passed in data is not tagged with tag 0");
        }
        String dateString = ((co.nstant.in.cbor.model.UnicodeString) di).getString();

        // Manually parse the timezone
        TimeZone parsedTz = TimeZone.getTimeZone("UTC");
        if (dateString.endsWith("Z")) {
        } else {
            String timeZoneSubstr = dateString.substring(dateString.length() - 6);
            parsedTz = TimeZone.getTimeZone("GMT" + timeZoneSubstr);
        }

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
        df.setTimeZone(parsedTz);
        Date date = null;
        try {
            date = df.parse(dateString);
        } catch (ParseException e) {
            // Try again, this time without the milliseconds
            df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            df.setTimeZone(parsedTz);
            try {
                date = df.parse(dateString);
            } catch (ParseException e2) {
                throw new IdentityCredentialException("Error parsing string", e2);
            }
        }

        Calendar c = new GregorianCalendar();
        c.clear();
        c.setTimeZone(df.getTimeZone());
        c.setTime(date);
        return c;
    }

    static DataItem entryNamespaceToCbor(EntryNamespace entryNamespace)
            throws IdentityCredentialException {
        CborBuilder entryBuilder = new CborBuilder();
        ArrayBuilder<CborBuilder> entryArrayBuilder = entryBuilder.addArray();
        for (String entryName : entryNamespace.getEntryNames()) {
            byte[] entryValue = entryNamespace.getEntryValue(entryName);
            Collection<Integer> accessControlProfileIds = entryNamespace.getAccessControlProfileIds(
                    entryName);

            CborBuilder accessControlProfileIdsBuilder = new CborBuilder();
            ArrayBuilder<CborBuilder> accessControlProfileIdsArrayBuilder =
                    accessControlProfileIdsBuilder.addArray();
            for (int id : accessControlProfileIds) {
                accessControlProfileIdsArrayBuilder.add(id);
            }

            MapBuilder<ArrayBuilder<CborBuilder>> entryMapBuilder = entryArrayBuilder.addMap();
            entryMapBuilder.put("name", entryName);
            entryMapBuilder.put(new UnicodeString("accessControlProfiles"),
                    accessControlProfileIdsBuilder.build().get(0));
            entryMapBuilder.put(new UnicodeString("value"), cborToDataItem(entryValue));
        }
        return entryBuilder.build().get(0);
    }

    public static EntryNamespace entryNamespaceFromCbor(String namespaceName, DataItem dataItem)
            throws IdentityCredentialException {
        if (!(dataItem instanceof Array)) {
            throw new IdentityCredentialException("Item is not an Array");
        }
        Array array = (Array) dataItem;

        EntryNamespace.Builder entryBuilder = new EntryNamespace.Builder(namespaceName);

        for (DataItem item : array.getDataItems()) {
            if (!(item instanceof co.nstant.in.cbor.model.Map)) {
                throw new IdentityCredentialException("Item is not a map");
            }
            co.nstant.in.cbor.model.Map map = (co.nstant.in.cbor.model.Map) item;

            String name = ((UnicodeString) map.get(new UnicodeString("name"))).getString();

            Collection<Integer> accessControlProfileIds = new ArrayList<Integer>();
            co.nstant.in.cbor.model.Array accessControlProfileArray =
                    (co.nstant.in.cbor.model.Array) map.get(
                            new UnicodeString("accessControlProfiles"));
            for (DataItem acpIdItem : accessControlProfileArray.getDataItems()) {
                accessControlProfileIds.add(((Number) acpIdItem).getValue().intValue());
            }

            DataItem cborValue = map.get(new UnicodeString("value"));
            byte[] data = cborEncode(cborValue);
            entryBuilder.addEntry(name, accessControlProfileIds, data);
        }

        return entryBuilder.build();
    }

    public static AccessControlProfile accessControlProfileFromCbor(DataItem item)
            throws IdentityCredentialException {
        if (!(item instanceof co.nstant.in.cbor.model.Map)) {
            throw new IdentityCredentialException("Item is not a map");
        }
        Map map = (Map) item;

        int accessControlProfileId = ((Number) map.get(
                new UnicodeString("id"))).getValue().intValue();
        AccessControlProfile.Builder builder = new AccessControlProfile.Builder(
                accessControlProfileId);

        item = map.get(new UnicodeString("readerCertificate"));
        if (item != null) {
            byte[] rcBytes = ((ByteString) item).getBytes();
            CertificateFactory certFactory = null;
            try {
                certFactory = CertificateFactory.getInstance("X.509");
                builder.setReaderCertificate((X509Certificate) certFactory.generateCertificate(
                        new ByteArrayInputStream(rcBytes)));
            } catch (CertificateException e) {
                throw new IdentityCredentialException("Error decoding readerCertificate", e);
            }
        }

        builder.setUserAuthenticationRequired(false);
        item = map.get(new UnicodeString("capabilityType"));
        if (item != null) {
            // TODO: deal with -1 as per entryNamespaceToCbor()
            builder.setUserAuthenticationRequired(true);
            item = map.get(new UnicodeString("timeout"));
            builder.setUserAuthenticationTimeout(
                    item == null ? 0 : (((Number) item).getValue().intValue()));
        }
        return builder.build();
    }

    static DataItem accessControlProfileToCbor(AccessControlProfile accessControlProfile)
            throws IdentityCredentialException {
        CborBuilder cborBuilder = new CborBuilder();
        MapBuilder<CborBuilder> mapBuilder = cborBuilder.addMap();

        mapBuilder.put("id", accessControlProfile.getAccessControlProfileId());
        X509Certificate readerCertificate = accessControlProfile.getReaderCertificate();
        if (readerCertificate != null) {
            try {
                mapBuilder.put("readerCertificate", readerCertificate.getEncoded());
            } catch (CertificateEncodingException e) {
                throw new IdentityCredentialException("Error encoding reader mCertificate", e);
            }
        }
        if (accessControlProfile.isUserAuthenticationRequired()) {
            mapBuilder.put("capabilityType", 1); // TODO: what value to put here?
            int timeout = accessControlProfile.getUserAuthenticationTimeout();
            if (timeout != 0) {
                mapBuilder.put("timeout", timeout);
            }
        }
        return cborBuilder.build().get(0);
    }

    static int[] integerCollectionToArray(Collection<Integer> collection) {
        int[] result = new int[collection.size()];
        int n = 0;
        for (int item : collection) {
            result[n++] = item;
        }
        return result;
    }

    /*
Certificate:
    Data:
        Version: 3 (0x2)
        Serial Number: 1 (0x1)
    Signature Algorithm: ecdsa-with-SHA256
        Issuer: CN=fake
        Validity
            Not Before: Jan  1 00:00:00 1970 GMT
            Not After : Jan  1 00:00:00 2048 GMT
        Subject: CN=fake
        Subject Public Key Info:
            Public Key Algorithm: id-ecPublicKey
                Public-Key: (256 bit)
                00000000  04 9b 60 70 8a 99 b6 bf  e3 b8 17 02 9e 93 eb 48  |..`p...........H|
                00000010  23 b9 39 89 d1 00 bf a0  0f d0 2f bd 6b 11 bc d1  |#.9......./.k...|
                00000020  19 53 54 28 31 00 f5 49  db 31 fb 9f 7d 99 bf 23  |.ST(1..I.1..}..#|
                00000030  fb 92 04 6b 23 63 55 98  ad 24 d2 68 c4 83 bf 99  |...k#cU..$.h....|
                00000040  62                                                |b|
    Signature Algorithm: ecdsa-with-SHA256
         30:45:02:20:67:ad:d1:34:ed:a5:68:3f:5b:33:ee:b3:18:a2:
         eb:03:61:74:0f:21:64:4a:a3:2e:82:b3:92:5c:21:0f:88:3f:
         02:21:00:b7:38:5c:9b:f2:9c:b1:27:86:37:44:df:eb:4a:b2:
         6c:11:9a:c1:ff:b2:80:95:ce:fc:5f:26:b4:20:6e:9b:0d
     */


    static @NonNull
    X509Certificate signPublicKeyWithPrivateKey(String keyToSignAlias,
            String keyToSignWithAlias) throws IdentityCredentialException {

        KeyStore ks = null;
        try {
            ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);

            /* First note that KeyStore.getCertificate() returns a self-signed X.509 certificate
             * for the key in question. As per RFC 5280, section 4.1 an X.509 certificate has the
             * following structure:
             *
             *   Certificate  ::=  SEQUENCE  {
             *        tbsCertificate       TBSCertificate,
             *        signatureAlgorithm   AlgorithmIdentifier,
             *        signatureValue       BIT STRING  }
             *
             * Conveniently, the X509Certificate class has a getTBSCertificate() method which
             * returns the tbsCertificate blob. So all we need to do is just sign that and build
             * signatureAlgorithm and signatureValue and combine it with tbsCertificate. We don't
             * need a full-blown ASN.1/DER encoder to do this.
             */
            X509Certificate selfSignedCert = (X509Certificate) ks.getCertificate(keyToSignAlias);
            byte[] tbsCertificate = selfSignedCert.getTBSCertificate();

            KeyStore.Entry keyToSignWithEntry = ks.getEntry(keyToSignWithAlias, null);
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initSign(((KeyStore.PrivateKeyEntry) keyToSignWithEntry).getPrivateKey());
            s.update(tbsCertificate);
            byte[] signatureValue = s.sign();

            /* The DER encoding for a SEQUENCE of length 128-65536 - the length is updated below.
             *
             * We assume - and test for below - that the final length is always going to be in
             * this range. This is a sound assumption given we're using 256-bit EC keys.
             */
            byte[] sequence = new byte[]{
                    0x30, (byte) 0x82, 0x00, 0x00
            };

            /* The DER encoding for the ECDSA with SHA-256 signature algorithm:
             *
             *   SEQUENCE (1 elem)
             *      OBJECT IDENTIFIER 1.2.840.10045.4.3.2 ecdsaWithSHA256 (ANSI X9.62 ECDSA
             *      algorithm with SHA256)
             */
            byte[] signatureAlgorithm = new byte[]{
                    0x30, 0x0a, 0x06, 0x08, 0x2a, (byte) 0x86, 0x48, (byte) 0xce, 0x3d, 0x04, 0x03,
                    0x02
            };

            /* The DER encoding for a BIT STRING with one element - the length is updated below.
             *
             * We assume the length of signatureValue is always going to be less than 128. This
             * assumption works since we know ecdsaWithSHA256 signatures are always 69, 70, or
             * 71 bytes long when DER encoded.
             */
            byte[] bitStringForSignature = new byte[]{0x03, 0x00, 0x00};

            // Calculate sequence length and set it in |sequence|.
            int sequenceLength = tbsCertificate.length +
                    signatureAlgorithm.length +
                    bitStringForSignature.length +
                    signatureValue.length;
            if (sequenceLength < 128 || sequenceLength > 65535) {
                throw new Exception("Unexpected sequenceLength " + sequenceLength);
            }
            sequence[2] = (byte) (sequenceLength >> 8);
            sequence[3] = (byte) (sequenceLength & 0xff);

            // Calculate signatureValue length and set it in |bitStringForSignature|.
            int signatureValueLength = signatureValue.length + 1;
            if (signatureValueLength >= 128) {
                throw new Exception("Unexpected signatureValueLength " + signatureValueLength);
            }
            bitStringForSignature[1] = (byte) signatureValueLength;

            // Finally concatenate everything together.
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(sequence);
            baos.write(tbsCertificate);
            baos.write(signatureAlgorithm);
            baos.write(bitStringForSignature);
            baos.write(signatureValue);
            byte[] resultingCertBytes = baos.toByteArray();

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream bais = new ByteArrayInputStream(resultingCertBytes);
            X509Certificate result = (X509Certificate) cf.generateCertificate(bais);
            return result;
        } catch (Exception e) {
            throw new IdentityCredentialException("Error signing public key with private key", e);
        }
    }

    /**
     * Helper function to check if a given certificate chain is valid.
     *
     * NOTE NOTE NOTE: We only check that the certificates in the chain sign each other. We
     * <em>specifically</em> don't check that each certificate is also a CA certificate.
     *
     * @param certificateChain the chain to validate.
     * @return <code>true</code> if valid, <code>false</code> otherwise.
     */
    public static boolean validateCertificateChain(Collection<X509Certificate> certificateChain) {
        int certChainLength = certificateChain.size();

        // TODO: add unit tests

        // First check that each certificate signs the previous one...
        X509Certificate prevCertificate = null;
        int n = 0;
        for (X509Certificate certificate : certificateChain) {
            if (prevCertificate != null) {
                // We're not the leaf certificate...
                //
                // Check the previous certificate was signed by this one.
                try {
                    prevCertificate.verify(certificate.getPublicKey());
                } catch (CertificateException |
                        InvalidKeyException |
                        NoSuchAlgorithmException |
                        NoSuchProviderException |
                        SignatureException e) {
                    return false;
                }
            } else {
                // we're the leaf certificate so we're not signing anything nor
                // do we need to be e.g. a CA certificate.
            }
            prevCertificate = certificate;
            n++;
        }
        return true;
    }

    static byte[] getDeviceAuthenticationCbor(String docType,
            byte[] sessionTranscript,
            byte[] deviceNameSpaceCbor) throws IdentityCredentialException {

        ByteArrayOutputStream daBaos = new ByteArrayOutputStream();
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(deviceNameSpaceCbor);
            List<DataItem> dataItems = null;
            dataItems = new CborDecoder(bais).decode();
            DataItem deviceNameSpace = dataItems.get(0);
            new CborEncoder(daBaos).encode(new CborBuilder()
                    .addArray()
                    .add("DeviceAuthentication")
                    .add(sessionTranscript)
                    .add(docType)
                    .add(deviceNameSpace)
                    .end()
                    .build());
        } catch (CborException e) {
            throw new IdentityCredentialException("Error encoding DeviceAuthentication", e);
        }
        return daBaos.toByteArray();
    }


    /**
     * Computes an HKDF.
     *
     * This is based on https://github.com/google/tink/blob/master/java/src/main/java/com/google
     * /crypto/tink/subtle/Hkdf.java
     * which is also Copyright (c) Google and also licensed under the Apache 2 license.
     *
     * @param macAlgorithm the MAC algorithm used for computing the Hkdf. I.e., "HMACSHA1" or
     *                     "HMACSHA256".
     * @param ikm          the input keying material.
     * @param salt         optional salt. A possibly non-secret random value. If no salt is
     *                     provided (i.e. if
     *                     salt has length 0) then an array of 0s of the same size as the hash
     *                     digest is used as salt.
     * @param info         optional context and application specific information.
     * @param size         The length of the generated pseudorandom string in bytes. The maximal
     *                     size is
     *                     255.DigestSize, where DigestSize is the size of the underlying HMAC.
     * @return size pseudorandom bytes.
     */
    static byte[] computeHkdf(
            String macAlgorithm, final byte[] ikm, final byte[] salt, final byte[] info, int size) {
        Mac mac = null;
        try {
            mac = Mac.getInstance(macAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("No such algorithm: " + macAlgorithm, e);
        }
        if (size > 255 * mac.getMacLength()) {
            throw new RuntimeException("size too large");
        }
        try {
            if (salt == null || salt.length == 0) {
                // According to RFC 5869, Section 2.2 the salt is optional. If no salt is provided
                // then HKDF uses a salt that is an array of zeros of the same length as the hash
                // digest.
                mac.init(new SecretKeySpec(new byte[mac.getMacLength()], macAlgorithm));
            } else {
                mac.init(new SecretKeySpec(salt, macAlgorithm));
            }
            byte[] prk = mac.doFinal(ikm);
            byte[] result = new byte[size];
            int ctr = 1;
            int pos = 0;
            mac.init(new SecretKeySpec(prk, macAlgorithm));
            byte[] digest = new byte[0];
            while (true) {
                mac.update(digest);
                mac.update(info);
                mac.update((byte) ctr);
                digest = mac.doFinal();
                if (pos + digest.length < size) {
                    System.arraycopy(digest, 0, result, pos, digest.length);
                    pos += digest.length;
                    ctr++;
                } else {
                    System.arraycopy(digest, 0, result, pos, size - pos);
                    break;
                }
            }
            return result;
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Error MACing", e);
        }
    }

    // Helper to build proofOfProvisioning CBOR data.
    static byte[] buildProofOfProvisioningCbor(String docType,
            Collection<AccessControlProfile> accessControlProfiles,
            Collection<EntryNamespace> entryNamespaces,
            PrivateKey key) throws IdentityCredentialException {

        CborBuilder accessControlProfileBuilder = new CborBuilder();
        ArrayBuilder<CborBuilder> arrayBuilder = accessControlProfileBuilder.addArray();
        for (AccessControlProfile profile : accessControlProfiles) {
            arrayBuilder.add(Util.accessControlProfileToCbor(profile));
        }

        CborBuilder dataBuilder = new CborBuilder();
        MapBuilder<CborBuilder> dataMapBuilder = dataBuilder.addMap();
        for (EntryNamespace entryNamespace : entryNamespaces) {
            dataMapBuilder.put(
                    new UnicodeString(entryNamespace.getNamespaceName()),
                    Util.entryNamespaceToCbor(entryNamespace));
        }

        CborBuilder signedDataBuilder = new CborBuilder();
        ArrayBuilder<CborBuilder> signedDataArrayBuilder = signedDataBuilder.addArray()
                .add("ProofOfProvisioning")
                .add(docType)
                .add(accessControlProfileBuilder.build().get(0))
                .add(dataBuilder.build().get(0))
                .add(false);

        byte[] encodedBytes;
        try {
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initSign(key);
            DataItem dataToSignItem = signedDataBuilder.build().get(0);

            ByteArrayOutputStream dtsBaos = new ByteArrayOutputStream();
            CborEncoder dtsEncoder = new CborEncoder(dtsBaos);
            dtsEncoder.encode(dataToSignItem);
            byte[] dataToSign = dtsBaos.toByteArray();
            s.update(dataToSign);
            byte[] signature = s.sign();

            CborBuilder proofOfProvisioningBuilder = new CborBuilder();
            ArrayBuilder<CborBuilder> proofOfProvisioningArrayBuilder =
                    proofOfProvisioningBuilder.addArray();
            proofOfProvisioningArrayBuilder.add(dataToSignItem).add(signature);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            CborEncoder encoder = new CborEncoder(baos);
            encoder.encode(proofOfProvisioningBuilder.build());
            encodedBytes = baos.toByteArray();

        } catch (NoSuchAlgorithmException |
                InvalidKeyException |
                SignatureException |
                CborException e) {
            throw new IdentityCredentialException("Error building proof of provisioning", e);
        }
        return encodedBytes;
    }

    // Helper to build proofOfDeletion CBOR data.
    static byte[] buildProofOfDeletionCbor(String docType,
            PrivateKey key) throws IdentityCredentialException {

        CborBuilder signedDataBuilder = new CborBuilder();
        ArrayBuilder<CborBuilder> signedDataArrayBuilder = signedDataBuilder.addArray()
                .add("ProofOfDeletion")
                .add(docType)
                .add(false);

        byte[] encodedBytes;
        try {
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initSign(key);
            DataItem dataToSignItem = signedDataBuilder.build().get(0);

            ByteArrayOutputStream dtsBaos = new ByteArrayOutputStream();
            CborEncoder dtsEncoder = new CborEncoder(dtsBaos);
            byte[] dataToSign = new byte[0];
            dtsEncoder.encode(dataToSignItem);
            dataToSign = dtsBaos.toByteArray();
            s.update(dataToSign);
            byte[] signature = s.sign();

            CborBuilder proofOfProvisioningBuilder = new CborBuilder();
            ArrayBuilder<CborBuilder> proofOfProvisioningArrayBuilder =
                    proofOfProvisioningBuilder.addArray();
            proofOfProvisioningArrayBuilder.add(dataToSignItem).add(signature);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            CborEncoder encoder = new CborEncoder(baos);
            encoder.encode(proofOfProvisioningBuilder.build());
            encodedBytes = baos.toByteArray();
        } catch (NoSuchAlgorithmException |
                InvalidKeyException |
                SignatureException |
                CborException e) {
            throw new IdentityCredentialException("Error building proof of deletion", e);
        }
        return encodedBytes;
    }

}
