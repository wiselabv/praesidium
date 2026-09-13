package io.github.wiselabv.praesidium.admin.audit.application;

import java.util.List;

import io.github.wiselabv.praesidium.admin.audit.application.dto.LoginLogItem;
import io.github.wiselabv.praesidium.admin.audit.domain.LoginLogRepository;
import io.github.wiselabv.praesidium.admin.audit.domain.model.LoginLog;
import io.github.wiselabv.praesidium.admin.shared.api.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 登录日志应用服务：分页查询。
 */
@Service
public class LoginLogService {

    private final LoginLogRepository loginLogRepository;

    public LoginLogService(LoginLogRepository loginLogRepository) {
        this.loginLogRepository = loginLogRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<LoginLogItem> list(String keyword, String result, int page, int size) {
        List<LoginLog> logs = loginLogRepository.findPage(keyword, result, (page - 1) * size, size);
        long total = loginLogRepository.countPage(keyword, result);
        List<LoginLogItem> items = logs.stream().map(LoginLogItem::from).toList();
        return PageResponse.of(items, total, page, size);
    }
}
