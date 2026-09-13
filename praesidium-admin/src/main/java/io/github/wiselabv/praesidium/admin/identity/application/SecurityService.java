package io.github.wiselabv.praesidium.admin.identity.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

import io.github.wiselabv.praesidium.admin.identity.application.dto.EmailMfaToggleRequest;
import io.github.wiselabv.praesidium.admin.identity.application.dto.MfaBindRequest;
import io.github.wiselabv.praesidium.admin.identity.application.dto.MfaSetupResponse;
import io.github.wiselabv.praesidium.admin.identity.application.dto.RecoveryCodesResponse;
import io.github.wiselabv.praesidium.admin.identity.application.dto.SecuritySummaryResponse;
import io.github.wiselabv.praesidium.admin.identity.application.dto.SendCodeRequest;
import io.github.wiselabv.praesidium.admin.identity.application.dto.SmsBindRequest;
import io.github.wiselabv.praesidium.admin.identity.application.dto.WebauthnItem;
import io.github.wiselabv.praesidium.admin.identity.application.dto.WebauthnRegisterRequest;
import io.github.wiselabv.praesidium.admin.identity.domain.RecoveryCodeRepository;
import io.github.wiselabv.praesidium.admin.identity.domain.UserRepository;
import io.github.wiselabv.praesidium.admin.identity.domain.UserSecuritySettingsRepository;
import io.github.wiselabv.praesidium.admin.identity.domain.WebauthnCredentialRepository;
import io.github.wiselabv.praesidium.admin.identity.domain.model.RecoveryCode;
import io.github.wiselabv.praesidium.admin.identity.domain.model.User;
import io.github.wiselabv.praesidium.admin.identity.domain.model.UserSecuritySettings;
import io.github.wiselabv.praesidium.admin.identity.domain.model.WebauthnCredential;
import io.github.wiselabv.praesidium.admin.shared.api.ApiErrorCode;
import io.github.wiselabv.praesidium.admin.shared.api.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 安全设置应用服务：TOTP 绑定/解绑、MFA 恢复码、WebAuthn 设备、短信/邮件验证码绑定。
 *
 * <p>演示约定：短信/邮件验证码固定 123456（网关接入前不发真实短信），
 * WebAuthn 注册由后端生成凭据元数据（真实场景由浏览器 attestation 提供）。
 */
@Service
public class SecurityService {

    private static final Logger log = LoggerFactory.getLogger(SecurityService.class);

    /** 演示验证码（短信/邮件验证码登录与绑定共用） */
    public static final String DEMO_CODE = "123456";

    /** 单次生成的恢复码数量 */
    private static final int RECOVERY_CODE_COUNT = 10;

    /** TOTP 签发方（otpauth URI 展示名） */
    private static final String TOTP_ISSUER = "Praesidium";

    private final UserRepository userRepository;
    private final UserSecuritySettingsRepository securitySettingsRepository;
    private final RecoveryCodeRepository recoveryCodeRepository;
    private final WebauthnCredentialRepository webauthnRepository;
    private final TotpService totpService;
    private final SecureRandom secureRandom = new SecureRandom();

    public SecurityService(UserRepository userRepository,
                           UserSecuritySettingsRepository securitySettingsRepository,
                           RecoveryCodeRepository recoveryCodeRepository,
                           WebauthnCredentialRepository webauthnRepository,
                           TotpService totpService) {
        this.userRepository = userRepository;
        this.securitySettingsRepository = securitySettingsRepository;
        this.recoveryCodeRepository = recoveryCodeRepository;
        this.webauthnRepository = webauthnRepository;
        this.totpService = totpService;
    }

    /** 安全设置摘要（个人安全页） */
    @Transactional(readOnly = true)
    public SecuritySummaryResponse summary(Long userId) {
        User user = requireUser(userId);
        UserSecuritySettings settings = securitySettingsRepository.findByUserId(userId).orElse(null);
        return new SecuritySummaryResponse(
                user.isMfaEnabled(),
                settings != null ? settings.getSmsPhone() : null,
                settings != null && settings.isSmsEnabled(),
                settings != null && settings.isEmailMfaEnabled(),
                webauthnRepository.countByUserId(userId),
                recoveryCodeRepository.countUnusedByUserId(userId));
    }

    /** TOTP 绑定阶段一：生成新密钥与 otpauth URI（密钥尚未落库，绑定成功后生效） */
    public MfaSetupResponse mfaSetup(Long userId) {
        User user = requireUser(userId);
        if (user.isMfaEnabled()) {
            throw new BizException(ApiErrorCode.AUTH_ALREADY_BOUND, "已启用两步验证，请先停用再重新绑定");
        }
        String secret = totpService.generateSecret();
        return new MfaSetupResponse(secret, totpService.otpAuthUri(secret, user.getUsername(), TOTP_ISSUER));
    }

    /** TOTP 绑定阶段二：校验动态码后启用 MFA */
    @Transactional
    public void mfaBind(Long userId, MfaBindRequest request) {
        User user = requireUser(userId);
        if (user.isMfaEnabled()) {
            throw new BizException(ApiErrorCode.AUTH_ALREADY_BOUND, "已启用两步验证");
        }
        if (!totpService.verify(request.secret(), request.code())) {
            throw new BizException(ApiErrorCode.AUTH_MFA_CODE_INVALID, "动态码校验失败，请确认时间同步后重试");
        }
        user.enableMfa(request.secret());
        userRepository.save(user);
    }

    /** 停用 TOTP 两步验证 */
    @Transactional
    public void mfaDisable(Long userId) {
        User user = requireUser(userId);
        if (!user.isMfaEnabled()) {
            throw new BizException(ApiErrorCode.BAD_REQUEST, "尚未启用两步验证");
        }
        user.disableMfa();
        userRepository.save(user);
    }

    /** 重新生成恢复码：作废旧未使用码，签发新码（明文仅此一次下发） */
    @Transactional
    public RecoveryCodesResponse generateRecoveryCodes(Long userId) {
        requireUser(userId);
        List<RecoveryCode> oldUnused = recoveryCodeRepository.findUnusedByUserId(userId);
        if (!oldUnused.isEmpty()) {
            recoveryCodeRepository.deleteAll(oldUnused);
        }
        List<String> plainCodes = new ArrayList<>(RECOVERY_CODE_COUNT);
        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            String plain = newRecoveryCode();
            plainCodes.add(plain);
            recoveryCodeRepository.save(RecoveryCode.issue(userId, sha256(normalizeCode(plain))));
        }
        return new RecoveryCodesResponse(plainCodes, plainCodes.size());
    }

    /** WebAuthn 设备列表 */
    @Transactional(readOnly = true)
    public List<WebauthnItem> webauthnList(Long userId) {
        requireUser(userId);
        return webauthnRepository.findByUserId(userId).stream().map(WebauthnItem::from).toList();
    }

    /** 注册 WebAuthn 设备（演示：生成随机凭据元数据） */
    @Transactional
    public WebauthnItem webauthnRegister(Long userId, WebauthnRegisterRequest request) {
        requireUser(userId);
        byte[] raw = new byte[32];
        secureRandom.nextBytes(raw);
        WebauthnCredential credential = WebauthnCredential.register(
                userId,
                Base64.getUrlEncoder().withoutPadding().encodeToString(raw),
                "demo-public-key-" + Base64.getUrlEncoder().withoutPadding().encodeToString(raw).substring(0, 16),
                request.name().trim());
        webauthnRepository.save(credential);
        return WebauthnItem.from(credential);
    }

    /** 删除 WebAuthn 设备（校验归属） */
    @Transactional
    public void webauthnDelete(Long userId, Long credentialId) {
        WebauthnCredential credential = webauthnRepository.findById(credentialId)
                .orElseThrow(() -> new BizException(ApiErrorCode.BAD_REQUEST, "设备不存在"));
        if (!credential.getUserId().equals(userId)) {
            throw new BizException(ApiErrorCode.FORBIDDEN, "无权操作该设备");
        }
        webauthnRepository.delete(credential);
    }

    /** 绑定短信验证手机号（演示验证码固定 123456） */
    @Transactional
    public void bindSms(Long userId, SmsBindRequest request) {
        requireUser(userId);
        verifyDemoCode(request.code());
        UserSecuritySettings settings = requireSettings(userId);
        settings.bindSms(request.phone().trim());
        securitySettingsRepository.save(settings);
    }

    /** 解绑短信验证 */
    @Transactional
    public void unbindSms(Long userId) {
        UserSecuritySettings settings = requireSettings(userId);
        settings.unbindSms();
        securitySettingsRepository.save(settings);
    }

    /** 启用/停用邮件验证码 */
    @Transactional
    public void toggleEmailMfa(Long userId, EmailMfaToggleRequest request) {
        UserSecuritySettings settings = requireSettings(userId);
        settings.toggleEmailMfa(request.enabled());
        securitySettingsRepository.save(settings);
    }

    /** 发送验证码（演示：打印到服务端日志，固定 123456） */
    public void sendCode(SendCodeRequest request) {
        String channel = request.channel().trim().toLowerCase();
        if (!"sms".equals(channel) && !"email".equals(channel)) {
            throw new BizException(ApiErrorCode.BAD_REQUEST, "channel 仅支持 sms / email");
        }
        log.info("[DEMO] 向 {} 发送 {} 验证码：{}（演示模式固定值）",
                request.target(), channel, DEMO_CODE);
    }

    /** 校验演示验证码 */
    public void verifyDemoCode(String code) {
        if (!DEMO_CODE.equals(code)) {
            throw new BizException(ApiErrorCode.AUTH_CODE_INVALID, "验证码错误，请重试（演示模式固定 123456）");
        }
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ApiErrorCode.UNAUTHORIZED, "登录状态已失效"));
    }

    private UserSecuritySettings requireSettings(Long userId) {
        return securitySettingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserSecuritySettings settings = UserSecuritySettings.create(userId);
                    securitySettingsRepository.save(settings);
                    return settings;
                });
    }

    /** 恢复码格式：XXXX-XXXX-XXXX-XXXX（大写十六进制） */
    private String newRecoveryCode() {
        byte[] bytes = new byte[8];
        secureRandom.nextBytes(bytes);
        String hex = HexFormat.of().formatHex(bytes).toUpperCase();
        return hex.substring(0, 4) + "-" + hex.substring(4, 8) + "-"
                + hex.substring(8, 12) + "-" + hex.substring(12);
    }

    /** 规范化恢复码（去分隔符、大写），供哈希比较 */
    public static String normalizeCode(String code) {
        return code.replaceAll("[\\s-]", "").toUpperCase();
    }

    public static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 不可用", ex);
        }
    }
}
