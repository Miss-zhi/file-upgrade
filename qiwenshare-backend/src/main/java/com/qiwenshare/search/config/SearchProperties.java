package com.qiwenshare.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 搜索模块配置属性。
 *
 * <p>绑定 {@code search.*} 配置项，控制索引名称、搜索分页等搜索行为。
 * 文件内容索引由 document 模块负责，搜索模块仅索引文件元数据。</p>
 */
@ConfigurationProperties(prefix = "search")
public class SearchProperties {

    /**
     * ES 索引名称，默认 "filesearch"。
     */
    private String indexName = "filesearch";

    /**
     * 搜索默认分页大小，默认 20。
     */
    private int defaultPageSize = 20;

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    public void setDefaultPageSize(int defaultPageSize) {
        this.defaultPageSize = defaultPageSize;
    }
}
