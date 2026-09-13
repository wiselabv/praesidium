package io.github.wiselabv.praesidium.admin.audit.application.dto;

import java.time.Instant;

import io.github.wiselabv.praesidium.admin.audit.domain.model.AuditLog;

/** 操作日志列表项（冗余用户名 / 资产名 / 账号名） */
public record OperationLogItem(
        Long id,
        Instant time,
        String user,
        String asset,
        String account,
        String action,
        String result,
        String risk,
        String detail,
        String sourceIp) {

    public static OperationLogItem of(AuditLog log, String user, String asset, String account) {
        return new OperationLogItem(log.getId(), log.getCreatedAt(), user, asset, account,
                log.getAction(), log.getResult(), log.getRisk(), log.getCommandDetail(), log.getSourceIp());
    }
}
