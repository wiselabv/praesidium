package io.github.wiselabv.praesidium.admin.identity.api;

import io.github.wiselabv.praesidium.admin.identity.application.ProfileService;
import io.github.wiselabv.praesidium.admin.identity.application.dto.ProfileResponse;
import io.github.wiselabv.praesidium.admin.identity.application.dto.UpdateProfileRequest;
import io.github.wiselabv.praesidium.admin.shared.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人信息接口：查看 / 更新当前用户资料。
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ApiResponse<ProfileResponse> get(Authentication authentication) {
        return ApiResponse.ok(profileService.get((Long) authentication.getPrincipal()));
    }

    @PutMapping
    public ApiResponse<ProfileResponse> update(@Valid @RequestBody UpdateProfileRequest request,
                                               Authentication authentication) {
        return ApiResponse.ok(profileService.update((Long) authentication.getPrincipal(), request));
    }
}
