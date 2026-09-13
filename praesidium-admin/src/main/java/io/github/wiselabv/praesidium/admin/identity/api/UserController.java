package io.github.wiselabv.praesidium.admin.identity.api;

import java.util.List;

import io.github.wiselabv.praesidium.admin.identity.application.UserService;
import io.github.wiselabv.praesidium.admin.identity.application.dto.UserOption;
import io.github.wiselabv.praesidium.admin.shared.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口：简单清单（授权策略表单的用户下拉数据源）。
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<List<UserOption>> list() {
        return ApiResponse.ok(userService.listOptions());
    }
}
