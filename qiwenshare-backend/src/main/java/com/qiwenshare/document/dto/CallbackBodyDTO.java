package com.qiwenshare.document.dto;

import java.util.List;

/**
 * OnlyOffice 回调请求体。
 *
 * @param status        回调状态码（1=编辑中, 2=保存, 3=损坏, 4=关闭无编辑, 6=强制保存, 7=损坏强制发送）
 * @param url           编辑后文件的下载 URL（status=2/6 时提供）
 * @param key           文档 key
 * @param users         当前编辑用户列表
 * @param actions       操作列表（包含 userId 和 type）
 * @param changesurl    变更文件 URL（可选）
 * @param history       历史数据（可选）
 * @param filetype      下载文件的扩展名（不含点号，如 docx）
 * @param forcesavetype 强制保存类型（0=命令触发, 1=保存按钮, 2=提交表单, 3=表单提交）
 */
public record CallbackBodyDTO(
        int status,
        String url,
        String key,
        List<String> users,
        List<Action> actions,
        String changesurl,
        Object history,
        String filetype,
        Integer forcesavetype,
        /** OnlyOffice JWT，status=1/4 时 Authorization header 缺失，需要从 body 回退验证 */
        String token
) {

    /**
     * 回调中的操作记录。
     *
     * @param userId 用户标识
     * @param type   操作类型（0=断开, 1=连接, 2=编辑）
     */
    public record Action(String userId, int type) {
    }
}
