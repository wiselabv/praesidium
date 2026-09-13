package io.github.wiselabv.praesidium.admin.identity.application;

import io.github.wiselabv.praesidium.admin.identity.application.dto.ProfileResponse;
import io.github.wiselabv.praesidium.admin.identity.application.dto.UpdateProfileRequest;
import io.github.wiselabv.praesidium.admin.identity.domain.UserProfileRepository;
import io.github.wiselabv.praesidium.admin.identity.domain.UserRepository;
import io.github.wiselabv.praesidium.admin.identity.domain.model.User;
import io.github.wiselabv.praesidium.admin.identity.domain.model.UserProfile;
import io.github.wiselabv.praesidium.admin.shared.api.ApiErrorCode;
import io.github.wiselabv.praesidium.admin.shared.api.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 个人信息应用服务：查询 / 更新（users 基础字段 + user_profiles 扩展字段）。
 */
@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;

    public ProfileService(UserRepository userRepository, UserProfileRepository profileRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    /** 当前用户完整资料 */
    @Transactional(readOnly = true)
    public ProfileResponse get(Long userId) {
        User user = requireUser(userId);
        UserProfile profile = profileRepository.findByUserId(userId).orElse(null);
        return ProfileResponse.from(user, profile);
    }

    /** 更新资料：displayName/email 写 users，其余写 user_profiles（无则创建） */
    @Transactional
    public ProfileResponse update(Long userId, UpdateProfileRequest request) {
        User user = requireUser(userId);
        user.updateProfile(request.displayName().trim(),
                request.email() == null ? null : request.email().trim());
        userRepository.save(user);

        UserProfile profile = profileRepository.findByUserId(userId).orElse(null);
        if (profile == null) {
            profile = UserProfile.create(userId);
        }
        profile.update(trimToNull(request.avatar()), trimToNull(request.phone()),
                trimToNull(request.department()), trimToNull(request.title()),
                trimToNull(request.bio()));
        profileRepository.save(profile);
        return ProfileResponse.from(user, profile);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ApiErrorCode.UNAUTHORIZED, "登录状态已失效"));
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
