package io.github.wiselabv.praesidium.admin.identity.application.dto;

import java.util.List;

/** 恢复码生成响应：明文仅此一次下发 */
public record RecoveryCodesResponse(List<String> codes, long remaining) {
}
