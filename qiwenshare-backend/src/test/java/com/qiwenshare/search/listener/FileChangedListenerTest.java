package com.qiwenshare.search.listener;

import com.qiwenshare.file.event.FileChangedEvent;
import com.qiwenshare.search.service.SearchIndexService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * FileChangedListener 单元测试??
 */
@ExtendWith(MockitoExtension.class)
class FileChangedListenerTest {

    @Mock
    private SearchIndexService searchIndexService;

    private FileChangedListener listener;

    @BeforeEach
    void setUp() {
        listener = new FileChangedListener(searchIndexService);
    }

    @Test
    @DisplayName("CREATED 事件触发 indexAsync")
    void onFileChanged_created_callsIndexAsync() {
        // Given
        FileChangedEvent event = new FileChangedEvent(this, 1L, FileChangedEvent.ChangeType.CREATED);

        // When
        listener.onFileChanged(event);

        // Then
        verify(searchIndexService).indexAsync(1L);
    }

    @Test
    @DisplayName("UPDATED 事件触发 updateAsync")
    void onFileChanged_updated_callsUpdateAsync() {
        // Given
        FileChangedEvent event = new FileChangedEvent(this, 2L, FileChangedEvent.ChangeType.UPDATED);

        // When
        listener.onFileChanged(event);

        // Then
        verify(searchIndexService).updateAsync(2L);
    }

    @Test
    @DisplayName("DELETED 事件触发 deleteAsync")
    void onFileChanged_deleted_callsDeleteAsync() {
        // Given
        FileChangedEvent event = new FileChangedEvent(this, 3L, FileChangedEvent.ChangeType.DELETED);

        // When
        listener.onFileChanged(event);

        // Then
        verify(searchIndexService).deleteAsync(3L);
    }
}
