package com.qiwenshare.document.callback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 保存回调处理器（status=2, 6）。
 *
 * <p>将保存任务委托给 {@link SaveCallbackAsyncWriter} 异步执行（跨类调用，注意红线 #16），
 * 回调立即返回。</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
@Order(2)
public class SaveCallbackHandler implements CallbackStatusHandler {

    private final SaveCallbackAsyncWriter asyncWriter;

    @Override
    public boolean supports(int status) {
        return status == 2 || status == 6;
    }

    @Override
    public void handle(CallbackContext context) {
        String downloadUrl = context.getBody().url();
        if (downloadUrl == null || downloadUrl.isBlank()) {
            log.warn("保存回调缺少下载 URL: userFileId={}, key={}",
                    context.getUserFileId(), context.getBody().key());
            context.markError();
            return;
        }

        // 传递 OnlyOffice 返回的文件格式，用于判断是否需要格式转换
        String filetype = context.getBody().filetype();
        asyncWriter.asyncSave(context, downloadUrl, filetype);
    }
}