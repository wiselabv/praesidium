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
    /**
     * 授权策略服务
     */
    private final AccessPolicyService policyService;

    public AccessPolicyController(AccessPolicyService policyService) {
        this.policyService = policyService;
    }

    /**
     * 列出所有授权策略。
     *
     * @param page    页数
     * @param size    每页大小
     * @param keyword 关键字
     * @param status  状态
     * @return 授权策略列表
     */
    @GetMapping
    public ApiResponse<PageResponse<PolicyItem>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(policyService.list(keyword, status, page, size));
    }

    /**
     * 创建一个授权策略。
     *
     * @param request 请求
     * @return 授权策略
     */
    @PostMapping
    public ApiResponse<PolicyItem> create(@Valid @RequestBody AccessPolicyRequest request) {
        return ApiResponse.ok(policyService.create(request));
    }

    /**
     * 更新一个授权策略。
     *
     * @param id      ID
     * @param request 请求
     * @return 授权策略
     */
    @PutMapping("/{id}")
    public ApiResponse<PolicyItem> update(@PathVariable Long id,
                                          @Valid @RequestBody AccessPolicyRequest request) {
        return ApiResponse.ok(policyService.update(id, request));
    }

    /**
     * 删除一个授权策略。
     *
     * @param id ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        policyService.delete(id);
        return ApiResponse.ok();
    }

    /**
     * 批准一个授权策略。
     *
     * @param id ID
     * @return 授权策略
     */
    @PostMapping("/{id}/approve")
    public ApiResponse<PolicyItem> approve(@PathVariable Long id) {
        return ApiResponse.ok(policyService.approve(id));
    }

    /**
     * 撤销一个授权策略。
     *
     * @param id ID
     * @return 授权策略
     */
    @PostMapping("/{id}/revoke")
    public ApiResponse<PolicyItem> revoke(@PathVariable Long id) {
        return ApiResponse.ok(policyService.revoke(id));
    }
}
