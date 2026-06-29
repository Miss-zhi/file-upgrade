package com.qiwenshare.ufop.util;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ReadFileUtils {

    public static String readFileContent(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        }
        return sb.toString();
    }

    public static byte[] readFileBytes(InputStream is) throws IOException {
        return is.readAllBytes();
    }
}
