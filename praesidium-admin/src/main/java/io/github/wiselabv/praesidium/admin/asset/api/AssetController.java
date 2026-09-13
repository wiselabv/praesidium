package io.github.wiselabv.praesidium.admin.asset.api;

import java.util.List;

import io.github.wiselabv.praesidium.admin.asset.application.AssetAccountService;
import io.github.wiselabv.praesidium.admin.asset.application.AssetService;
import io.github.wiselabv.praesidium.admin.asset.application.dto.AssetAccountItem;
import io.github.wiselabv.praesidium.admin.asset.application.dto.AssetAccountRequest;
import io.github.wiselabv.praesidium.admin.asset.application.dto.AssetItem;
import io.github.wiselabv.praesidium.admin.asset.application.dto.AssetRequest;
import io.github.wiselabv.praesidium.admin.asset.application.dto.AssetTestResult;
import io.github.wiselabv.praesidium.admin.shared.api.ApiResponse;
import io.github.wiselabv.praesidium.admin.shared.api.PageResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 资产接口：资产 CRUD、连通性测试、资产下账号管理。
 */
@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;
    private final AssetAccountService accountService;

    public AssetController(AssetService assetService, AssetAccountService accountService) {
        this.assetService = assetService;
        this.accountService = accountService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AssetItem>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type) {
        return ApiResponse.ok(assetService.list(keyword, type, page, size));
    }

    @PostMapping
    public ApiResponse<AssetItem> create(@Valid @RequestBody AssetRequest request,
                                         Authentication authentication) {
        return ApiResponse.ok(assetService.create(request, (Long) authentication.getPrincipal()));
    }

    @PutMapping("/{id}")
    public ApiResponse<AssetItem> update(@PathVariable Long id, @Valid @RequestBody AssetRequest request) {
        return ApiResponse.ok(assetService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        assetService.delete(id);
        return ApiResponse.ok();
    }

    /** 连通性测试（TCP 连接，3 秒超时） */
    @PostMapping("/{id}/test")
    public ApiResponse<AssetTestResult> test(@PathVariable Long id) {
        return ApiResponse.ok(assetService.test(id));
    }

    /** 资产下账号清单 */
    @GetMapping("/{id}/accounts")
    public ApiResponse<List<AssetAccountItem>> accounts(@PathVariable Long id) {
        return ApiResponse.ok(accountService.listByAsset(id));
    }

    /** 资产下新建账号 */
    @PostMapping("/{id}/accounts")
    public ApiResponse<AssetAccountItem> createAccount(@PathVariable Long id,
                                                       @Valid @RequestBody AssetAccountRequest request) {
        return ApiResponse.ok(accountService.create(id, request));
    }
}
