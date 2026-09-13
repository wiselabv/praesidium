package io.github.wiselabv.praesidium.admin.dashboard.application.dto;

/** 总览卡片指标（今日 = 当天 0 点以来） */
public record DashboardOverview(
        long totalAssets,
        long totalUsers,
        long onlineSessions,
        long todaySessions,
        long todayLogins,
        long todayFailedLogins,
        long todayOperations,
        long todayBlocked) {
}
