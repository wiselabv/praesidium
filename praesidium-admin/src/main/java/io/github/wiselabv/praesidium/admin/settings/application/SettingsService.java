package io.github.wiselabv.praesidium.admin.settings.application;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import io.github.wiselabv.praesidium.admin.settings.domain.SettingRepository;
import io.github.wiselabv.praesidium.admin.settings.domain.model.Setting;
import io.github.wiselabv.praesidium.admin.shared.api.ApiErrorCode;
import io.github.wiselabv.praesidium.admin.shared.api.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 系统设置应用服务：全量读取 / 按分组读取与保存。
 */
@Service
public class SettingsService {

    /**
     * 允许的分组白名单，与种子数据保持一致
     */
    public static final Set<String> SECTIONS = Set.of("basic", "security", "notify", "auth", "storage");

    private final SettingRepository settingRepository;
    private final ObjectMapper objectMapper;

    public SettingsService(SettingRepository settingRepository, ObjectMapper objectMapper) {
        this.settingRepository = settingRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 全部分组配置：section -> JSON 对象
     *
     * @return
     */
    @Transactional(readOnly = true)
    public Map<String, JsonNode> all() {
        Map<String, JsonNode> result = new LinkedHashMap<>();
        for (Setting setting : settingRepository.findAll()) {
            result.put(setting.getSection(), parse(setting));
        }
        return result;
    }

    /**
     * 单分组配置
     *
     * @param section
     * @return
     */
    @Transactional(readOnly = true)
    public JsonNode get(String section) {
        return parse(requireSection(section));
    }

    /**
     * 保存单分组配置（整体替换）
     *
     * @param section
     * @param config
     * @return
     */
    @Transactional
    public JsonNode update(String section, JsonNode config) {
        if (config == null || !config.isObject()) {
            throw new BizException(ApiErrorCode.BAD_REQUEST, "配置内容必须为 JSON 对象");
        }
        Setting setting = requireSection(section);
        setting.replaceConfig(config.toString());
        settingRepository.save(setting);
        return config;
    }

    private Setting requireSection(String section) {
        if (section == null || !SECTIONS.contains(section)) {
            throw new BizException(ApiErrorCode.SETTINGS_SECTION_NOT_FOUND, "配置分组不存在: " + section);
        }
        return settingRepository.findBySection(section)
                .orElseThrow(() -> new BizException(ApiErrorCode.SETTINGS_SECTION_NOT_FOUND,
                        "配置分组不存在: " + section));
    }

    private JsonNode parse(Setting setting) {
        try {
            return objectMapper.readTree(setting.getConfig());
        } catch (Exception e) {
            throw new BizException(ApiErrorCode.INTERNAL_ERROR, "配置内容解析失败: " + setting.getSection());
        }
    }
}
