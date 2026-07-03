package com.qiwenshare.document.callback;

/**
 * 回调状态处理器接口。
 *
 * <p>每个 OnlyOffice 回调状态码对应一个实现。
 * 由 {@link CallbackManager} 按 status 分发。</p>
 */
public interface CallbackStatusHandler {

    /**
     * 判断是否支持处理指定的回调状态。
     *
     * @param status OnlyOffice 回调状态码
     * @return 支持返回 true
     */
    boolean supports(int status);

    /**
     * 处理回调。
     *
     * @param context 回调上下文
     */
    void handle(CallbackContext context);
}
