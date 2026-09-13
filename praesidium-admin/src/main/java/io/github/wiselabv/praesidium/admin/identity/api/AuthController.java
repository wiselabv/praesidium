package io.github.wiselabv.praesidium.admin.identity.api;

import io.github.wiselabv.praesidium.admin.identity.application.AuthService;
import io.github.wiselabv.praesidium.admin.identity.application.SsoProviderService;
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
import io.github.wiselabv.praesidium.admin.identity.application.dto.SsoAuthorizeResponse;
import io.github.wiselabv.praesidium.admin.identity.application.dto.TokenResponse;
import io.github.wiselabv.praesidium.admin.shared.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：登录（密码 → MFA 两阶段）、验证码/域账号/恢复码/SSO 登录、刷新、登出、当前用户。
 *
 * <p>统一响应体 {@link ApiResponse}；登录日志的 IP 取请求真实地址
 * （首版直接取 remoteAddr，网关接入后改为 X-Forwarded-For）。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SsoProviderService ssoProviderService;

    public AuthController(AuthService authService, SsoProviderService ssoProviderService) {
        this.authService = authService;
        this.ssoProviderService = ssoProviderService;
    }

    /** 阶段一：密码登录（成功返回 next=mfa 或 next=done） */
    @PostMapping("/login/password")
    public ApiResponse<LoginPasswordResponse> loginPassword(
            @Valid @RequestBody LoginPasswordRequest request, HttpServletRequest http) {
        return ApiResponse.ok(authService.loginWithPassword(request, clientInfo(http)));
    }

    /** 阶段二：MFA 验证（校验 TOTP 动态码，通过后签发正式令牌对） */
    @PostMapping("/login/mfa")
    public ApiResponse<TokenResponse> loginMfa(
            @Valid @RequestBody LoginMfaRequest request, HttpServletRequest http) {
        return ApiResponse.ok(authService.loginWithMfa(request, clientInfo(http)));
    }

    /** 验证码登录（短信/邮件验证码，演示固定 123456） */
    @PostMapping("/login/code")
    public ApiResponse<TokenResponse> loginCode(
            @Valid @RequestBody LoginCodeRequest request, HttpServletRequest http) {
        return ApiResponse.ok(authService.loginWithCode(request, clientInfo(http)));
    }

    /** 域账号（LDAP/AD）登录：PRAESIDIUM\admin 或 admin@domain */
    @PostMapping("/login/domain")
    public ApiResponse<TokenResponse> loginDomain(
            @Valid @RequestBody LoginDomainRequest request, HttpServletRequest http) {
        return ApiResponse.ok(authService.loginWithDomain(request, clientInfo(http)));
    }

    /** 恢复码登录（MFA 设备不可用时的一次性登录） */
    @PostMapping("/login/recover")
    public ApiResponse<TokenResponse> loginRecover(
            @Valid @RequestBody RecoverLoginRequest request, HttpServletRequest http) {
        return ApiResponse.ok(authService.recoverLogin(request, clientInfo(http)));
    }

    /** SSO 授权发起：返回演示授权 URL 供前端跳转 */
    @GetMapping("/sso/{provider}/authorize")
    public ApiResponse<SsoAuthorizeResponse> ssoAuthorize(@PathVariable String provider) {
        return ApiResponse.ok(ssoProviderService.authorize(provider));
    }

    /** SSO 回调（演示）：按第三方绑定定位用户并签发令牌对 */
    @GetMapping("/sso/{provider}/callback")
    public ApiResponse<TokenResponse> ssoCallback(@PathVariable String provider,
                                                  @RequestParam(required = false) String code,
                                                  @RequestParam(required = false) String state,
                                                  HttpServletRequest http) {
        return ApiResponse.ok(authService.ssoCallback(provider, clientInfo(http)));
    }

    /** 刷新令牌对（旧刷新令牌轮换吊销） */
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    /** 登出：吊销刷新令牌（幂等） */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ApiResponse.ok();
    }

    /** 当前用户信息（由 JWT 过滤器建立的认证上下文提供 userId） */
    @GetMapping("/me")
    public ApiResponse<MeResponse> me(Authentication authentication) {
        return ApiResponse.ok(authService.me((Long) authentication.getPrincipal()));
    }

    /** 客户端请求来源：IP + User-Agent（登录日志埋点） */
    private ClientInfo clientInfo(HttpServletRequest request) {
        return new ClientInfo(clientIp(request), request.getHeader("User-Agent"));
    }

    /** 客户端真实地址：网关接入后优先取 X-Forwarded-For 首项 */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
