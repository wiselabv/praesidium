package io.github.wiselabv.praesidium.admin.sessions.application.dto;

import jakarta.validation.constraints.NotNull;

/** 发起连接请求（演示网关：创建一条在线会话记录） */
public record ConnectRequest(
        @NotNull(message = "不能为空") Long assetId,
        Long accountId,
        @NotNull(message = "不能为空") String protocol) {
}
