package com.qiwenshare.file.common;

import java.util.*;

/**
 * 文件类型分类枚举。
 *
 * <p>根据扩展名将文件映射到语义分类（image/document/video/audio/archive/other）。</p>
 */
public enum FileCategory {

    IMAGE("图片", Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico", "tiff", "tif")),
    DOCUMENT("文档", Set.of("doc", "docx", "xls", "xlsx", "ppt", "pptx", "pdf", "txt", "md", "csv", "rtf", "odt", "ods", "odp", "wps", "et", "dps")),
    VIDEO("视频", Set.of("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "rmvb", "rm", "3gp", "ts")),
    AUDIO("音频", Set.of("mp3", "wav", "flac", "aac", "ogg", "wma", "m4a", "ape", "opus")),
    ARCHIVE("压缩包", Set.of("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "tgz", "cab")),
    OTHER("其他", Set.of());

    private final String label;
    private final Set<String> extensions;

    private static final Map<String, FileCategory> EXTENSION_MAP = new HashMap<>();

    static {
        for (FileCategory category : values()) {
            for (String ext : category.extensions) {
                EXTENSION_MAP.put(ext, category);
            }
        }
    }

    FileCategory(String label, Set<String> extensions) {
        this.label = label;
        this.extensions = extensions;
    }

    public String getLabel() {
        return label;
    }

    public Set<String> getExtensions() {
        return Collections.unmodifiableSet(extensions);
    }

    /**
     * 根据扩展名获取文件分类。
     *
     * @param extension 扩展名（不含点号，如 "jpg"）
     * @return 文件分类
     */
    public static FileCategory fromExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return OTHER;
        }
        return EXTENSION_MAP.getOrDefault(extension.toLowerCase(), OTHER);
    }

    /**
     * 根据分类名获取扩展名集合。
     *
     * @param categoryName 分类名（如 "image", "document"）
     * @return 扩展名集合
     */
    public static Set<String> getExtensionsByCategoryName(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return Set.of();
        }
        try {
            return FileCategory.valueOf(categoryName.toUpperCase()).getExtensions();
        } catch (IllegalArgumentException e) {
            return Set.of();
        }
    }
}
