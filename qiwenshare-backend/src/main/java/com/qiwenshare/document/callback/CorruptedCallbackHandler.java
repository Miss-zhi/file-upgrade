package com.qiwenshare.document.callback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 文档损坏回调处理器（status=3, 7）。
 *
 * <p>记录 ERROR 日志，返回 error=0（不触发 OnlyOffice 重试）。</p>
 *
 * <p>文档损坏是永久性状态，重试无意义，会导致 retry storm。</p>
 */
@Component
@Slf4j
@Order(3)
public class CorruptedCallbackHandler implements CallbackStatusHandler {

    @Override
    public boolean supports(int status) {
        return status == 3 || status == 7;
    }

    @Override
    public void handle(CallbackContext context) {
        log.error("文档损坏（永久性错误，不触发重试）: userFileId={}, status={}, key={}",
                context.getUserFileId(), context.getBody().status(), context.getBody().key());
        // 不调用 markError()，返回 error=0 避免 OnlyOffice 无意义重试
    }
}
