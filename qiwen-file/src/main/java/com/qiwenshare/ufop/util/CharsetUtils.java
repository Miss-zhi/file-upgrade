package com.qiwenshare.ufop.util;

import java.nio.charset.Charset;

public class CharsetUtils {

    public static String detectCharset(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF)
            return "UTF-8";
        return Charset.defaultCharset().name();
    }
}
