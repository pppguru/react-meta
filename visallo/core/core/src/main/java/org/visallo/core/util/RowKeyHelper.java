package org.visallo.core.util;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class RowKeyHelper {
    public static final char FIELD_SEPARATOR = (char) 0x1e;
    public static final int OFFSET_WIDTH = 16;

    public static String build(String... parts) {
        return StringUtils.join(parts, FIELD_SEPARATOR);
    }

    public static String buildSHA256KeyString(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] sha = digest.digest(bytes);
            return "urn" + FIELD_SEPARATOR + "sha256" + FIELD_SEPARATOR + Hex.encodeHexString(sha);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String buildSHA256KeyString(InputStream in) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            int bufferSize = 4096;
            byte[] buffer = new byte[bufferSize];
            int read;
            while ((read = in.read(buffer, 0, buffer.length)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] sha = digest.digest();
            return "urn" + FIELD_SEPARATOR + "sha256" + FIELD_SEPARATOR + Hex.encodeHexString(sha);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String[] splitOnMinorFieldSeparator(String rowKey) {
        return rowKey.split("" + FIELD_SEPARATOR);
    }
}
