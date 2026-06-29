package com.qiwenshare.ufop.util;

import java.io.File;
import java.nio.file.Paths;

public class UFOPUtils {

    public static String getUploadPath(String rootPath, String fileName) {
        return Paths.get(rootPath, fileName).toString();
    }

    public static String getLocalRootPath() {
        return System.getProperty("user.dir") + File.separator + "uploads";
    }

    public static String getParentPath(String path) {
        int idx = path.lastIndexOf('/');
        return idx <= 0 ? "/" : path.substring(0, idx + 1);
    }

    public static String getFileName(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
