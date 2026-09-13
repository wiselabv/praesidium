package io.github.wiselabv.praesidium.admin.identity.infrastructure;

import java.io.IOException;
import java.util.List;

import io.github.wiselabv.praesidium.admin.identity.application.JwtTokenService;
import io.github.wiselabv.praesidium.admin.shared.api.BizException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT 认证过滤器：从 {@code Authorization: Bearer <token>} 解析访问令牌，
 * 成功则建立认证上下文（principal 为 userId），供 Controller 直接取用。
 *
 * <p>无令牌时放行（匿名），由 {@code SecurityConfig} 的入口点对受保护路径返回 401；
 * 令牌无效时同样视为匿名继续链路（如携带失效令牌调 permitAll 的 refresh 接口），
 * 是否放行由授权规则决定，避免过滤器过早拦截。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }
        try {
            Claims claims = jwtTokenService.parseAccessToken(header.substring(BEARER_PREFIX.length()));
            Long userId = Long.valueOf(claims.getSubject());
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        } catch (BizException ex) {
            // 无效令牌视为匿名：清上下文后继续链路，由授权规则决定放行（permitAll）或 401（受保护路径）
            SecurityContextHolder.clearContext();
            chain.doFilter(request, response);
        }
    }
}
