package io.github.wiselabv.praesidium.admin.asset.application.dto;

/** 凭据明文查看（单次下发，前端展示后立即丢弃） */
public record SecretRevealResponse(String secret) {
}
