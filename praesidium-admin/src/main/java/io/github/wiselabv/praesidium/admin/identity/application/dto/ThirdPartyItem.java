package io.github.wiselabv.praesidium.admin.identity.application.dto;

import java.time.Instant;

import io.github.wiselabv.praesidium.admin.identity.domain.model.UserThirdParty;

/** 第三方绑定条目 */
public record ThirdPartyItem(
        String provider,
        String providerUserId,
        String nickname,
        String avatarUrl,
        Instant boundAt) {

    public static ThirdPartyItem from(UserThirdParty binding) {
        return new ThirdPartyItem(binding.getProvider(), binding.getProviderUserId(),
                binding.getNickname(), binding.getAvatarUrl(), binding.getBoundAt());
    }
}
