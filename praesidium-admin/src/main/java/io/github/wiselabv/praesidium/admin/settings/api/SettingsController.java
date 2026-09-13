package io.github.wiselabv.praesidium.admin.settings.api;

import java.util.Map;

import io.github.wiselabv.praesidium.admin.settings.application.SettingsService;
import io.github.wiselabv.praesidium.admin.shared.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/**
 * 系统设置接口：全量读取 / 按分组读取与保存。
 */
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public ApiResponse<Map<String, JsonNode>> all() {
        return ApiResponse.ok(settingsService.all());
    }

    @GetMapping("/{section}")
    public ApiResponse<JsonNode> get(@PathVariable String section) {
        return ApiResponse.ok(settingsService.get(section));
    }

    @PutMapping("/{section}")
    public ApiResponse<JsonNode> update(@PathVariable String section, @RequestBody JsonNode config) {
        return ApiResponse.ok(settingsService.update(section, config));
    }
}
