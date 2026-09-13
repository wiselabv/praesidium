package io.github.wiselabv.praesidium.admin.settings.domain.model;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 系统设置实体：一个分组（section）一行，配置内容以 JSONB 存储。
 *
 * <p>预置分组：basic（基础）、security（安全策略）、notify（通知）、auth（认证）、storage（存储）。
 */
@Entity
@Table(name = "settings")
public class Setting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 16)
    private String section;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String config;

    /** JPA 要求 */
    protected Setting() {
    }

    public Setting(String section, String config) {
        this.section = section;
        this.config = config;
    }

    /** 整体替换配置内容 */
    public void replaceConfig(String config) {
        this.config = config;
    }

    public Long getId() {
        return id;
    }

    public String getSection() {
        return section;
    }

    public String getConfig() {
        return config;
    }
}
