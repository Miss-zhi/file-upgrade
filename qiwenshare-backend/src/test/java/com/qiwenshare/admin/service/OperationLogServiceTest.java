package com.qiwenshare.admin.service;

import com.qiwenshare.admin.entity.OperationLog;
import com.qiwenshare.admin.repository.OperationLogRepository;
import com.qiwenshare.admin.vo.OperationLogVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OperationLogService 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class OperationLogServiceTest {

    @InjectMocks
    private OperationLogService operationLogService;

    @Mock
    private OperationLogRepository operationLogRepository;

    private OperationLog createLog(Long id, String module, String action) {
        OperationLog log = new OperationLog();
        log.setId(id);
        log.setUserId("U001");
        log.setUsername("admin");
        log.setModule(module);
        log.setAction(action);
        log.setDescription("test desc");
        log.setRequestMethod("POST");
        log.setRequestUri("/api/v1/test");
        log.setRequestParams("{}");
        log.setResponseCode(200);
        log.setErrorMessage(null);
        log.setIpAddress("127.0.0.1");
        log.setUserAgent("test-agent");
        log.setExecutionTime(50L);
        log.setCreateTime(LocalDateTime.of(2026, 7, 1, 10, 0));
        return log;
    }

    @Nested
    @DisplayName("listLogs")
    class ListLogs {

        @Test
        @DisplayName("returns mapped VO page with all filters")
        void returnsMappedPage() {
            OperationLog log1 = createLog(1L, "admin", "SET_QUOTA");
            OperationLog log2 = createLog(2L, "admin", "DISABLE_USER");
            Page<OperationLog> entityPage = new PageImpl<>(List.of(log1, log2));

            LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
            LocalDateTime end = LocalDateTime.of(2026, 12, 31, 23, 59);
            PageRequest pageable = PageRequest.of(0, 20);

            when(operationLogRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(entityPage);

            Page<OperationLogVO> result = operationLogService.listLogs(
                    "admin", "SET_QUOTA", "admin", start, end, pageable);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).module()).isEqualTo("admin");
            assertThat(result.getContent().get(0).action()).isEqualTo("SET_QUOTA");
            assertThat(result.getContent().get(0).username()).isEqualTo("admin");
            assertThat(result.getContent().get(1).action()).isEqualTo("DISABLE_USER");
        }

        @Test
        @DisplayName("works with null filters (no conditions)")
        void worksWithNullFilters() {
            OperationLog log = createLog(1L, "file", "UPLOAD");
            Page<OperationLog> entityPage = new PageImpl<>(List.of(log));
            PageRequest pageable = PageRequest.of(0, 10);

            when(operationLogRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(entityPage);

            Page<OperationLogVO> result = operationLogService.listLogs(
                    null, null, null, null, null, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).module()).isEqualTo("file");
        }

        @Test
        @DisplayName("works with blank string filters")
        void worksWithBlankFilters() {
            Page<OperationLog> entityPage = new PageImpl<>(List.of());
            PageRequest pageable = PageRequest.of(0, 10);

            when(operationLogRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(entityPage);

            Page<OperationLogVO> result = operationLogService.listLogs(
                    "  ", "", "  ", null, null, pageable);

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("maps all entity fields to VO correctly")
        void mapsAllFields() {
            OperationLog log = createLog(99L, "document", "EDIT");
            Page<OperationLog> entityPage = new PageImpl<>(List.of(log));
            PageRequest pageable = PageRequest.of(0, 10);

            when(operationLogRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(entityPage);

            Page<OperationLogVO> result = operationLogService.listLogs(
                    null, null, null, null, null, pageable);

            OperationLogVO vo = result.getContent().get(0);
            assertThat(vo.id()).isEqualTo(99L);
            assertThat(vo.userId()).isEqualTo("U001");
            assertThat(vo.requestMethod()).isEqualTo("POST");
            assertThat(vo.requestUri()).isEqualTo("/api/v1/test");
            assertThat(vo.ipAddress()).isEqualTo("127.0.0.1");
            assertThat(vo.executionTime()).isEqualTo(50L);
        }
    }
}
