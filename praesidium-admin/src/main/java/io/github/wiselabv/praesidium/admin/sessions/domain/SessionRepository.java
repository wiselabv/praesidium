package io.github.wiselabv.praesidium.admin.sessions.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import io.github.wiselabv.praesidium.admin.sessions.domain.model.Session;

/**
 * 会话仓储（领域层接口）。
 */
public interface SessionRepository {

    Optional<Session> findById(Long id);

    Session save(Session session);

    /** 分页查询（keyword 模糊匹配协议/来源 IP，status 精确过滤），开始时间倒序 */
    List<Session> findPage(String keyword, String status, int offset, int limit);

    long countPage(String keyword, String status);

    long countByStatus(String status);

    /** 统计：自某个时间点以来发起的会话数 */
    long countStartedSince(Instant since);

    /** 按天统计会话数：返回 [date, count] 行（总览趋势图用） */
    List<Object[]> countDailySince(Instant since);

    /** 录像分页（已结束且有录像的会话） */
    List<Session> findRecordings(String keyword, int offset, int limit);

    long countRecordings(String keyword);

    /** 某状态下最近 N 条（总览在线会话列表用） */
    List<Session> findRecentByStatus(String status, int limit);
}
