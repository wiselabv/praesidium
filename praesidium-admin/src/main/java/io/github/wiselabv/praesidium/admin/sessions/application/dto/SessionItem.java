package io.github.wiselabv.praesidium.admin.sessions.application.dto;

import java.time.Instant;

import io.github.wiselabv.praesidium.admin.sessions.domain.model.Session;

/** 会话列表项（冗余用户名/资产名/账号名，时长秒） */
public record SessionItem(
        Long id,
        String user,
        String asset,
        String account,
        String protocol,
        String status,
        String sourceIp,
        Instant startedAt,
        long durationSeconds) {

    public static SessionItem of(Session session, String user, String asset, String account) {
        long duration = 0;
        if (session.getEndedAt() != null) {
            duration = session.getEndedAt().getEpochSecond() - session.getStartedAt().getEpochSecond();
        } else {
            duration = Instant.now().getEpochSecond() - session.getStartedAt().getEpochSecond();
        }
        return new SessionItem(session.getId(), user, asset, account, session.getProtocol(),
                session.getStatus(), session.getSourceIp(), session.getStartedAt(), Math.max(duration, 0));
    }
}
