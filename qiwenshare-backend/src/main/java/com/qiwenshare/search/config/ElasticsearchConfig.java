package com.qiwenshare.search.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Elasticsearch 客户端配置。
 *
 * <p>从 {@code spring.elasticsearch.*} 配置项创建 {@link ElasticsearchClient} Bean。
 * 支持可选的用户名/密码认证。</p>
 */
@Configuration
@EnableConfigurationProperties(SearchProperties.class)
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String uris;

    @Value("${spring.elasticsearch.username:}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    @Value("${spring.elasticsearch.connection-timeout:5s}")
    private String connectionTimeout;

    @Value("${spring.elasticsearch.socket-timeout:30s}")
    private String socketTimeout;

    /**
     * 创建 Elasticsearch Java API Client。
     *
     * @return ElasticsearchClient 实例
     */
    @Bean
    public ElasticsearchClient elasticsearchClient() {
        RestClientBuilder builder = RestClient.builder(HttpHost.create(uris));

        // 配置认证
        if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password));
            builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        }

        // 配置超时
        builder.setRequestConfigCallback(requestConfigBuilder ->
                requestConfigBuilder
                        .setConnectTimeout(parseTimeoutMillis(connectionTimeout))
                        .setSocketTimeout(parseTimeoutMillis(socketTimeout)));

        RestClient restClient = builder.build();
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }

    /**
     * 解析超时字符串为毫秒数。
     *
     * <p>支持格式：
     * <ul>
     *   <li>{@code "5s"} — 5 秒</li>
     *   <li>{@code "500ms"} — 500 毫秒</li>
     *   <li>{@code "2m"} — 2 分钟</li>
     *   <li>{@code "5000"} — 纯数字视为毫秒</li>
     * </ul>
     *
     * @param timeout 超时字符串
     * @return 毫秒数
     */
    private int parseTimeoutMillis(String timeout) {
        if (timeout == null || timeout.isBlank()) {
            throw new IllegalArgumentException("超时值不能为空");
        }
        String trimmed = timeout.trim();
        if (trimmed.endsWith("ms")) {
            return Integer.parseInt(trimmed.substring(0, trimmed.length() - 2));
        } else if (trimmed.endsWith("s")) {
            return Integer.parseInt(trimmed.substring(0, trimmed.length() - 1)) * 1000;
        } else if (trimmed.endsWith("m")) {
            return Integer.parseInt(trimmed.substring(0, trimmed.length() - 1)) * 60 * 1000;
        }
        // 纯数字视为毫秒
        return Integer.parseInt(trimmed);
    }
}
