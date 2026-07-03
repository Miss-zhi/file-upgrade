package com.qiwenshare.document.callback;

import com.qiwenshare.document.service.OnlyOfficeCommandClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 编辑中回调处理器（status=1）。
 *
 * <p>记录编辑状态日志。检测非协作用户触发 forcesave。</p>
 */
@Component
@Slf4j
@Order(1)
public class EditingCallbackHandler implements CallbackStatusHandler {

    private final OnlyOfficeCommandClient commandClient;

    public EditingCallbackHandler(OnlyOfficeCommandClient commandClient) {
        this.commandClient = commandClient;
    }

    @Override
    public boolean supports(int status) {
        return status == 1;
    }

    @Override
    public void handle(CallbackContext context) {
        var body = context.getBody();
        log.info("文档编辑中: userFileId={}, users={}", context.getUserFileId(), body.users());

        // 检测 actions 中是否有 disconnect（type=0），非协作用户断开时触发 forcesave
        if (body.actions() != null) {
            boolean hasDisconnect = body.actions().stream()
                    .anyMatch(action -> action.type() == 0);
            if (hasDisconnect && body.key() != null) {
                log.info("检测到用户断开，触发 forcesave: userFileId={}, key={}",
                        context.getUserFileId(), body.key());
                boolean success = commandClient.forcesave(body.key());
                if (!success) {
                    log.warn("forcesave 调用失败: userFileId={}, key={}",
                            context.getUserFileId(), body.key());
                }
            }
        }
    }
}
