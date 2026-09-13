package io.github.wiselabv.praesidium.admin.identity.domain.model;

/**
 * 用户账号状态。
 */
public enum UserStatus {
    /** 正常 */
    ACTIVE,
    /** 已禁用（禁用后拒绝登录与刷新令牌） */
    DISABLED
}
