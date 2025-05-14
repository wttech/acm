package com.vml.es.aem.acm.core.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Base64;
import org.apache.commons.io.IOUtils;

public class Base64Formatter {

    public String encodeBytes(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public byte[] decodeToBytes(String base64String) {
        return Base64.getDecoder().decode(base64String);
    }

    public String encodeToString(String input) {
        return Base64.getEncoder().encodeToString(input.getBytes());
    }

    public String encodeToString(InputStream inputStream) throws IOException {
        return encodeBytes(IOUtils.toByteArray(inputStream));
    }

    public String decodeToString(String base64String) {
        return new String(decodeToBytes(base64String));
    }

    public void encodeStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] bytes = IOUtils.toByteArray(inputStream);
        byte[] encodedBytes = Base64.getEncoder().encode(bytes);
        outputStream.write(encodedBytes);
    }

    public void decodeStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] bytes = IOUtils.toByteArray(inputStream);
        byte[] decodedBytes = Base64.getDecoder().decode(bytes);
        outputStream.write(decodedBytes);
    }
}
