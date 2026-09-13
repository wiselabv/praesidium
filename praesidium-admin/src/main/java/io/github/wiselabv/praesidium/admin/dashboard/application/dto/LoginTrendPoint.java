package io.github.wiselabv.praesidium.admin.dashboard.application.dto;

/** 按天登录趋势点（成功 / 失败分列） */
public record LoginTrendPoint(String date, long success, long failed) {
}
