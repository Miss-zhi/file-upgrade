package com.qiwenshare.ufop.util;

import java.io.File;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * UFOP 工具类
 */
public class UFOPUtils {

    /** 本地存储路径（由 UFOPAutoConfiguration 初始化） */
    public static String LOCAL_STORAGE_PATH;

    /** 根路径/桶名称（由 UFOPAutoConfiguration 初始化） */
    public static String ROOT_PATH;

    // ---- 文件类型分类 ----

    public static final String[] IMG_FILE = {"bmp", "jpg", "png", "tif", "gif", "jpeg"};
    public static final String[] DOC_FILE = {"doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "hlp", "wps", "rtf", "html", "pdf"};
    public static final String[] VIDEO_FILE = {"avi", "mp4", "mpg", "mov", "swf"};
    public static final String[] MUSIC_FILE = {"wav", "aif", "au", "mp3", "ram", "wma", "mmf", "amr", "aac", "flac"};
    public static final String[] TXT_FILE = {"txt", "html", "java", "xml", "js", "css", "json", "sql"};

    public static final int IMAGE_TYPE = 1;
    public static final int DOC_TYPE = 2;
    public static final int VIDEO_TYPE = 3;
    public static final int MUSIC_TYPE = 4;
    public static final int OTHER_TYPE = 5;
    public static final int SHARE_FILE = 6;
    public static final int RECYCLE_FILE = 7;

    public static boolean isImageFile(String extendName) {
        for (String ext : IMG_FILE) {
            if (ext.equalsIgnoreCase(extendName)) return true;
        }
        return false;
    }

    public static boolean isVideoFile(String extendName) {
        for (String ext : VIDEO_FILE) {
            if (ext.equalsIgnoreCase(extendName)) return true;
        }
        return false;
    }

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

    public static String pathSplitFormat(String filePath) {
        return filePath.replace("///", "/")
                .replace("//", "/")
                .replace("\\\\\\", "/")
                .replace("\\\\", "/");
    }

    /**
     * 获取上传文件路径，格式 "upload/yyyyMMdd/"
     */
    public static String getUploadFileUrl(String identifier, String extendName) {
        SimpleDateFormat formater = new SimpleDateFormat("yyyyMMdd");
        String path = ROOT_PATH + "/" + formater.format(new Date()) + "/";
        String staticPath = getStaticPath();
        File dir = new File(staticPath + path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return path + identifier + "." + extendName;
    }

    /**
     * 获取本地存储静态目录
     */
    public static String getStaticPath() {
        String path = LOCAL_STORAGE_PATH;
        if (path == null || path.isEmpty()) {
            path = getLocalRootPath();
        }
        return new File(path).getPath() + File.separator;
    }

    /**
     * 获取本地文件
     */
    public static File getLocalSaveFile(String fileUrl) {
        return new File(getStaticPath() + fileUrl);
    }

    /**
     * 获取缓存文件
     */
    public static File getCacheFile(String fileUrl) {
        return new File(getStaticPath() + "cache" + File.separator + fileUrl);
    }

    /**
     * 获取临时文件
     */
    public static File getTempFile(String fileUrl) {
        String tempPath = getStaticPath() + "temp" + File.separator + fileUrl;
        File tempFile = new File(tempPath);
        File parentFile = tempFile.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
        return tempFile;
    }
}
