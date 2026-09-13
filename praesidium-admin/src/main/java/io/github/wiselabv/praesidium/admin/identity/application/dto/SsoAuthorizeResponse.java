package io.github.wiselabv.praesidium.admin.identity.application.dto;

/** SSO 授权发起响应：前端跳转 authorizeUrl，演示回调固定重定向回前端登录页 */
public record SsoAuthorizeResponse(String provider, String displayName, String authorizeUrl, String state) {
}
