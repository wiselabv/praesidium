package io.github.wiselabv.praesidium.admin.sessions.domain.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 会话实体：一次「用户 → 资产账号」的访问会话。
 *
 * <p>status：online（进行中）/ ended（已结束）；结束且存在录像路径的会话即录像记录，
 * 一张表同时服务「在线会话」与「会话录像」两个视图。
 */
@Entity
@Table(name = "sessions")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "account_id")
    private Long accountId;

    @Column(nullable = false, length = 16)
    private String protocol;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "source_ip", length = 45)
    private String sourceIp;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "recording_path", length = 255)
    private String recordingPath;

    @Column(name = "recording_size_bytes")
    private Long recordingSizeBytes;

    /** JPA 要求 */
    protected Session() {
    }

    private Session(Long userId, Long assetId, Long accountId, String protocol, String sourceIp) {
        this.userId = userId;
        this.assetId = assetId;
        this.accountId = accountId;
        this.protocol = protocol;
        this.sourceIp = sourceIp;
        this.status = "online";
        this.startedAt = Instant.now();
    }

    /** 发起连接：创建进行中的会话 */
    public static Session connect(Long userId, Long assetId, Long accountId, String protocol, String sourceIp) {
        return new Session(userId, assetId, accountId, protocol, sourceIp);
    }

    /** 断开会话：online → ended */
    public void disconnect() {
        this.status = "ended";
        this.endedAt = Instant.now();
    }

    /** 追加录像切片索引（网关逐片上报）：路径逗号拼接，大小累加 */
    public void appendRecording(String objectKey, long sizeBytes) {
        this.recordingPath = (recordingPath == null || recordingPath.isBlank())
                ? objectKey : recordingPath + "," + objectKey;
        this.recordingSizeBytes = (recordingSizeBytes == null ? 0 : recordingSizeBytes) + sizeBytes;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getAssetId() {
        return assetId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getStatus() {
        return status;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public String getRecordingPath() {
        return recordingPath;
    }

    public Long getRecordingSizeBytes() {
        return recordingSizeBytes;
    }
}
