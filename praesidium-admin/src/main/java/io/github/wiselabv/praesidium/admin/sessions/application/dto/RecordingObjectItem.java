package io.github.wiselabv.praesidium.admin.sessions.application.dto;

/** 录像切片对象（MinIO 对象 key + 预签名下载 URL） */
public record RecordingObjectItem(String key, String url) {
}
