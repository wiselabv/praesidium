package io.github.wiselabv.praesidium.admin.sessions.application.dto;

import io.github.wiselabv.praesidium.admin.sessions.domain.model.Session;

/**
 * 发起连接响应：会话摘要 + 网关接入信息。
 *
 * <p>前端持 {@code gatewayToken} 连 {@code gatewayUrl}（Rust 网关 WebSocket），
 * 令牌内含 sessionId/userId，网关凭此拉取解密凭据并校验授权策略。
 */
public record ConnectResponse(
        SessionItem session,
        String gatewayToken,
        String gatewayUrl) {

    public static ConnectResponse of(Session session, SessionItem item,
                                     String gatewayToken, String gatewayUrl) {
        return new ConnectResponse(item, gatewayToken, gatewayUrl);
    }
}
