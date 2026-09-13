package io.github.wiselabv.praesidium.admin.shared.api;

/**
 * 全站业务错误码（{@link ApiResponse#code()}）。
 *
 * <p>分段约定：1xxx 身份认证，2xxx 资产，3xxx 访问控制，4xxx 审计，
 * 5xxx 系统与公共。HTTP 语义：仅 401/403 由安全层映射为对应 HTTP 状态码，其余业务码一律 HTTP 200。
 */
public final class ApiErrorCode {

    private ApiErrorCode() {
    }

    // ---------- 公共 ----------
    /** 参数校验失败 */
    public static final int BAD_REQUEST = 400;
    /** 未认证（HTTP 401） */
    public static final int UNAUTHORIZED = 401;
    /** 无权限（HTTP 403） */
    public static final int FORBIDDEN = 403;
    /** 系统内部错误 */
    public static final int INTERNAL_ERROR = 500;

    // ---------- 1xxx 身份认证 ----------
    /** 用户名或密码错误 */
    public static final int AUTH_BAD_CREDENTIALS = 1001;
    /** MFA 验证码错误 */
    public static final int AUTH_MFA_CODE_INVALID = 1002;
    /** 登录会话（sessionToken）无效或已过期 */
    public static final int AUTH_SESSION_EXPIRED = 1003;
    /** 刷新令牌无效或已撤销 */
    public static final int AUTH_REFRESH_INVALID = 1004;
    /** 账号已禁用 */
    public static final int AUTH_ACCOUNT_DISABLED = 1005;
    /** 恢复码无效或已使用 */
    public static final int AUTH_RECOVERY_CODE_INVALID = 1006;
    /** 短信/邮件验证码错误 */
    public static final int AUTH_CODE_INVALID = 1007;
    /** 验证码发送过于频繁 */
    public static final int AUTH_CODE_TOO_FREQUENT = 1008;
    /** 该验证方式未绑定 */
    public static final int AUTH_METHOD_NOT_BOUND = 1009;
    /** 原密码错误 */
    public static final int AUTH_OLD_PASSWORD_WRONG = 1010;
    /** SSO 登录失败 */
    public static final int AUTH_SSO_FAILED = 1011;
    /** LDAP 登录失败 */
    public static final int AUTH_LDAP_FAILED = 1012;
    /** 该方式已绑定 */
    public static final int AUTH_ALREADY_BOUND = 1013;

    // ---------- 2xxx 资产 ----------
    /** 资产不存在 */
    public static final int ASSET_NOT_FOUND = 2001;
    /** 资产名称已存在 */
    public static final int ASSET_NAME_EXISTS = 2002;
    /** 资产账号已存在 */
    public static final int ASSET_ACCOUNT_EXISTS = 2003;
    /** 资产账号不存在 */
    public static final int ASSET_ACCOUNT_NOT_FOUND = 2004;
    /** 凭据不存在 */
    public static final int CREDENTIAL_NOT_FOUND = 2005;
    /** 凭据仍被账号引用 */
    public static final int CREDENTIAL_IN_USE = 2006;

    // ---------- 3xxx 访问控制 ----------
    /** 授权策略不存在 */
    public static final int POLICY_NOT_FOUND = 3001;
    /** 无该资产的访问授权策略（建连被拒） */
    public static final int POLICY_DENIED = 3002;

    // ---------- 4xxx 审计 ----------
    /** 审计日志不存在 */
    public static final int LOG_NOT_FOUND = 4001;

    // ---------- 5xxx 系统与公共 ----------
    /** 会话不存在 */
    public static final int SESSION_NOT_FOUND = 5001;
    /** 配置分组不存在 */
    public static final int SETTINGS_SECTION_NOT_FOUND = 5002;
}
