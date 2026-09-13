package io.github.wiselabv.praesidium.admin.audit.domain;

import java.time.Instant;
import java.util.List;

import io.github.wiselabv.praesidium.admin.audit.domain.model.LoginLog;

/**
 * 登录日志仓储（领域层接口）。
 */
public interface LoginLogRepository {

    LoginLog save(LoginLog log);

    /** 分页查询（keyword 模糊匹配用户名/IP，result 精确过滤） */
    List<LoginLog> findPage(String keyword, String result, int offset, int limit);

    long countPage(String keyword, String result);

    /** 统计：自某个时间点以来的登录尝试数 */
    long countSince(Instant since);

    /** 统计：自某个时间点以来的失败登录数 */
    long countFailedSince(Instant since);

    /** 按天按结果统计：返回 [date, result, count] 行（总览登录趋势用） */
    List<Object[]> countDailySince(Instant since);
}
