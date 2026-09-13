package io.github.wiselabv.praesidium.admin.sessions.application.dto;

import java.time.Instant;

import io.github.wiselabv.praesidium.admin.sessions.domain.model.Session;

/** 会话录像列表项 */
public record RecordingItem(
        Long id,
        Long sessionId,
        String user,
        String asset,
        String protocol,
        Instant startedAt,
        long durationSeconds,
        long sizeBytes,
        String path) {

    public static RecordingItem of(Session session, String user, String asset) {
        long duration = session.getEndedAt() == null ? 0
                : session.getEndedAt().getEpochSecond() - session.getStartedAt().getEpochSecond();
        return new RecordingItem(session.getId(), session.getId(), user, asset, session.getProtocol(),
                session.getStartedAt(), Math.max(duration, 0),
                session.getRecordingSizeBytes() == null ? 0 : session.getRecordingSizeBytes(),
                session.getRecordingPath());
    }
}
