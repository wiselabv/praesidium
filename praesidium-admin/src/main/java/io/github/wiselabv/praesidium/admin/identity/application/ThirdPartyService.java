package io.github.wiselabv.praesidium.admin.identity.application;

import java.security.SecureRandom;
import java.util.List;
import java.util.Set;

import io.github.wiselabv.praesidium.admin.identity.application.dto.ThirdPartyBindRequest;
import io.github.wiselabv.praesidium.admin.identity.application.dto.ThirdPartyItem;
import io.github.wiselabv.praesidium.admin.identity.domain.UserRepository;
import io.github.wiselabv.praesidium.admin.identity.domain.UserThirdPartyRepository;
import io.github.wiselabv.praesidium.admin.identity.domain.model.User;
import io.github.wiselabv.praesidium.admin.identity.domain.model.UserThirdParty;
import io.github.wiselabv.praesidium.admin.shared.api.ApiErrorCode;
import io.github.wiselabv.praesidium.admin.shared.api.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第三方登录绑定应用服务：查看 / 绑定 / 解绑。
 *
 * <p>演示约定：绑定直接完成（真实场景先跳转提供商授权再回调落库）。
 */
@Service
public class ThirdPartyService {

    /** 支持的第三方提供商 */
    public static final Set<String> PROVIDERS =
            Set.of("github", "oidc", "saml", "cas", "dingtalk", "feishu", "wecom");

    private final UserRepository userRepository;
    private final UserThirdPartyRepository thirdPartyRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public ThirdPartyService(UserRepository userRepository,
                             UserThirdPartyRepository thirdPartyRepository) {
        this.userRepository = userRepository;
        this.thirdPartyRepository = thirdPartyRepository;
    }

    /** 我的绑定列表 */
    @Transactional(readOnly = true)
    public List<ThirdPartyItem> list(Long userId) {
        requireUser(userId);
        return thirdPartyRepository.findByUserId(userId).stream().map(ThirdPartyItem::from).toList();
    }

    /** 绑定第三方账号（演示直绑） */
    @Transactional
    public ThirdPartyItem bind(Long userId, ThirdPartyBindRequest request) {
        User user = requireUser(userId);
        String provider = request.provider().trim().toLowerCase();
        if (!PROVIDERS.contains(provider)) {
            throw new BizException(ApiErrorCode.BAD_REQUEST, "不支持的提供商: " + provider);
        }
        if (thirdPartyRepository.findByUserIdAndProvider(userId, provider).isPresent()) {
            throw new BizException(ApiErrorCode.AUTH_ALREADY_BOUND, "该提供商已绑定，请先解绑");
        }
        UserThirdParty binding = UserThirdParty.bind(
                userId, provider,
                "demo-" + provider + "-" + (1000 + secureRandom.nextInt(9000)),
                user.getUsername() + "@" + provider,
                null);
        thirdPartyRepository.save(binding);
        return ThirdPartyItem.from(binding);
    }

    /** 解绑第三方账号 */
    @Transactional
    public void unbind(Long userId, String provider) {
        UserThirdParty binding = thirdPartyRepository.findByUserIdAndProvider(userId, provider)
                .orElseThrow(() -> new BizException(ApiErrorCode.BAD_REQUEST, "未绑定该提供商"));
        thirdPartyRepository.delete(binding);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ApiErrorCode.UNAUTHORIZED, "登录状态已失效"));
    }
}
