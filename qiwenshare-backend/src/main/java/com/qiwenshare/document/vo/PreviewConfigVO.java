package com.qiwenshare.document.vo;

import java.util.Map;

/**
 * OnlyOffice 预览配置 VO。
 *
 * <p>包含 OnlyOffice DocEditor 所需的完整配置对象。</p>
 */
public class PreviewConfigVO {

    /** OnlyOffice API JS 地址 */
    private String docserviceApiUrl;

    /** 文档配置 */
    private DocumentConfig document;

    /** 编辑器配置 */
    private EditorConfig editorConfig;

    /** 文档 token（用于 OnlyOffice 内部验证） */
    private String token;

    public String getDocserviceApiUrl() { return docserviceApiUrl; }
    public void setDocserviceApiUrl(String docserviceApiUrl) { this.docserviceApiUrl = docserviceApiUrl; }

    public DocumentConfig getDocument() { return document; }
    public void setDocument(DocumentConfig document) { this.document = document; }

    public EditorConfig getEditorConfig() { return editorConfig; }
    public void setEditorConfig(EditorConfig editorConfig) { this.editorConfig = editorConfig; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    /**
     * 文档配置段。
     */
    public static class DocumentConfig {
        private String key;
        private String title;
        private String url;
        private String fileType;
        private String docType;
        private Long fileSize;
        private Map<String, Object> permissions;

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }
        public String getDocType() { return docType; }
        public void setDocType(String docType) { this.docType = docType; }
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
        public Map<String, Object> getPermissions() { return permissions; }
        public void setPermissions(Map<String, Object> permissions) { this.permissions = permissions; }
    }

    /**
     * 编辑器配置段。
     */
    public static class EditorConfig {
        private String callbackUrl;
        private String mode;
        private String lang;
        private Map<String, Object> customization;
        private UserConfig user;

        public String getCallbackUrl() { return callbackUrl; }
        public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public String getLang() { return lang; }
        public void setLang(String lang) { this.lang = lang; }
        public Map<String, Object> getCustomization() { return customization; }
        public void setCustomization(Map<String, Object> customization) { this.customization = customization; }
        public UserConfig getUser() { return user; }
        public void setUser(UserConfig user) { this.user = user; }
    }

    /**
     * 编辑器中的用户信息。
     */
    public static class UserConfig {
        private String id;
        private String name;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
