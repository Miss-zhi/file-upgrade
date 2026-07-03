package com.qiwenshare.document.callback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 回调管理器。
 *
 * <p>注入所有 {@link CallbackStatusHandler} 实现，按回调状态码分发处理。
 * 无匹配处理器时记录 WARN 日志并返回 error=1。</p>
 */
@Component
@Slf4j
public class CallbackManager {

    private final List<CallbackStatusHandler> handlers;

    public CallbackManager(List<CallbackStatusHandler> handlers) {
        // 按 @Order 排序，确保分发顺序确定（S5）
        List<CallbackStatusHandler> sorted = new ArrayList<>(handlers);
        AnnotationAwareOrderComparator.sort(sorted);
        this.handlers = sorted;
        log.info("回调管理器初始化，已注册 {} 个处理器", handlers.size());
    }

    /**
     * 分发回调到匹配的处理器。
     *
     * @param context 回调上下文
     * @return 处理结果 error code（0=成功，1=失败）
     */
    public int dispatch(CallbackContext context) {
        int status = context.getBody().status();
        // status=1 为编辑心跳，高频场景下降为 DEBUG 避免日志量爆炸（S6）
        if (status == 1) {
            log.debug("分发回调: status={}, userFileId={}", status, context.getUserFileId());
        } else {
            log.info("分发回调: status={}, userFileId={}", status, context.getUserFileId());
        }

        for (CallbackStatusHandler handler : handlers) {
            if (handler.supports(status)) {
                try {
                    handler.handle(context);
                } catch (Exception e) {
                    log.error("回调处理器异常: status={}, userFileId={}", status, context.getUserFileId(), e);
                    context.markError();
                }
                return context.getErrorCode();
            }
        }

        log.warn("未找到回调处理器: status={}", status);
        return 1;
    }
}
