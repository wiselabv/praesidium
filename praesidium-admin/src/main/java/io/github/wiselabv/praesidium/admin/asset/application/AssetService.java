package io.github.wiselabv.praesidium.admin.asset.application;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.github.wiselabv.praesidium.admin.asset.application.dto.AssetItem;
import io.github.wiselabv.praesidium.admin.asset.application.dto.AssetRequest;
import io.github.wiselabv.praesidium.admin.asset.application.dto.AssetTestResult;
import io.github.wiselabv.praesidium.admin.asset.domain.AssetAccountRepository;
import io.github.wiselabv.praesidium.admin.asset.domain.AssetRepository;
import io.github.wiselabv.praesidium.admin.asset.domain.model.Asset;
import io.github.wiselabv.praesidium.admin.shared.api.ApiErrorCode;
import io.github.wiselabv.praesidium.admin.shared.api.BizException;
import io.github.wiselabv.praesidium.admin.shared.api.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 资产应用服务：资产 CRUD + 连通性测试。
 */
@Service
public class AssetService {

    /** 协议默认端口（资产未填端口时用于连通性测试） */
    private static final Map<String, Integer> DEFAULT_PORTS = Map.of(
            "SSH", 22, "RDP", 3389, "VNC", 5900, "Telnet", 23,
            "PostgreSQL", 5432, "MySQL", 3306, "Redis", 6379,
            "HTTP", 80, "HTTPS", 443);

    private final AssetRepository assetRepository;
    private final AssetAccountRepository accountRepository;

    public AssetService(AssetRepository assetRepository, AssetAccountRepository accountRepository) {
        this.assetRepository = assetRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AssetItem> list(String keyword, String type, int page, int size) {
        List<Asset> assets = assetRepository.findPage(keyword, type, (page - 1) * size, size);
        long total = assetRepository.countPage(keyword, type);
        Map<Long, Long> accountCounts = accountRepository.countGroupByAsset(
                assets.stream().map(Asset::getId).toList());
        List<AssetItem> items = assets.stream()
                .map(a -> AssetItem.from(a, accountCounts.getOrDefault(a.getId(), 0L)))
                .toList();
        return PageResponse.of(items, total, page, size);
    }

    @Transactional
    public AssetItem create(AssetRequest request, Long userId) {
        String name = request.name().trim();
        if (assetRepository.existsByName(name)) {
            throw new BizException(ApiErrorCode.ASSET_NAME_EXISTS, "资产名称已存在");
        }
        Asset asset = Asset.create(name, request.type().trim(), request.address().trim(),
                request.protocol().trim(), request.port(), request.groupPath(), request.description(), userId);
        assetRepository.save(asset);
        return AssetItem.from(asset, 0);
    }

    @Transactional
    public AssetItem update(Long id, AssetRequest request) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new BizException(ApiErrorCode.ASSET_NOT_FOUND, "资产不存在"));
        String name = request.name().trim();
        if (assetRepository.existsByNameExcludingId(name, id)) {
            throw new BizException(ApiErrorCode.ASSET_NAME_EXISTS, "资产名称已存在");
        }
        asset.update(name, request.type().trim(), request.address().trim(), request.protocol().trim(),
                request.port(), request.groupPath(), request.description());
        assetRepository.save(asset);
        long accountCount = accountRepository.countGroupByAsset(List.of(id)).getOrDefault(id, 0L);
        return AssetItem.from(asset, accountCount);
    }

    @Transactional
    public void delete(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new BizException(ApiErrorCode.ASSET_NOT_FOUND, "资产不存在"));
        assetRepository.delete(asset);
    }

    /** 连通性测试：TCP 连接（3 秒超时）；web 类资产支持 http(s) 地址自动解析主机与端口 */
    public AssetTestResult test(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new BizException(ApiErrorCode.ASSET_NOT_FOUND, "资产不存在"));
        String host = asset.getAddress();
        int port = asset.getPort() != null ? asset.getPort()
                : DEFAULT_PORTS.getOrDefault(asset.getProtocol(), 22);
        if (host.startsWith("http://") || host.startsWith("https://")) {
            try {
                URI uri = URI.create(host);
                if (uri.getHost() != null) {
                    host = uri.getHost();
                }
                if (uri.getPort() > 0) {
                    port = uri.getPort();
                }
            } catch (IllegalArgumentException ignored) {
                // 非法地址按原样尝试连接
            }
        }
        long start = System.nanoTime();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 3000);
            long latency = Duration.ofNanos(System.nanoTime() - start).toMillis();
            return new AssetTestResult(true, latency, host + ":" + port + " TCP 连接成功");
        } catch (Exception ex) {
            String reason = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            return new AssetTestResult(false, 0, host + ":" + port + " 连接失败：" + reason);
        }
    }
}
