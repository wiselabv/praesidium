package io.github.wiselabv.praesidium.admin.audit.application.dto;

import java.time.Instant;

import io.github.wiselabv.praesidium.admin.audit.domain.model.LoginLog;

/** 登录日志列表项 */
public record LoginLogItem(
        Long id,
        Instant time,
        String user,
        String sourceIp,
        String location,
        String method,
        String result,
        String reason,
        String client) {

    public static LoginLogItem from(LoginLog log) {
        return new LoginLogItem(log.getId(), log.getCreatedAt(), log.getUsername(), log.getSourceIp(),
                log.getLocation(), log.getMethod(), log.getResult(), log.getReason(), log.getClient());
    }
}
