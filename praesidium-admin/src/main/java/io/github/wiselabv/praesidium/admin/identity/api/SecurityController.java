package io.github.wiselabv.praesidium.admin.identity.api;

import java.util.List;

import io.github.wiselabv.praesidium.admin.identity.application.SecurityService;
import io.github.wiselabv.praesidium.admin.identity.application.dto.EmailMfaToggleRequest;
import io.github.wiselabv.praesidium.admin.identity.application.dto.MfaBindRequest;
import io.github.wiselabv.praesidium.admin.identity.application.dto.MfaSetupResponse;
import io.github.wiselabv.praesidium.admin.identity.application.dto.RecoveryCodesResponse;
import io.github.wiselabv.praesidium.admin.identity.application.dto.SecuritySummaryResponse;
import io.github.wiselabv.praesidium.admin.identity.application.dto.SendCodeRequest;
import io.github.wiselabv.praesidium.admin.identity.application.dto.SmsBindRequest;
import io.github.wiselabv.praesidium.admin.identity.application.dto.WebauthnItem;
import io.github.wiselabv.praesidium.admin.identity.application.dto.WebauthnRegisterRequest;
import io.github.wiselabv.praesidium.admin.shared.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 安全设置接口：TOTP 绑定/解绑、恢复码、WebAuthn、短信/邮件验证码。
 */
@RestController
@RequestMapping("/api/profile/security")
public class SecurityController {

    private final SecurityService securityService;

    public SecurityController(SecurityService securityService) {
        this.securityService = securityService;
    }

    /** 安全设置摘要 */
    @GetMapping("/summary")
    public ApiResponse<SecuritySummaryResponse> summary(Authentication authentication) {
        return ApiResponse.ok(securityService.summary((Long) authentication.getPrincipal()));
    }

    /** TOTP 绑定阶段一：生成密钥与 otpauth URI */
    @PostMapping("/mfa/setup")
    public ApiResponse<MfaSetupResponse> mfaSetup(Authentication authentication) {
        return ApiResponse.ok(securityService.mfaSetup((Long) authentication.getPrincipal()));
    }

    /** TOTP 绑定阶段二：校验动态码后启用 */
    @PostMapping("/mfa/bind")
    public ApiResponse<Void> mfaBind(@Valid @RequestBody MfaBindRequest request,
                                     Authentication authentication) {
        securityService.mfaBind((Long) authentication.getPrincipal(), request);
        return ApiResponse.ok();
    }

    /** 停用 TOTP */
    @PostMapping("/mfa/disable")
    public ApiResponse<Void> mfaDisable(Authentication authentication) {
        securityService.mfaDisable((Long) authentication.getPrincipal());
        return ApiResponse.ok();
    }

    /** 重新生成恢复码（明文仅此一次下发） */
    @PostMapping("/recovery-codes")
    public ApiResponse<RecoveryCodesResponse> generateRecoveryCodes(Authentication authentication) {
        return ApiResponse.ok(securityService.generateRecoveryCodes((Long) authentication.getPrincipal()));
    }

    /** WebAuthn 设备列表 */
    @GetMapping("/webauthn")
    public ApiResponse<List<WebauthnItem>> webauthnList(Authentication authentication) {
        return ApiResponse.ok(securityService.webauthnList((Long) authentication.getPrincipal()));
    }

    /** 注册 WebAuthn 设备（演示） */
    @PostMapping("/webauthn")
    public ApiResponse<WebauthnItem> webauthnRegister(@Valid @RequestBody WebauthnRegisterRequest request,
                                                      Authentication authentication) {
        return ApiResponse.ok(securityService.webauthnRegister((Long) authentication.getPrincipal(), request));
    }

    /** 删除 WebAuthn 设备 */
    @DeleteMapping("/webauthn/{id}")
    public ApiResponse<Void> webauthnDelete(@PathVariable Long id, Authentication authentication) {
        securityService.webauthnDelete((Long) authentication.getPrincipal(), id);
        return ApiResponse.ok();
    }

    /** 绑定短信验证手机号 */
    @PostMapping("/sms/bind")
    public ApiResponse<Void> bindSms(@Valid @RequestBody SmsBindRequest request,
                                     Authentication authentication) {
        securityService.bindSms((Long) authentication.getPrincipal(), request);
        return ApiResponse.ok();
    }

    /** 解绑短信验证 */
    @PostMapping("/sms/unbind")
    public ApiResponse<Void> unbindSms(Authentication authentication) {
        securityService.unbindSms((Long) authentication.getPrincipal());
        return ApiResponse.ok();
    }

    /** 启用/停用邮件验证码 */
    @PutMapping("/email-mfa")
    public ApiResponse<Void> toggleEmailMfa(@RequestBody EmailMfaToggleRequest request,
                                            Authentication authentication) {
        securityService.toggleEmailMfa((Long) authentication.getPrincipal(), request);
        return ApiResponse.ok();
    }

    /** 发送验证码（演示：打印到服务端日志） */
    @PostMapping("/code/send")
    public ApiResponse<Void> sendCode(@Valid @RequestBody SendCodeRequest request) {
        securityService.sendCode(request);
        return ApiResponse.ok();
    }
}
