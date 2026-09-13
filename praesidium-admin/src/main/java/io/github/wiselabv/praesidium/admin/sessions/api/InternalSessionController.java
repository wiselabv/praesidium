package io.github.wiselabv.praesidium.admin.sessions.api;

import io.github.wiselabv.praesidium.admin.sessions.application.InternalSessionService;
import io.github.wiselabv.praesidium.admin.sessions.application.dto.ConnectionInfo;
import io.github.wiselabv.praesidium.admin.shared.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 内网会话接口：仅面向 Rust 网关，以 {@code X-Internal-Key} 共享密钥鉴权
 * （不走用户 JWT 过滤器，见 {@code SecurityConfig} 放行规则）。
 */
@RestController
@RequestMapping("/api/internal/sessions")
public class InternalSessionController {

    private final InternalSessionService internalSessionService;
    private final String internalKey;

    public InternalSessionController(InternalSessionService internalSessionService,
                                     @Value("${praesidium.internal.key:}") String internalKey) {
        this.internalSessionService = internalSessionService;
        this.internalKey = internalKey;
    }

    /** 拉取会话连接信息（含解密凭据）：网关建 SSH 连接前调用 */
    @GetMapping("/{id}/connection")
    public ApiResponse<ConnectionInfo> connection(@PathVariable Long id, HttpServletRequest request) {
        requireInternalKey(request);
        return ApiResponse.ok(internalSessionService.connectionInfo(id));
    }

    /** 共享密钥校验：未配置（blank）时一律拒绝，避免误开放 */
    private void requireInternalKey(HttpServletRequest request) {
        String key = request.getHeader("X-Internal-Key");
        if (internalKey.isBlank() || !internalKey.equals(key)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid internal key");
        }
    }
}
