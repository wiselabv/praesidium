package io.github.wiselabv.praesidium.admin.identity.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import io.github.wiselabv.praesidium.admin.identity.application.dto.ClientInfo;
import io.github.wiselabv.praesidium.admin.identity.application.dto.LoginCodeRequest;
import io.github.wiselabv.praesidium.admin.identity.application.dto.LoginDomainRequest;
import io.github.wiselabv.praesidium.admin.identity.application.dto.LoginMfaRequest;
import io.github.wiselabv.praesidium.admin.identity.application.dto.LoginPasswordRequest;
import io.github.wiselabv.praesidium.admin.identity.application.dto.LoginPasswordResponse;
import io.github.wiselabv.praesidium.admin.identity.application.dto.LogoutRequest;
import io.github.wiselabv.praesidium.admin.identity.application.dto.MeResponse;
import io.github.wiselabv.praesidium.admin.identity.application.dto.RecoverLoginRequest;
import io.github.wiselabv.praesidium.admin.identity.application.dto.RefreshRequest;
import io.github.wiselabv.praesidium.admin.identity.application.dto.TokenResponse;
import io.github.wiselabv.praesidium.admin.identity.domain.RecoveryCodeRepository;
import io.github.wiselabv.praesidium.admin.identity.domain.RefreshTokenRepository;
import io.github.wiselabv.praesidium.admin.identity.domain.SsoProviderRepository;
import io.github.wiselabv.praesidium.admin.identity.domain.UserRepository;
import io.github.wiselabv.praesidium.admin.identity.domain.UserThirdPartyRepository;
import io.github.wiselabv.praesidium.admin.identity.domain.event.UserLoggedIn;
import io.github.wiselabv.praesidium.admin.identity.domain.event.UserLoginFailed;
import io.github.wiselabv.praesidium.admin.identity.domain.model.RecoveryCode;
import io.github.wiselabv.praesidium.admin.identity.domain.model.RefreshToken;
import io.github.wiselabv.praesidium.admin.identity.domain.model.User;
import io.github.wiselabv.praesidium.admin.identity.domain.model.UserThirdParty;
import io.github.wiselabv.praesidium.admin.shared.api.ApiErrorCode;
import io.github.wiselabv.praesidium.admin.shared.api.BizException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证应用服务：登录（密码 → MFA 两阶段）、验证码/域账号/恢复码/SSO 登录、刷新、登出、当前用户。
 *
 * <p>一个公开方法对应一个用例；登录成功发布 {@link UserLoggedIn} 领域事件
 * （audit 上下文订阅落登录日志）。验证码/SSO/LDAP 演示模式说明见各方法注释。
 */
@Service
public class AuthService {

    /** 用户不存在时也参与一次 BCrypt 比较，抹平「用户存在与否」的响应时序差（防枚举） */
    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RecoveryCodeRepository recoveryCodeRepository;
    private final UserThirdPartyRepository thirdPartyRepository;
    private final SsoProviderRepository ssoProviderRepository;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final TotpService totpService;
    private final SecurityService securityService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       RecoveryCodeRepository recoveryCodeRepository,
                       UserThirdPartyRepository thirdPartyRepository,
                       SsoProviderRepository ssoProviderRepository,
                       JwtTokenService jwtTokenService,
                       JwtProperties jwtProperties,
                       TotpService totpService,
                       SecurityService securityService,
                       PasswordEncoder passwordEncoder,
                       ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.recoveryCodeRepository = recoveryCodeRepository;
        this.thirdPartyRepository = thirdPartyRepository;
        this.ssoProviderRepository = ssoProviderRepository;
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
        this.totpService = totpService;
        this.securityService = securityService;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    /** 阶段一：密码登录。启用 MFA 的用户返回会话令牌进入阶段二，否则直接完成登录 */
    @Transactional
    public LoginPasswordResponse loginWithPassword(LoginPasswordRequest request, ClientInfo client) {
        User user = userRepository.findByUsername(request.username().trim()).orElse(null);
        boolean matched = passwordEncoder.matches(request.password(),
                user != null ? user.getPasswordHash() : DUMMY_PASSWORD_HASH);
        if (user == null || !matched) {
            publishFailed(null, request.username().trim(), client, "password", "用户名或密码错误");
            throw new BizException(ApiErrorCode.AUTH_BAD_CREDENTIALS, "用户名或密码错误");
        }
        if (!user.isActive()) {
            publishFailed(user.getId(), user.getUsername(), client, "password", "账号已禁用");
            throw new BizException(ApiErrorCode.AUTH_ACCOUNT_DISABLED, "账号已被禁用，请联系管理员");
        }
        if (user.isMfaEnabled()) {
            return new LoginPasswordResponse("mfa", jwtTokenService.issueSessionToken(user.getId()), null);
        }
        return new LoginPasswordResponse("done", null, issueTokenPair(user, client, "password"));
    }

    /** 阶段二：MFA 验证。校验会话令牌与 TOTP 动态码，通过后签发正式令牌对 */
    @Transactional
    public TokenResponse loginWithMfa(LoginMfaRequest request, ClientInfo client) {
        Long userId = jwtTokenService.parseSessionToken(request.sessionToken());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ApiErrorCode.AUTH_SESSION_EXPIRED, "登录会话无效，请重新发起登录"));
        if (!user.isActive()) {
            throw new BizException(ApiErrorCode.AUTH_ACCOUNT_DISABLED, "账号已被禁用，请联系管理员");
        }
        if (!user.isMfaEnabled() || !totpVerify(user, request.code())) {
            publishFailed(user.getId(), user.getUsername(), client, "password", "MFA 验证码错误");
            throw new BizException(ApiErrorCode.AUTH_MFA_CODE_INVALID, "验证码错误，请重试");
        }
        return issueTokenPair(user, client, "password");
    }

    /** 验证码登录：用户名 + 短信/邮件验证码（演示模式固定 123456，见 {@link SecurityService#DEMO_CODE}） */
    @Transactional
    public TokenResponse loginWithCode(LoginCodeRequest request, ClientInfo client) {
        User user = userRepository.findByUsername(request.username().trim()).orElse(null);
        if (user == null || !SecurityService.DEMO_CODE.equals(request.code())) {
            publishFailed(user != null ? user.getId() : null, request.username().trim(),
                    client, "code", "用户名或验证码错误");
            throw new BizException(ApiErrorCode.AUTH_BAD_CREDENTIALS, "用户名或验证码错误");
        }
        if (!user.isActive()) {
            publishFailed(user.getId(), user.getUsername(), client, "code", "账号已禁用");
            throw new BizException(ApiErrorCode.AUTH_ACCOUNT_DISABLED, "账号已被禁用，请联系管理员");
        }
        return issueTokenPair(user, client, "code");
    }

    /**
     * 域账号（LDAP/AD）登录：username 形如 PRAESIDIUM\admin 或 admin@domain，解析出本地账号名后校验。
     *
     * <p>演示模式：域凭据回落本地 BCrypt 校验（网关接入后由 LDAP 绑定替换）。
     */
    @Transactional
    public TokenResponse loginWithDomain(LoginDomainRequest request, ClientInfo client) {
        String localUsername = parseDomainUsername(request.username().trim());
        User user = userRepository.findByUsername(localUsername).orElse(null);
        boolean matched = passwordEncoder.matches(request.password(),
                user != null ? user.getPasswordHash() : DUMMY_PASSWORD_HASH);
        if (user == null || !matched) {
            publishFailed(null, request.username().trim(), client, "domain", "域账号或密码错误");
            throw new BizException(ApiErrorCode.AUTH_LDAP_FAILED, "域账号或密码错误");
        }
        if (!user.isActive()) {
            publishFailed(user.getId(), user.getUsername(), client, "domain", "账号已禁用");
            throw new BizException(ApiErrorCode.AUTH_ACCOUNT_DISABLED, "账号已被禁用，请联系管理员");
        }
        return issueTokenPair(user, client, "domain");
    }

    /** 恢复码登录：MFA 设备不可用时的一次性恢复码登录（跳过 TOTP，用后即焚） */
    @Transactional
    public TokenResponse recoverLogin(RecoverLoginRequest request, ClientInfo client) {
        User user = userRepository.findByUsername(request.username().trim())
                .orElseThrow(() -> new BizException(ApiErrorCode.AUTH_BAD_CREDENTIALS, "用户名或恢复码错误"));
        if (!user.isActive()) {
            publishFailed(user.getId(), user.getUsername(), client, "recovery", "账号已禁用");
            throw new BizException(ApiErrorCode.AUTH_ACCOUNT_DISABLED, "账号已被禁用，请联系管理员");
        }
        String codeHash = SecurityService.sha256(SecurityService.normalizeCode(request.recoveryCode()));
        RecoveryCode code = recoveryCodeRepository.findUnusedByUserIdAndHash(user.getId(), codeHash)
                .orElseThrow(() -> {
                    publishFailed(user.getId(), user.getUsername(), client, "recovery", "恢复码无效或已使用");
                    return new BizException(ApiErrorCode.AUTH_RECOVERY_CODE_INVALID, "恢复码无效或已使用");
                });
        code.consume();
        recoveryCodeRepository.save(code);
        return issueTokenPair(user, client, "recovery");
    }

    /**
     * SSO 回调：按提供商下第一个第三方绑定定位用户（演示模式，真实场景先做 OAuth 令牌交换）。
     */
    @Transactional
    public TokenResponse ssoCallback(String provider, ClientInfo client) {
        ssoProviderRepository.findByProvider(provider)
                .filter(io.github.wiselabv.praesidium.admin.identity.domain.model.SsoProvider::isEnabled)
                .orElseThrow(() -> new BizException(ApiErrorCode.AUTH_SSO_FAILED, "该登录方式未启用"));
        UserThirdParty binding = thirdPartyRepository.findFirstByProvider(provider)
                .orElseThrow(() -> new BizException(ApiErrorCode.AUTH_SSO_FAILED,
                        "该登录方式未绑定任何用户，请先在个人中心绑定"));
        User user = userRepository.findById(binding.getUserId())
                .orElseThrow(() -> new BizException(ApiErrorCode.AUTH_SSO_FAILED, "绑定的用户不存在"));
        if (!user.isActive()) {
            throw new BizException(ApiErrorCode.AUTH_ACCOUNT_DISABLED, "账号已被禁用，请联系管理员");
        }
        return issueTokenPair(user, client, "sso");
    }

    /** 刷新令牌对：旧刷新令牌吊销（轮换），签发新的令牌对 */
    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(sha256(request.refreshToken()))
                .orElseThrow(() -> new BizException(ApiErrorCode.AUTH_REFRESH_INVALID, "刷新令牌无效或已过期"));
        if (!stored.isUsable()) {
            throw new BizException(ApiErrorCode.AUTH_REFRESH_INVALID, "刷新令牌无效或已过期");
        }
        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new BizException(ApiErrorCode.AUTH_REFRESH_INVALID, "刷新令牌无效或已过期"));
        if (!user.isActive()) {
            throw new BizException(ApiErrorCode.AUTH_ACCOUNT_DISABLED, "账号已被禁用，请联系管理员");
        }
        stored.revoke();
        refreshTokenRepository.save(stored);
        return issueTokenPair(user, null, null);
    }

    /** 登出：吊销刷新令牌（幂等，令牌不存在也视为成功） */
    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenRepository.findByTokenHash(sha256(request.refreshToken())).ifPresent(stored -> {
            stored.revoke();
            refreshTokenRepository.save(stored);
        });
    }

    /** 当前用户信息 */
    @Transactional(readOnly = true)
    public MeResponse me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ApiErrorCode.UNAUTHORIZED, "登录状态已失效"));
        return MeResponse.from(user);
    }

    /** 签发令牌对：访问令牌（JWT）+ 不透明刷新令牌（库存哈希）；登录场景发布登录事件（埋点） */
    private TokenResponse issueTokenPair(User user, ClientInfo client, String method) {
        String accessToken = jwtTokenService.issueAccessToken(user);
        String refreshToken = newRefreshToken();
        refreshTokenRepository.save(RefreshToken.issue(
                user.getId(),
                sha256(refreshToken),
                Instant.now().plusSeconds(jwtProperties.getRefreshTtlDays() * 86_400)));
        if (client != null) {
            eventPublisher.publishEvent(new UserLoggedIn(
                    user.getId(), user.getUsername(), client.ip(), method, client.userAgent()));
        }
        return new TokenResponse(accessToken, refreshToken,
                jwtTokenService.accessTtlSeconds(), MeResponse.from(user));
    }

    /** 发布登录失败事件（审计埋点）；client 为空（如刷新链路）时跳过 */
    private void publishFailed(Long userId, String username, ClientInfo client, String method, String reason) {
        if (client == null) {
            return;
        }
        eventPublisher.publishEvent(new UserLoginFailed(userId, username, client.ip(), method, reason, client.userAgent()));
    }

    /** 不透明刷新令牌：48 字节熵，Base64url 无填充 */
    private String newRefreshToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** 校验 TOTP 动态码（RFC 6238，±1 时间步窗口，常数时间比较） */
    private boolean totpVerify(User user, String code) {
        return totpService.verify(user.getTotpSecret(), code);
    }

    /** 解析域账号为本地账号名：PRAESIDIUM\admin 与 admin@domain 形式均取账号部分 */
    private String parseDomainUsername(String raw) {
        int backslash = raw.indexOf('\\');
        if (backslash >= 0 && backslash < raw.length() - 1) {
            return raw.substring(backslash + 1);
        }
        int at = raw.indexOf('@');
        if (at > 0) {
            return raw.substring(0, at);
        }
        return raw;
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 不可用", ex);
        }
    }
}
