package io.github.wiselabv.praesidium.admin.shared.api;

import java.util.List;

/**
 * 统一分页响应体：列表 + 总数 + 页码（1 起）+ 页大小。
 *
 * <p>所有列表接口统一此结构，前端按 {@code total} 渲染 Arco 分页器。
 */
public record PageResponse<T>(List<T> items, long total, int page, int size) {

    public static <T> PageResponse<T> of(List<T> items, long total, int page, int size) {
        return new PageResponse<>(items, total, page, size);
    }
}
