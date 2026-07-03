package com.qiwenshare.document.service;

import com.qiwenshare.document.config.OnlyOfficeProperties;
import com.qiwenshare.document.vo.DocumentHealthVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DocumentAdminService 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class DocumentAdminServiceTest {

    @Mock
    private OnlyOfficeProperties onlyOfficeProperties;

    @Mock
    private HttpClient httpClient;

    private DocumentAdminService createService() {
        return new DocumentAdminService(onlyOfficeProperties, httpClient);
    }

    @Nested
    @DisplayName("checkHealth")
    class CheckHealth {

        @Test
        @DisplayName("returns UP when HTTP 200")
        void returnsUp() throws Exception {
            when(onlyOfficeProperties.getServerUrl()).thenReturn("http://onlyoffice:8080");
            @SuppressWarnings("unchecked")
            HttpResponse<String> response = mock(HttpResponse.class);
            when(response.statusCode()).thenReturn(200);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(response);

            DocumentHealthVO result = createService().checkHealth();

            assertThat(result.status()).isEqualTo("UP");
            assertThat(result.serverUrl()).isEqualTo("http://onlyoffice:8080");
            assertThat(result.error()).isNull();
        }

        @Test
        @DisplayName("returns DOWN when non-200 status")
        void returnsDownOnNon200() throws Exception {
            when(onlyOfficeProperties.getServerUrl()).thenReturn("http://onlyoffice:8080");
            @SuppressWarnings("unchecked")
            HttpResponse<String> response = mock(HttpResponse.class);
            when(response.statusCode()).thenReturn(503);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(response);

            DocumentHealthVO result = createService().checkHealth();

            assertThat(result.status()).isEqualTo("DOWN");
            assertThat(result.error()).isEqualTo("HTTP 503");
        }

        @Test
        @DisplayName("returns DOWN on connection error")
        void returnsDownOnConnectionError() throws Exception {
            when(onlyOfficeProperties.getServerUrl()).thenReturn("http://onlyoffice:8080");
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenThrow(new IOException("Connection refused"));

            DocumentHealthVO result = createService().checkHealth();

            assertThat(result.status()).isEqualTo("DOWN");
            assertThat(result.error()).contains("Connection refused");
        }

        @Test
        @DisplayName("normalizes trailing slash in server URL")
        void normalizesTrailingSlash() throws Exception {
            when(onlyOfficeProperties.getServerUrl()).thenReturn("http://onlyoffice:8080/");
            @SuppressWarnings("unchecked")
            HttpResponse<String> response = mock(HttpResponse.class);
            when(response.statusCode()).thenReturn(200);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(response);

            DocumentHealthVO result = createService().checkHealth();

            assertThat(result.status()).isEqualTo("UP");
            assertThat(result.serverUrl()).isEqualTo("http://onlyoffice:8080/");
        }
    }
}
