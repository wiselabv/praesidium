package io.github.wiselabv.praesidium.admin.audit.api;

import java.util.List;

import io.github.wiselabv.praesidium.admin.audit.application.AuditService;
import io.github.wiselabv.praesidium.admin.audit.application.dto.OperationLogItem;
import io.github.wiselabv.praesidium.admin.shared.api.ApiResponse;
import io.github.wiselabv.praesidium.admin.shared.api.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作审计日志接口：分页查询 + 最近动态。
 */
@RestController
@RequestMapping("/api/logs/operations")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ApiResponse<PageResponse<OperationLogItem>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String risk,
            @RequestParam(required = false) String result) {
        return ApiResponse.ok(auditService.list(keyword, risk, result, page, size));
    }

    /** 最近 N 条操作（总览页「最近动态」） */
    @GetMapping("/recent")
    public ApiResponse<List<OperationLogItem>> recent(
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(auditService.recent(limit));
    }
}
