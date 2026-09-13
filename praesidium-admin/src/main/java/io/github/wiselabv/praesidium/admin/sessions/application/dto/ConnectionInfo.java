package io.github.wiselabv.praesidium.admin.sessions.application.dto;

/**
 * 内网连接信息：Rust 网关凭此直连目标资产（JSON 字段 camelCase，与 Rust 侧反序列化约定一致）。
 */
public record ConnectionInfo(
        Long assetId,
        String host,
        int port,
        String protocol,
        String account,
        Long accountId,
        /** password | key */
        String authType,
        String secret) {
}
