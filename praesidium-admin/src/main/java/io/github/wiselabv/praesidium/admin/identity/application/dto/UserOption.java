package io.github.wiselabv.praesidium.admin.identity.application.dto;

import io.github.wiselabv.praesidium.admin.identity.domain.model.User;

/** 用户下拉选项（授权策略表单）：仅暴露身份标识字段 */
public record UserOption(Long id, String username, String displayName) {

    public static UserOption from(User user) {
        return new UserOption(user.getId(), user.getUsername(), user.getDisplayName());
    }
}
