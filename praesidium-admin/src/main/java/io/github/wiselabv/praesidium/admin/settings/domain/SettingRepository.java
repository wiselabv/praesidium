package io.github.wiselabv.praesidium.admin.settings.domain;

import java.util.List;
import java.util.Optional;

import io.github.wiselabv.praesidium.admin.settings.domain.model.Setting;

/**
 * 系统设置仓储（领域层接口）。
 */
public interface SettingRepository {

    /** 按分组名精确查询 */
    Optional<Setting> findBySection(String section);

    /** 全部分组（按分组名排序） */
    List<Setting> findAll();

    Setting save(Setting setting);
}
