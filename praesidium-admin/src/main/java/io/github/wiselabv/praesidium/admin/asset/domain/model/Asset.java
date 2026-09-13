package io.github.wiselabv.praesidium.admin.asset.domain.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 资产实体：被纳管的目标主机 / 数据库 / Web 站点。
 *
 * <p>type：host（主机）、db（数据库）、web（Web 站点）、network（网络设备）；
 * status：active（启用）、disabled（停用）；groupPath 为空时落「未分组」。
 * 与身份上下文一致：实体直接携带 JPA 注解，字段私有、无 setter，状态变更走行为方法。
 */
@Entity
@Table(name = "assets")
public class Asset {

    public static final String DEFAULT_GROUP = "未分组";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 16)
    private String type;

    @Column(nullable = false, length = 128)
    private String address;

    @Column(nullable = false, length = 16)
    private String protocol;

    private Integer port;

    @Column(name = "group_path", nullable = false, length = 255)
    private String groupPath;

    @Column(length = 512)
    private String description;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** JPA 要求 */
    protected Asset() {
    }

    private Asset(String name, String type, String address, String protocol, Integer port,
                  String groupPath, String description, Long createdBy) {
        Instant now = Instant.now();
        this.name = name;
        this.type = type;
        this.address = address;
        this.protocol = protocol;
        this.port = port;
        this.groupPath = normalizeGroup(groupPath);
        this.description = description;
        this.status = "active";
        this.createdBy = createdBy;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** 新建资产（状态默认 active） */
    public static Asset create(String name, String type, String address, String protocol,
                               Integer port, String groupPath, String description, Long createdBy) {
        return new Asset(name, type, address, protocol, port, groupPath, description, createdBy);
    }

    /** 修改资产基础信息 */
    public void update(String name, String type, String address, String protocol,
                       Integer port, String groupPath, String description) {
        this.name = name;
        this.type = type;
        this.address = address;
        this.protocol = protocol;
        this.port = port;
        this.groupPath = normalizeGroup(groupPath);
        this.description = description;
        this.updatedAt = Instant.now();
    }

    private static String normalizeGroup(String groupPath) {
        return groupPath == null || groupPath.isBlank() ? DEFAULT_GROUP : groupPath;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getAddress() {
        return address;
    }

    public String getProtocol() {
        return protocol;
    }

    public Integer getPort() {
        return port;
    }

    public String getGroupPath() {
        return groupPath;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
