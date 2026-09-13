package io.github.wiselabv.praesidium.admin.identity.application;

import java.util.List;

import io.github.wiselabv.praesidium.admin.identity.application.dto.UserOption;
import io.github.wiselabv.praesidium.admin.identity.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户应用服务：简单清单查询。
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** 全量用户下拉选项（演示规模，不分页） */
    @Transactional(readOnly = true)
    public List<UserOption> listOptions() {
        return userRepository.findAllSimple().stream().map(UserOption::from).toList();
    }
}
