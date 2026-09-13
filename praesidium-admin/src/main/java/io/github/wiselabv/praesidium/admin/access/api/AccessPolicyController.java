package io.github.wiselabv.praesidium.admin.access.api;

import io.github.wiselabv.praesidium.admin.access.application.AccessPolicyService;
import io.github.wiselabv.praesidium.admin.access.application.dto.AccessPolicyRequest;
import io.github.wiselabv.praesidium.admin.access.application.dto.PolicyItem;
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
 * 授权策略接口：CRUD + 审批 / 撤销。
 */
@RestController
@RequestMapping("/api/access-policies")
public class AccessPolicyController {

    private final AccessPolicyService policyService;

    public AccessPolicyController(AccessPolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping
    public ApiResponse<PageResponse<PolicyItem>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(policyService.list(keyword, status, page, size));
    }

    @PostMapping
    public ApiResponse<PolicyItem> create(@Valid @RequestBody AccessPolicyRequest request) {
        return ApiResponse.ok(policyService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<PolicyItem> update(@PathVariable Long id,
                                          @Valid @RequestBody AccessPolicyRequest request) {
        return ApiResponse.ok(policyService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        policyService.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<PolicyItem> approve(@PathVariable Long id) {
        return ApiResponse.ok(policyService.approve(id));
    }

    @PostMapping("/{id}/revoke")
    public ApiResponse<PolicyItem> revoke(@PathVariable Long id) {
        return ApiResponse.ok(policyService.revoke(id));
    }
}
