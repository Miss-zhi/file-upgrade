package com.qiwenshare.document.callback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 关闭回调处理器（status=4）。
 *
 * <p>正常关闭，无编辑内容保存，无需操作。</p>
 */
@Component
@Slf4j
@Order(4)
public class ClosedCallbackHandler implements CallbackStatusHandler {

    @Override
    public boolean supports(int status) {
        return status == 4;
    }

    @Override
    public void handle(CallbackContext context) {
        log.info("文档正常关闭: userFileId={}", context.getUserFileId());
    }
}
