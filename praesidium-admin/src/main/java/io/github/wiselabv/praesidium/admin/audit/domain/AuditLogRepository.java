package io.github.wiselabv.praesidium.admin.audit.domain;

import java.time.Instant;
import java.util.List;

import io.github.wiselabv.praesidium.admin.audit.domain.model.AuditLog;

/**
 * 操作审计日志仓储（领域层接口）。
 */
public interface AuditLogRepository {

    AuditLog save(AuditLog log);

    /** 分页查询（keyword 模糊匹配操作/详情，risk、result 精确过滤） */
    List<AuditLog> findPage(String keyword, String risk, String result, int offset, int limit);

    long countPage(String keyword, String risk, String result);

    /** 最近 N 条（总览动态用） */
    List<AuditLog> findRecent(int limit);

    /** 统计：自某个时间点以来的日志总数 */
    long countSince(Instant since);

    /** 统计：自某个时间点以来被拦截（blocked）的日志数 */
    long countBlockedSince(Instant since);

    /** 按风险等级统计：返回 [risk, count] 行 */
    List<Object[]> countGroupByRiskSince(Instant since);
}
