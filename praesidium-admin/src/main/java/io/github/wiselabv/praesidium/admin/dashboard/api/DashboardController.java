package io.github.wiselabv.praesidium.admin.dashboard.api;

import java.util.List;

import io.github.wiselabv.praesidium.admin.audit.application.dto.OperationLogItem;
import io.github.wiselabv.praesidium.admin.dashboard.application.DashboardService;
import io.github.wiselabv.praesidium.admin.dashboard.application.dto.DashboardOverview;
import io.github.wiselabv.praesidium.admin.dashboard.application.dto.LoginTrendPoint;
import io.github.wiselabv.praesidium.admin.dashboard.application.dto.NameCount;
import io.github.wiselabv.praesidium.admin.dashboard.application.dto.TrendPoint;
import io.github.wiselabv.praesidium.admin.sessions.application.dto.SessionItem;
import io.github.wiselabv.praesidium.admin.shared.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 总览统计接口：卡片指标、分布、趋势、最近动态与在线会话。
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /** 总览卡片指标 */
    @GetMapping("/overview")
    public ApiResponse<DashboardOverview> overview() {
        return ApiResponse.ok(dashboardService.overview());
    }

    /** 资产类型分布 */
    @GetMapping("/asset-types")
    public ApiResponse<List<NameCount>> assetTypes() {
        return ApiResponse.ok(dashboardService.assetTypes());
    }

    /** 协议分布 */
    @GetMapping("/protocols")
    public ApiResponse<List<NameCount>> protocols() {
        return ApiResponse.ok(dashboardService.protocols());
    }

    /** 近 N 天会话趋势（默认 7 天） */
    @GetMapping("/session-trend")
    public ApiResponse<List<TrendPoint>> sessionTrend(@RequestParam(defaultValue = "7") int days) {
        return ApiResponse.ok(dashboardService.sessionTrend(days));
    }

    /** 近 N 天登录趋势（默认 7 天） */
    @GetMapping("/login-trend")
    public ApiResponse<List<LoginTrendPoint>> loginTrend(@RequestParam(defaultValue = "7") int days) {
        return ApiResponse.ok(dashboardService.loginTrend(days));
    }

    /** 最近操作动态（默认 10 条） */
    @GetMapping("/recent-operations")
    public ApiResponse<List<OperationLogItem>> recentOperations(@RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(dashboardService.recentOperations(limit));
    }

    /** 在线会话列表（默认 5 条） */
    @GetMapping("/online-sessions")
    public ApiResponse<List<SessionItem>> onlineSessions(@RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.ok(dashboardService.onlineSessions(limit));
    }
}
