package com.qiwenshare.document.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * OnlyOffice 配置属性。
 *
 * <p>从 {@code onlyoffice.*} 配置项绑定。启动时校验必要配置项（server-url）。</p>
 */
@ConfigurationProperties(prefix = "onlyoffice")
public class OnlyOfficeProperties {

    /** OnlyOffice Document Server 地址（如 https://document-server） */
    private String serverUrl;

    /** OnlyOffice API JS 地址（如 https://document-server/web-apps/apps/api/documents/api.js） */
    private String apiUrl;

    /** Converter 服务地址 */
    private String converterUrl;

    /** Command 服务地址 */
    private String commandUrl;

    /** JWT 配置 */
    private Jwt jwt = new Jwt();

    /** 最大文件大小（字节），默认 50MB */
    private long maxFileSize = 50L * 1024 * 1024;

    /** SSL 证书验证，默认 true */
    private boolean sslVerify = true;

    /** 回调基础 URL（如 https://your-domain.com/api/v1/document/callback） */
    private String callbackBaseUrl;

    /** OnlyOffice 可访问的后端文件下载基础 URL（用于本地存储时构建文件下载链接） */
    private String previewBaseUrl;

    /** 可预览的扩展名列表 */
    private List<String> viewedExtensions = List.of(
            "pdf", "txt", "html", "htm", "mht", "mhtml", "epub", "fb2", "xps", "djvu", "oxps"
    );

    /** 可直接编辑的扩展名列表 */
    private List<String> editedExtensions = List.of(
            "docx", "xlsx", "pptx", "csv", "json", "txt"
    );

    /** 可转换的扩展名列表 */
    private List<String> convertExtensions = List.of(
            "doc", "xls", "ppt", "dot", "xlt", "pot", "rtf", "odt", "ods", "odp"
    );

    /** FillForms 扩展名列表 */
    private List<String> fillFormsExtensions = List.of(
            "docxf"
    );

    /** 最大版本保留数，默认 10 */
    private int maxVersionCount = 10;

    /** 文档 token 有效期（秒），默认 4 小时 */
    private long documentTokenTtl = 4 * 3600L;

    /** 回调 token 有效期（秒），默认 30 分钟 */
    private long callbackTokenTtl = 30 * 60L;

    @PostConstruct
    public void validate() {
        if (serverUrl == null || serverUrl.isBlank()) {
            throw new IllegalStateException("onlyoffice.server-url 必须配置");
        }
    }

    // --- Getters and Setters ---

    public String getServerUrl() { return serverUrl; }
    public void setServerUrl(String serverUrl) { this.serverUrl = serverUrl; }

    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }

    public String getConverterUrl() { return converterUrl; }
    public void setConverterUrl(String converterUrl) { this.converterUrl = converterUrl; }

    public String getCommandUrl() { return commandUrl; }
    public void setCommandUrl(String commandUrl) { this.commandUrl = commandUrl; }

    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }

    public long getMaxFileSize() { return maxFileSize; }
    public void setMaxFileSize(long maxFileSize) { this.maxFileSize = maxFileSize; }

    public boolean isSslVerify() { return sslVerify; }
    public void setSslVerify(boolean sslVerify) { this.sslVerify = sslVerify; }

    public String getCallbackBaseUrl() { return callbackBaseUrl; }
    public void setCallbackBaseUrl(String callbackBaseUrl) { this.callbackBaseUrl = callbackBaseUrl; }

    public String getPreviewBaseUrl() { return previewBaseUrl; }
    public void setPreviewBaseUrl(String previewBaseUrl) { this.previewBaseUrl = previewBaseUrl; }

    public List<String> getViewedExtensions() { return viewedExtensions; }
    public void setViewedExtensions(List<String> viewedExtensions) { this.viewedExtensions = viewedExtensions; }

    public List<String> getEditedExtensions() { return editedExtensions; }
    public void setEditedExtensions(List<String> editedExtensions) { this.editedExtensions = editedExtensions; }

    public List<String> getConvertExtensions() { return convertExtensions; }
    public void setConvertExtensions(List<String> convertExtensions) { this.convertExtensions = convertExtensions; }

    public List<String> getFillFormsExtensions() { return fillFormsExtensions; }
    public void setFillFormsExtensions(List<String> fillFormsExtensions) { this.fillFormsExtensions = fillFormsExtensions; }

    public int getMaxVersionCount() { return maxVersionCount; }
    public void setMaxVersionCount(int maxVersionCount) { this.maxVersionCount = maxVersionCount; }

    public long getDocumentTokenTtl() { return documentTokenTtl; }
    public void setDocumentTokenTtl(long documentTokenTtl) { this.documentTokenTtl = documentTokenTtl; }

    public long getCallbackTokenTtl() { return callbackTokenTtl; }
    public void setCallbackTokenTtl(long callbackTokenTtl) { this.callbackTokenTtl = callbackTokenTtl; }

    /**
     * JWT 子配置。
     */
    public static class Jwt {
        /** JWT 签名密钥（与 Docker JWT_SECRET 一致） */
        private String secret;

        /** JWT header 名称，默认 Authorization */
        private String header = "Authorization";

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }

        public String getHeader() { return header; }
        public void setHeader(String header) { this.header = header; }
    }
}
