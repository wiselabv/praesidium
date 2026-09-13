package io.github.wiselabv.praesidium.admin.shared.api;

/**
 * 全站统一响应体：{@code {code, message, data}}。
 *
 * <p>约定（见仓库根 docs/ARCHITECTURE.md 第 4 节）：
 * <ul>
 *   <li>{@code code=0} 表示成功，非 0 为业务错误码（见 {@link ApiErrorCode}）；</li>
 *   <li>业务失败仍以 HTTP 200 返回，由前端按 {@code code} 分支处理；</li>
 *   <li>仅「未认证 / 无权限」由安全层以 HTTP 401 / 403 返回（{@code code} 同步对齐）。</li>
 * </ul>
 *
 * @param <T> 业务数据载荷类型
 */
public record ApiResponse<T>(int code, String message, T data) {

    /** 成功（无数据） */
    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(0, "ok", null);
    }

    /** 成功（带数据） */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", data);
    }

    /** 业务失败（HTTP 200，前端按 code 分支） */
    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
