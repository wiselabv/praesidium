package io.github.wiselabv.praesidium.admin.dashboard.application;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.wiselabv.praesidium.admin.asset.domain.AssetRepository;
import io.github.wiselabv.praesidium.admin.audit.application.AuditService;
import io.github.wiselabv.praesidium.admin.audit.application.dto.OperationLogItem;
import io.github.wiselabv.praesidium.admin.audit.domain.AuditLogRepository;
import io.github.wiselabv.praesidium.admin.audit.domain.LoginLogRepository;
import io.github.wiselabv.praesidium.admin.dashboard.application.dto.DashboardOverview;
import io.github.wiselabv.praesidium.admin.dashboard.application.dto.LoginTrendPoint;
import io.github.wiselabv.praesidium.admin.dashboard.application.dto.NameCount;
import io.github.wiselabv.praesidium.admin.dashboard.application.dto.TrendPoint;
import io.github.wiselabv.praesidium.admin.identity.domain.UserRepository;
import io.github.wiselabv.praesidium.admin.sessions.application.SessionService;
import io.github.wiselabv.praesidium.admin.sessions.application.dto.SessionItem;
import io.github.wiselabv.praesidium.admin.sessions.domain.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 总览统计应用服务：聚合各仓储的计数/分布查询，供总览页使用。
 */
@Service
public class DashboardService {

    private static final ZoneId ZONE = ZoneId.systemDefault();

    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final AuditLogRepository auditLogRepository;
    private final LoginLogRepository loginLogRepository;
    private final AuditService auditService;
    private final SessionService sessionService;

    public DashboardService(AssetRepository assetRepository,
                            UserRepository userRepository,
                            SessionRepository sessionRepository,
                            AuditLogRepository auditLogRepository,
                            LoginLogRepository loginLogRepository,
                            AuditService auditService,
                            SessionService sessionService) {
        this.assetRepository = assetRepository;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.auditLogRepository = auditLogRepository;
        this.loginLogRepository = loginLogRepository;
        this.auditService = auditService;
        this.sessionService = sessionService;
    }

    /** 总览卡片：资产/用户/会话/今日登录与操作等指标 */
    @Transactional(readOnly = true)
    public DashboardOverview overview() {
        Instant todayStart = todayStart();
        return new DashboardOverview(
                assetRepository.countAll(),
                userRepository.countAll(),
                sessionRepository.countByStatus("online"),
                sessionRepository.countStartedSince(todayStart),
                loginLogRepository.countSince(todayStart),
                loginLogRepository.countFailedSince(todayStart),
                auditLogRepository.countSince(todayStart),
                auditLogRepository.countBlockedSince(todayStart));
    }

    /** 资产类型分布 */
    @Transactional(readOnly = true)
    public List<NameCount> assetTypes() {
        return assetRepository.countGroupByType().stream()
                .map(row -> new NameCount(String.valueOf(row[0]), (Long) row[1]))
                .toList();
    }

    /** 协议分布 */
    @Transactional(readOnly = true)
    public List<NameCount> protocols() {
        return assetRepository.countGroupByProtocol().stream()
                .map(row -> new NameCount(String.valueOf(row[0]), (Long) row[1]))
                .toList();
    }

    /** 近 N 天会话趋势（缺失日期补零） */
    @Transactional(readOnly = true)
    public List<TrendPoint> sessionTrend(int days) {
        Instant since = Instant.now().minus(days, java.time.temporal.ChronoUnit.DAYS);
        Map<String, Long> byDate = new LinkedHashMap<>();
        for (Object[] row : sessionRepository.countDailySince(since)) {
            byDate.put(String.valueOf(row[0]), (Long) row[1]);
        }
        return fillDays(days).stream()
                .map(date -> new TrendPoint(date, byDate.getOrDefault(date, 0L)))
                .toList();
    }

    /** 近 N 天登录趋势（成功/失败分列，缺失日期补零） */
    @Transactional(readOnly = true)
    public List<LoginTrendPoint> loginTrend(int days) {
        Instant since = Instant.now().minus(days, java.time.temporal.ChronoUnit.DAYS);
        Map<String, long[]> byDate = new LinkedHashMap<>();
        for (Object[] row : loginLogRepository.countDailySince(since)) {
            long[] counters = byDate.computeIfAbsent(String.valueOf(row[0]), k -> new long[2]);
            if ("success".equals(row[1])) {
                counters[0] = (Long) row[2];
            } else if ("failed".equals(row[1])) {
                counters[1] = (Long) row[2];
            }
        }
        return fillDays(days).stream()
                .map(date -> {
                    long[] counters = byDate.getOrDefault(date, new long[2]);
                    return new LoginTrendPoint(date, counters[0], counters[1]);
                })
                .toList();
    }

    /** 最近操作动态 */
    @Transactional(readOnly = true)
    public List<OperationLogItem> recentOperations(int limit) {
        return auditService.recent(limit);
    }

    /** 在线会话列表 */
    @Transactional(readOnly = true)
    public List<SessionItem> onlineSessions(int limit) {
        return sessionService.recent("online", limit);
    }

    private Instant todayStart() {
        return LocalDate.now().atStartOfDay(ZONE).toInstant();
    }

    private List<String> fillDays(int days) {
        int safeDays = Math.max(1, Math.min(days, 90));
        List<String> dates = new ArrayList<>(safeDays);
        LocalDate cursor = LocalDate.now().minusDays(safeDays - 1L);
        for (int i = 0; i < safeDays; i++) {
            dates.add(cursor.toString());
            cursor = cursor.plusDays(1);
        }
        return dates;
    }
}
