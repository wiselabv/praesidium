package io.github.wiselabv.praesidium.admin.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import io.github.wiselabv.praesidium.admin.identity.infrastructure.JwtAuthenticationFilter;
import io.github.wiselabv.praesidium.admin.shared.api.ApiErrorCode;
import io.github.wiselabv.praesidium.admin.shared.api.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

/**
 * 安全配置：无状态 JWT 过滤器链。
 *
 * <p>约定（见 docs/ARCHITECTURE.md）：
 * <ul>
 *   <li>登录、刷新端点放行，其余一律认证；</li>
 *   <li>「未认证 / 无权限」由安全层以 HTTP 401 / 403 返回（响应体 code 对齐）；</li>
 *   <li>其余业务失败一律 HTTP 200 + 业务码（见 {@link ApiResponse}）。</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** 允许跨域的前端源（逗号分隔，默认 Vite 开发服务器；由 PRAESIDIUM_CORS_ORIGINS 覆盖） */
    @Value("${praesidium.security.cors-origins}")
    private String corsOrigins;

    /** 密码编码器：BCrypt（强度 10，与既有种子哈希兼容） */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter,
                                           ObjectMapper objectMapper) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login/**", "/api/auth/sso/**", "/api/auth/refresh", "/api/auth/logout", "/error").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeSecurityError(response, HttpServletResponse.SC_UNAUTHORIZED,
                                        ApiErrorCode.UNAUTHORIZED, "未认证或登录已过期", objectMapper))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeSecurityError(response, HttpServletResponse.SC_FORBIDDEN,
                                        ApiErrorCode.FORBIDDEN, "无权限执行该操作", objectMapper)));
        return http.build();
    }

    /** 安全层错误响应：HTTP 状态码与业务码对齐的 JSON */
    private void writeSecurityError(HttpServletResponse response, int httpStatus, int code,
                                    String message, ObjectMapper objectMapper) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(code, message));
    }

    /** CORS：放行前端开发源（Vite dev server），生产环境走网关同源后收紧 */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(corsOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
