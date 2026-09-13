package io.github.wiselabv.praesidium.admin.audit.api;

import io.github.wiselabv.praesidium.admin.audit.application.LoginLogService;
import io.github.wiselabv.praesidium.admin.audit.application.dto.LoginLogItem;
import io.github.wiselabv.praesidium.admin.shared.api.ApiResponse;
import io.github.wiselabv.praesidium.admin.shared.api.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录日志接口：分页查询。
 */
@RestController
@RequestMapping("/api/logs/login")
public class LoginLogController {

    private final LoginLogService loginLogService;

    public LoginLogController(LoginLogService loginLogService) {
        this.loginLogService = loginLogService;
    }

    @GetMapping
    public ApiResponse<PageResponse<LoginLogItem>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String result) {
        return ApiResponse.ok(loginLogService.list(keyword, result, page, size));
    }
}
