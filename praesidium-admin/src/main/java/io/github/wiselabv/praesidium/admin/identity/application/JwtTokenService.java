package io.github.wiselabv.praesidium.admin.identity.application;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import io.github.wiselabv.praesidium.admin.identity.domain.model.User;
import io.github.wiselabv.praesidium.admin.shared.api.ApiErrorCode;
import io.github.wiselabv.praesidium.admin.shared.api.BizException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

/**
 * JWT 令牌服务：访问令牌与登录会话令牌的签发、解析。
 *
 * <p>两类令牌均 HS256 签名（共享密钥，Rust 网关持同一密钥验签）：
 * <ul>
 *   <li>访问令牌（accessToken）：登录完成后随刷新令牌下发，claim 携带 {@code username}；</li>
 *   <li>会话令牌（sessionToken）：密码验证通过但尚未通过 MFA 时下发，
 *       claim 固定 {@code step=mfa}，短效（默认 5 分钟），仅用于换取正式令牌对。</li>
 * </ul>
 */
@Service
public class JwtTokenService {

    private static final String CLAIM_STEP = "step";
    private static final String CLAIM_USERNAME = "username";
    private static final String STEP_MFA = "mfa";

    private final SecretKey key;
    private final long accessTtlMillis;
    private final long sessionTtlMillis;

    public JwtTokenService(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.accessTtlMillis = properties.getAccessTtlMinutes() * 60_000;
        this.sessionTtlMillis = properties.getSessionTtlMinutes() * 60_000;
    }

    /** 签发访问令牌 */
    public String issueAccessToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(CLAIM_USERNAME, user.getUsername())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTtlMillis))
                .signWith(key, Jwts.SIG.HS256) // 显式 HS256：与 Rust 网关验签约定一致
                .compact();
    }

    /** 签发登录会话令牌（密码已通过、待 MFA 阶段） */
    public String issueSessionToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_STEP, STEP_MFA)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + sessionTtlMillis))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** 访问令牌有效期（秒），随令牌对下发供前端计算续期时机 */
    public long accessTtlSeconds() {
        return accessTtlMillis / 1000;
    }

    /** 解析会话令牌并校验阶段：失败统一抛「登录会话已过期」（不泄露具体原因） */
    public Long parseSessionToken(String token) {
        Claims claims = parse(token);
        if (!STEP_MFA.equals(claims.get(CLAIM_STEP))) {
            throw new BizException(ApiErrorCode.AUTH_SESSION_EXPIRED, "登录会话无效，请重新发起登录");
        }
        return Long.valueOf(claims.getSubject());
    }

    /** 解析访问令牌：供安全过滤器建立认证上下文，失败抛 401 */
    public Claims parseAccessToken(String token) {
        Claims claims = parse(token);
        if (claims.get(CLAIM_USERNAME) == null) {
            throw new BizException(ApiErrorCode.UNAUTHORIZED, "令牌无效");
        }
        return claims;
    }

    private Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BizException(ApiErrorCode.UNAUTHORIZED, "令牌无效或已过期");
        }
    }
}
