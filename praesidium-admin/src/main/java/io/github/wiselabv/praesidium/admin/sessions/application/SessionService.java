package io.github.wiselabv.praesidium.admin.sessions.application;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.github.wiselabv.praesidium.admin.asset.domain.AssetAccountRepository;
import io.github.wiselabv.praesidium.admin.asset.domain.AssetRepository;
import io.github.wiselabv.praesidium.admin.asset.domain.model.Asset;
import io.github.wiselabv.praesidium.admin.asset.domain.model.AssetAccount;
import io.github.wiselabv.praesidium.admin.identity.application.JwtTokenService;
import io.github.wiselabv.praesidium.admin.identity.domain.UserRepository;
import io.github.wiselabv.praesidium.admin.identity.domain.model.User;
import io.github.wiselabv.praesidium.admin.sessions.application.dto.ConnectRequest;
import io.github.wiselabv.praesidium.admin.sessions.application.dto.ConnectResponse;
import io.github.wiselabv.praesidium.admin.sessions.application.dto.RecordingItem;
import io.github.wiselabv.praesidium.admin.sessions.application.dto.RecordingObjectItem;
import io.github.wiselabv.praesidium.admin.sessions.application.dto.SessionItem;
import io.github.wiselabv.praesidium.admin.sessions.domain.SessionRepository;
import io.github.wiselabv.praesidium.admin.sessions.domain.model.Session;
import io.github.wiselabv.praesidium.admin.sessions.infrastructure.MinioPresigner;
import io.github.wiselabv.praesidium.admin.shared.api.ApiErrorCode;
import io.github.wiselabv.praesidium.admin.shared.api.BizException;
import io.github.wiselabv.praesidium.admin.shared.api.PageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 会话应用服务：在线会话查询/断开、发起连接（签发网关令牌）、录像查询。
 */
@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final AssetRepository assetRepository;
    private final AssetAccountRepository accountRepository;
    private final JwtTokenService jwtTokenService;
    private final MinioPresigner minioPresigner;
    private final String gatewayUrl;

    public SessionService(SessionRepository sessionRepository,
                          UserRepository userRepository,
                          AssetRepository assetRepository,
                          AssetAccountRepository accountRepository,
                          JwtTokenService jwtTokenService,
                          MinioPresigner minioPresigner,
                          @Value("${praesidium.gateway.url:ws://127.0.0.1:8081}") String gatewayUrl) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.assetRepository = assetRepository;
        this.accountRepository = accountRepository;
        this.jwtTokenService = jwtTokenService;
        this.minioPresigner = minioPresigner;
        this.gatewayUrl = gatewayUrl;
    }

    @Transactional(readOnly = true)
    public PageResponse<SessionItem> list(String keyword, String status, int page, int size) {
        List<Session> sessions = sessionRepository.findPage(keyword, status, (page - 1) * size, size);
        long total = sessionRepository.countPage(keyword, status);
        return PageResponse.of(toItems(sessions), total, page, size);
    }

    /** 最近 N 条指定状态的会话（总览在线列表用） */
    @Transactional(readOnly = true)
    public List<SessionItem> recent(String status, int limit) {
        return toItems(sessionRepository.findRecentByStatus(status, limit));
    }

    /** 断开：online → ended */
    @Transactional
    public SessionItem disconnect(Long id) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new BizException(ApiErrorCode.SESSION_NOT_FOUND, "会话不存在"));
        if (!"online".equals(session.getStatus())) {
            throw new BizException(ApiErrorCode.BAD_REQUEST, "会话已结束，无需重复断开");
        }
        session.disconnect();
        sessionRepository.save(session);
        return toItem(session);
    }

    /** 发起连接：创建在线会话并签发网关令牌（浏览器 → Rust 网关建连） */
    @Transactional
    public ConnectResponse connect(ConnectRequest request, Long userId, String sourceIp) {
        Asset asset = assetRepository.findById(request.assetId())
                .orElseThrow(() -> new BizException(ApiErrorCode.ASSET_NOT_FOUND, "资产不存在"));
        if (request.accountId() != null) {
            AssetAccount account = accountRepository.findById(request.accountId())
                    .orElseThrow(() -> new BizException(ApiErrorCode.ASSET_ACCOUNT_NOT_FOUND, "资产账号不存在"));
            if (!account.getAssetId().equals(request.assetId())) {
                throw new BizException(ApiErrorCode.BAD_REQUEST, "账号不属于该资产");
            }
        }
        Session session = Session.connect(userId, request.assetId(), request.accountId(),
                request.protocol().trim(), sourceIp);
        sessionRepository.save(session);
        String token = jwtTokenService.issueGatewayToken(session.getId(), userId);
        return ConnectResponse.of(session, toItem(session), token, gatewayUrl);
    }

    /** 录像分页（已结束且有录像的会话） */
    @Transactional(readOnly = true)
    public PageResponse<RecordingItem> recordings(String keyword, int page, int size) {
        List<Session> sessions = sessionRepository.findRecordings(keyword, (page - 1) * size, size);
        long total = sessionRepository.countRecordings(keyword);
        List<Long> userIds = sessions.stream().map(Session::getUserId).distinct().toList();
        Map<Long, String> userNames = userRepository.findByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
        List<Long> assetIds = sessions.stream().map(Session::getAssetId).distinct().toList();
        Map<Long, String> assetNames = assetRepository.findByIds(assetIds).stream()
                .collect(Collectors.toMap(Asset::getId, Asset::getName));
        List<RecordingItem> items = sessions.stream()
                .map(s -> RecordingItem.of(s, userNames.get(s.getUserId()), assetNames.get(s.getAssetId())))
                .toList();
        return PageResponse.of(items, total, page, size);
    }

    /** 录像切片对象：recordingPath 逐个签发 MinIO 预签名下载 URL（回放/下载用） */
    @Transactional(readOnly = true)
    public List<RecordingObjectItem> recordingObjects(Long id) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new BizException(ApiErrorCode.SESSION_NOT_FOUND, "会话不存在"));
        String path = session.getRecordingPath();
        if (path == null || path.isBlank()) {
            return List.of();
        }
        List<RecordingObjectItem> objects = new java.util.ArrayList<>();
        for (String key : path.split(",")) {
            String trimmed = key.trim();
            if (!trimmed.isEmpty()) {
                objects.add(new RecordingObjectItem(trimmed, minioPresigner.presignGet(trimmed)));
            }
        }
        return objects;
    }

    private List<SessionItem> toItems(List<Session> sessions) {
        List<Long> userIds = sessions.stream().map(Session::getUserId).distinct().toList();
        Map<Long, String> userNames = userRepository.findByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
        List<Long> assetIds = sessions.stream().map(Session::getAssetId).distinct().toList();
        Map<Long, String> assetNames = assetRepository.findByIds(assetIds).stream()
                .collect(Collectors.toMap(Asset::getId, Asset::getName));
        List<Long> accountIds = sessions.stream().map(Session::getAccountId)
                .filter(id -> id != null).distinct().toList();
        Map<Long, String> accountNames = accountRepository.findByIds(accountIds).stream()
                .collect(Collectors.toMap(AssetAccount::getId, AssetAccount::getName));
        return sessions.stream()
                .map(s -> SessionItem.of(s, userNames.get(s.getUserId()),
                        assetNames.get(s.getAssetId()), accountNames.get(s.getAccountId())))
                .toList();
    }

    private SessionItem toItem(Session session) {
        return toItems(List.of(session)).getFirst();
    }
}
