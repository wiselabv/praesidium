package io.github.wiselabv.praesidium.admin.sessions.api;

import java.util.List;

import io.github.wiselabv.praesidium.admin.sessions.application.SessionService;
import io.github.wiselabv.praesidium.admin.sessions.application.dto.ConnectRequest;
import io.github.wiselabv.praesidium.admin.sessions.application.dto.ConnectResponse;
import io.github.wiselabv.praesidium.admin.sessions.application.dto.RecordingItem;
import io.github.wiselabv.praesidium.admin.sessions.application.dto.RecordingObjectItem;
import io.github.wiselabv.praesidium.admin.sessions.application.dto.SessionItem;
import io.github.wiselabv.praesidium.admin.shared.api.ApiResponse;
import io.github.wiselabv.praesidium.admin.shared.api.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会话接口：在线会话查询/断开、发起连接、录像查询。
 */
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    public ApiResponse<PageResponse<SessionItem>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(sessionService.list(keyword, status, page, size));
    }

    /** 断开在线会话 */
    @PostMapping("/{id}/disconnect")
    public ApiResponse<SessionItem> disconnect(@PathVariable Long id) {
        return ApiResponse.ok(sessionService.disconnect(id));
    }

    /** 发起连接（签发网关令牌，前端携令牌连 Rust 网关） */
    @PostMapping("/connect")
    public ApiResponse<ConnectResponse> connect(@Valid @RequestBody ConnectRequest request,
                                                Authentication authentication,
                                                HttpServletRequest http) {
        return ApiResponse.ok(sessionService.connect(request,
                (Long) authentication.getPrincipal(), clientIp(http)));
    }

    /** 会话录像分页 */
    @GetMapping("/recordings")
    public ApiResponse<PageResponse<RecordingItem>> recordings(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(sessionService.recordings(keyword, page, size));
    }

    /** 录像切片对象（MinIO 预签名 URL，回放/下载用） */
    @GetMapping("/recordings/{id}/objects")
    public ApiResponse<List<RecordingObjectItem>> recordingObjects(@PathVariable Long id) {
        return ApiResponse.ok(sessionService.recordingObjects(id));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
