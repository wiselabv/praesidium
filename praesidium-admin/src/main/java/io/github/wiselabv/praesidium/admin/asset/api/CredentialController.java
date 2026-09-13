package io.github.wiselabv.praesidium.admin.asset.api;

import io.github.wiselabv.praesidium.admin.asset.application.CredentialService;
import io.github.wiselabv.praesidium.admin.asset.application.dto.CredentialItem;
import io.github.wiselabv.praesidium.admin.asset.application.dto.CredentialRequest;
import io.github.wiselabv.praesidium.admin.asset.application.dto.SecretRevealResponse;
import io.github.wiselabv.praesidium.admin.shared.api.ApiResponse;
import io.github.wiselabv.praesidium.admin.shared.api.PageResponse;
import jakarta.validation.Valid;
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
 * 凭据接口：CRUD + 单次明文查看。
 */
@RestController
@RequestMapping("/api/credentials")
public class CredentialController {

    private final CredentialService credentialService;

    public CredentialController(CredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @GetMapping
    public ApiResponse<PageResponse<CredentialItem>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(credentialService.list(keyword, page, size));
    }

    @PostMapping
    public ApiResponse<CredentialItem> create(@Valid @RequestBody CredentialRequest request) {
        return ApiResponse.ok(credentialService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CredentialItem> update(@PathVariable Long id,
                                              @Valid @RequestBody CredentialRequest request) {
        return ApiResponse.ok(credentialService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        credentialService.delete(id);
        return ApiResponse.ok();
    }

    /** 单次明文查看（前端展示后即弃） */
    @GetMapping("/{id}/secret")
    public ApiResponse<SecretRevealResponse> reveal(@PathVariable Long id) {
        return ApiResponse.ok(credentialService.reveal(id));
    }
}
