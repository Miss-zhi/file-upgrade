package com.qiwenshare.admin.vo;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 分页响应包装。
 *
 * <p>解决 {@link Page} 接口 Jackson 序列化问题。</p>
 *
 * @param content     当前页数据
 * @param totalElements 总记录数
 * @param totalPages    总页数
 * @param page          当前页码
 * @param size          每页大小
 * @param first         是否首页
 * @param last          是否最后一页
 * @param <T>           数据类型
 */
public record PageResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int page,
        int size,
        boolean first,
        boolean last
) {
    /**
     * 从 Spring Data Page 构造响应。
     *
     * @param page Spring Data 分页对象
     * @return PageResponse 实例
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize(),
                page.isFirst(),
                page.isLast()
        );
    }
}
