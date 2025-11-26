package com.wind.common.config;

import com.wind.common.WindConstants;
import org.jspecify.annotations.Nullable;

/**
 * 系统配置
 *
 * @author wuxp
 * @date 2023-11-15 09:36
 **/
public interface SystemConfigStorage {

    /**
     * 保存配置
     *
     * @param name  配置名称
     * @param group 配置分组
     * @param value 配置值
     */
    void saveConfig(String name, String group, String value);

    default void saveConfig(String name, String value) {
        saveConfig(name, WindConstants.DEFAULT_TEXT, value);
    }

    /**
     * 获取配置
     *
     * @param name 配置名称
     * @return 配置值
     */
    @Nullable
    String getConfig(String name);
}
