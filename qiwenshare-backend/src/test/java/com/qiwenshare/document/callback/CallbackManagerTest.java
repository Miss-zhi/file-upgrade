package com.qiwenshare.document.callback;

import com.qiwenshare.document.dto.CallbackBodyDTO;
import com.qiwenshare.document.service.OnlyOfficeCommandClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * CallbackManager 和各 Handler 单元测试??
 */
@ExtendWith(MockitoExtension.class)
class CallbackManagerTest {

    private CallbackManager callbackManager;
    private EditingCallbackHandler editingHandler;
    private SaveCallbackHandler saveHandler;
    private CorruptedCallbackHandler corruptedHandler;
    private ClosedCallbackHandler closedHandler;

    @BeforeEach
    void setUp() {
        OnlyOfficeCommandClient commandClient = org.mockito.Mockito.mock(OnlyOfficeCommandClient.class);
        SaveCallbackAsyncWriter asyncWriter = org.mockito.Mockito.mock(SaveCallbackAsyncWriter.class);
        editingHandler = new EditingCallbackHandler(commandClient);
        saveHandler = new SaveCallbackHandler(asyncWriter);
        corruptedHandler = new CorruptedCallbackHandler();
        closedHandler = new ClosedCallbackHandler();
        callbackManager = new CallbackManager(List.of(editingHandler, saveHandler, corruptedHandler, closedHandler));
    }

    @Nested
    @DisplayName("回调分发")
    class Dispatch {

        @Test
        @DisplayName("status=1 分发??EditingCallbackHandler")
        void dispatch_status1_callsEditingHandler() {
            CallbackBodyDTO body = new CallbackBodyDTO(1, null, "key", List.of("user1"), List.of(), null, null, null, null);
            CallbackContext context = new CallbackContext(body, 10L, 1L);

            // EditingCallbackHandler 无外部依赖，handle 不抛异常
            callbackManager.dispatch(context);

            assertThat(context.getErrorCode()).isEqualTo(0);
        }

        @Test
        @DisplayName("status=2 分发??SaveCallbackHandler")
        void dispatch_status2_callsSaveHandler() {
            CallbackBodyDTO body = new CallbackBodyDTO(2, "http://download/url", "key", List.of("user1"), List.of(), null, "http://download/url", null, null);
            CallbackContext context = new CallbackContext(body, 10L, 1L);

            int result = callbackManager.dispatch(context);

            // SaveCallbackHandler 异步处理，回调立即返??error=0
            assertThat(result).isEqualTo(0);
        }

        @Test
        @DisplayName("status=6 分发??SaveCallbackHandler")
        void dispatch_status6_callsSaveHandler() {
            CallbackBodyDTO body = new CallbackBodyDTO(6, "http://download/url", "key", List.of("user1"), List.of(), null, "http://download/url", null, null);
            CallbackContext context = new CallbackContext(body, 10L, 1L);

            int result = callbackManager.dispatch(context);

            assertThat(result).isEqualTo(0);
        }

        @Test
        @DisplayName("status=2 缺少 url 时返??error=1")
        void dispatch_status2_missingUrl_returnsError() {
            CallbackBodyDTO body = new CallbackBodyDTO(2, null, "key", List.of("user1"), List.of(), null, null, null, null);
            CallbackContext context = new CallbackContext(body, 10L, 1L);

            int result = callbackManager.dispatch(context);

            assertThat(result).isEqualTo(1);
        }

        @Test
        @DisplayName("status=4 分发??ClosedCallbackHandler")
        void dispatch_status4_callsClosedHandler() {
            CallbackBodyDTO body = new CallbackBodyDTO(4, null, "key", List.of("user1"), List.of(), null, null, null, null);
            CallbackContext context = new CallbackContext(body, 10L, 1L);

            callbackManager.dispatch(context);

            assertThat(context.getErrorCode()).isEqualTo(0);
        }

        @Test
        @DisplayName("未知 status 返回 error=1")
        void dispatch_unknownStatus_returnsError1() {
            CallbackBodyDTO body = new CallbackBodyDTO(99, null, "key", List.of("user1"), List.of(), null, null, null, null);
            CallbackContext context = new CallbackContext(body, 10L, 1L);

            int result = callbackManager.dispatch(context);

            assertThat(result).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Handler 支持检??)")

    class HandlerSupport {

        @Test
        @DisplayName("EditingCallbackHandler 支持 status=1")
        void editingHandler_supportsStatus1() {
            assertThat(editingHandler.supports(1)).isTrue();
            assertThat(editingHandler.supports(2)).isFalse();
        }

        @Test
        @DisplayName("CorruptedCallbackHandler 支持 status=3 ??7")
        void corruptedHandler_supportsStatus3And7() {
            assertThat(corruptedHandler.supports(3)).isTrue();
            assertThat(corruptedHandler.supports(7)).isTrue();
            assertThat(corruptedHandler.supports(1)).isFalse();
        }

        @Test
        @DisplayName("ClosedCallbackHandler 支持 status=4")
        void closedHandler_supportsStatus4() {
            assertThat(closedHandler.supports(4)).isTrue();
            assertThat(closedHandler.supports(1)).isFalse();
        }
        @Test
        @DisplayName("SaveCallbackHandler 支持 status=2 ??6")
        void saveHandler_supportsStatus2And6() {
            assertThat(saveHandler.supports(2)).isTrue();
            assertThat(saveHandler.supports(6)).isTrue();
            assertThat(saveHandler.supports(1)).isFalse();
            assertThat(saveHandler.supports(4)).isFalse();
        }
    }
}
