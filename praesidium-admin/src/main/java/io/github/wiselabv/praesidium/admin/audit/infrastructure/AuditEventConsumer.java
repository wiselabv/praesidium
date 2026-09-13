package io.github.wiselabv.praesidium.admin.audit.infrastructure;

import io.github.wiselabv.praesidium.admin.audit.domain.AuditLogRepository;
import io.github.wiselabv.praesidium.admin.audit.domain.model.AuditLog;
import io.github.wiselabv.praesidium.admin.sessions.domain.SessionRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 审计事件消费者：Rust 网关发布的会话事件落库。
 *
 * <ul>
 *   <li>{@code session.started}：会话已在 connect 时创建，此处仅确认；</li>
 *   <li>{@code session.ended}：会话状态收口（online → ended）；</li>
 *   <li>{@code session.command}：命令审计落 audit_logs；</li>
 *   <li>{@code session.recording.index}：录像切片路径/大小回填 sessions。</li>
 * </ul>
 */
@Component
public class AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);

    private final SessionRepository sessionRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditEventConsumer(SessionRepository sessionRepository,
                              AuditLogRepository auditLogRepository,
                              ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "${praesidium.audit.java-queue:praesidium.java.audit}")
    public void onEvent(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String event = node.path("event").asText("");
            // Rust 信封为扁平结构（AuditEnvelope + serde flatten），事件字段在顶层
            switch (event) {
                case "session.ended" -> handleSessionEnded(node);
                case "session.command" -> handleCommand(node);
                case "session.recording.index" -> handleRecordingIndex(node);
                default -> log.debug("忽略审计事件 event={}", event);
            }
        } catch (Exception ex) {
            // 单条消息失败不阻塞队列：记录后 ack（事件语义幂等，可经会话表兜底）
            log.warn("审计事件处理失败: {}", ex.getMessage());
        }
    }

    @Transactional
    void handleSessionEnded(JsonNode node) {
        sessionRepository.findById(node.path("session_id").asLong())
                .ifPresent(session -> {
                    if ("online".equals(session.getStatus())) {
                        session.disconnect();
                        sessionRepository.save(session);
                    }
                });
    }

    @Transactional
    void handleCommand(JsonNode node) {
        AuditLog logEntry = AuditLog.record(
                node.path("user_id").asLong(),
                node.hasNonNull("asset_id") ? node.path("asset_id").asLong() : null,
                null,
                "command",
                node.path("command").asText(""),
                "success",
                "low",
                null);
        auditLogRepository.save(logEntry);
    }

    @Transactional
    void handleRecordingIndex(JsonNode node) {
        sessionRepository.findById(node.path("session_id").asLong())
                .ifPresent(session -> {
                    session.appendRecording(
                            node.path("object_key").asText(""),
                            node.path("size_bytes").asLong());
                    sessionRepository.save(session);
                });
    }
}
