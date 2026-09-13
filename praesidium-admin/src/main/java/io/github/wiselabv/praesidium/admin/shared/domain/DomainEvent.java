package io.github.wiselabv.praesidium.admin.shared.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * 领域事件基类：跨上下文协作的通道之一（另一个是对方的 application 层接口）。
 *
 * <p>事件名用过去式表达「已发生」的事实，携带事件标识与发生时间，
 * 订阅方（如 audit 上下文）自行决定如何消费，发布方不关心后果。
 */
public abstract class DomainEvent {

    private final UUID eventId;
    private final Instant occurredAt;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
