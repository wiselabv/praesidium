package io.github.wiselabv.praesidium.admin.identity.infrastructure;

import io.github.wiselabv.praesidium.admin.identity.application.TotpService;
import io.github.wiselabv.praesidium.admin.identity.domain.UserRepository;
import io.github.wiselabv.praesidium.admin.identity.domain.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 首版演示种子：空库启动时播种演示账号，便于本地开发与冒烟联调。
 *
 * <ul>
 *   <li>{@code admin / Admin@123}：未启用 MFA，登录一步完成（next=done）；</li>
 *   <li>{@code demo / Demo@123}：启用 MFA，验证两阶段登录（next=mfa → TOTP）；</li>
 * </ul>
 *
 * <p>幂等：仅当 users 表为空时执行；TOTP 密钥打印到启动日志（仅开发环境种子）。
 * 生产部署时删除本类，用户由运维经导入流程创建。
 */
@Component
public class SeedUsersRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedUsersRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totpService;

    public SeedUsersRunner(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           TotpService totpService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.totpService = totpService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByUsername("admin")) {
            return; // 已有数据，跳过
        }
        userRepository.save(User.create("admin", passwordEncoder.encode("Admin@123"),
                "系统管理员", "admin@praesidium.local"));

        String demoSecret = totpService.generateSecret();
        User demo = User.create("demo", passwordEncoder.encode("Demo@123"),
                "演示用户", "demo@praesidium.local");
        demo.enableMfa(demoSecret);
        userRepository.save(demo);
        log.info("演示种子完成：admin/Admin@123（无 MFA）、demo/Demo@123（MFA 密钥 {}，"
                + "otpauth: {}）", demoSecret,
                totpService.otpAuthUri(demoSecret, "demo", "Praesidium"));
    }
}
