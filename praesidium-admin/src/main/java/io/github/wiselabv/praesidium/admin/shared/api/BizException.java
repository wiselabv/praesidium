package io.github.wiselabv.praesidium.admin.shared.api;

/**
 * 业务异常：携带业务错误码与对用户可读的消息。
 *
 * <p>应用层 / 领域层抛出的业务失败统一用本异常表达，
 * 由 {@link GlobalExceptionHandler} 转成 {@link ApiResponse}（HTTP 200 + 非 0 code）。
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
