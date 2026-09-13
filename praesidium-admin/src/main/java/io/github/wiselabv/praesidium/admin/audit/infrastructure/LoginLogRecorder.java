package io.github.wiselabv.praesidium.admin.audit.infrastructure;

import io.github.wiselabv.praesidium.admin.audit.domain.LoginLogRepository;
import io.github.wiselabv.praesidium.admin.audit.domain.model.LoginLog;
import io.github.wiselabv.praesidium.admin.identity.domain.event.UserLoggedIn;
import io.github.wiselabv.praesidium.admin.identity.domain.event.UserLoginFailed;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 登录埋点监听器：订阅认证域的登录事件，落登录日志。
 *
 * <p>成功（{@link UserLoggedIn}）与失败（{@link UserLoginFailed}）都留痕，
 * 支撑登录审计与暴力破解追溯。
 */
@Component
public class LoginLogRecorder {

    private final LoginLogRepository loginLogRepository;

    public LoginLogRecorder(LoginLogRepository loginLogRepository) {
        this.loginLogRepository = loginLogRepository;
    }

    @EventListener
    @Transactional
    public void onUserLoggedIn(UserLoggedIn event) {
        loginLogRepository.save(LoginLog.record(event.getUserId(), event.getUsername(),
                event.getSourceIp(), null, event.getMethod(), "success", null, truncate(event.getClient())));
    }

    @EventListener
    @Transactional
    public void onUserLoginFailed(UserLoginFailed event) {
        loginLogRepository.save(LoginLog.record(event.getUserId(), event.getUsername(),
                event.getSourceIp(), null, event.getMethod(), "failed", event.getReason(), truncate(event.getClient())));
    }

    /** 客户端标识截断到 64 字符（User-Agent 可能超长） */
    private String truncate(String client) {
        if (client == null || client.length() <= 64) {
            return client;
        }
        return client.substring(0, 64);
    }
}
