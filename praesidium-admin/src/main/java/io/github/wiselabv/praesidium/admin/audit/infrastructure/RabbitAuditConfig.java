package io.github.wiselabv.praesidium.admin.audit.infrastructure;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 审计事件总线声明：topic 交换机 + Java 专属队列（绑定 session.#）。
 *
 * <p>Rust 网关发布到 {@code praesidium.audit} 交换机；Python 检测服务自建
 * 队列绑定 {@code session.command}，两边互不干扰。
 */
@Configuration
public class RabbitAuditConfig {

    @Bean
    public TopicExchange auditExchange(
            @Value("${praesidium.audit.exchange:praesidium.audit}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue javaAuditQueue(
            @Value("${praesidium.audit.java-queue:praesidium.java.audit}") String queue) {
        return new Queue(queue, true);
    }

    @Bean
    public Binding javaAuditBinding(TopicExchange auditExchange, Queue javaAuditQueue) {
        return BindingBuilder.bind(javaAuditQueue).to(auditExchange).with("session.#");
    }
}
