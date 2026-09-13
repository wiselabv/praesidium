package io.github.wiselabv.praesidium.admin.identity.api;

import java.util.List;

import io.github.wiselabv.praesidium.admin.identity.application.SsoProviderService;
import io.github.wiselabv.praesidium.admin.identity.application.dto.SsoProviderItem;
import io.github.wiselabv.praesidium.admin.identity.application.dto.SsoProviderSaveRequest;
import io.github.wiselabv.praesidium.admin.shared.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SSO 提供商配置接口：CRUD（系统设置-认证分组使用）。
 */
@RestController
@RequestMapping("/api/sso/providers")
public class SsoProviderController {

    private final SsoProviderService ssoProviderService;

    public SsoProviderController(SsoProviderService ssoProviderService) {
        this.ssoProviderService = ssoProviderService;
    }

    @GetMapping
    public ApiResponse<List<SsoProviderItem>> list() {
        return ApiResponse.ok(ssoProviderService.list());
    }

    @GetMapping("/{provider}")
    public ApiResponse<SsoProviderItem> get(@PathVariable String provider) {
        return ApiResponse.ok(ssoProviderService.get(provider));
    }

    @PostMapping
    public ApiResponse<SsoProviderItem> save(@Valid @RequestBody SsoProviderSaveRequest request) {
        return ApiResponse.ok(ssoProviderService.save(request));
    }

    @DeleteMapping("/{provider}")
    public ApiResponse<Void> delete(@PathVariable String provider) {
        ssoProviderService.delete(provider);
        return ApiResponse.ok();
    }
}
