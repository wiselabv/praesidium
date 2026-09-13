package io.github.wiselabv.praesidium.admin.asset.api;

import io.github.wiselabv.praesidium.admin.asset.application.AssetAccountService;
import io.github.wiselabv.praesidium.admin.asset.application.dto.AssetAccountItem;
import io.github.wiselabv.praesidium.admin.asset.application.dto.AssetAccountRequest;
import io.github.wiselabv.praesidium.admin.shared.api.ApiResponse;
import io.github.wiselabv.praesidium.admin.shared.api.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 资产账号接口：全局账号清单（分页）、修改、删除。
 */
@RestController
@RequestMapping("/api/asset-accounts")
public class AssetAccountController {

    private final AssetAccountService accountService;

    public AssetAccountController(AssetAccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AssetAccountItem>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(accountService.listPage(keyword, page, size));
    }

    @PutMapping("/{id}")
    public ApiResponse<AssetAccountItem> update(@PathVariable Long id,
                                                @Valid @RequestBody AssetAccountRequest request) {
        return ApiResponse.ok(accountService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        accountService.delete(id);
        return ApiResponse.ok();
    }
}
