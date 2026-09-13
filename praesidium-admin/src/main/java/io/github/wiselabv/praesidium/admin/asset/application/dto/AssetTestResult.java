package io.github.wiselabv.praesidium.admin.asset.application.dto;

/** 资产连通性测试结果 */
public record AssetTestResult(boolean reachable, long latencyMs, String message) {
}
