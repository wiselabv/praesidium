package io.github.wiselabv.praesidium.admin.identity.application.dto;

/** 请求来源信息：客户端 IP 与 User-Agent（登录日志埋点用） */
public record ClientInfo(String ip, String userAgent) {
}
