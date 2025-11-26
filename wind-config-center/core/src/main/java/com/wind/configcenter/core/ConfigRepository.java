package com.wind.configcenter.core;

import com.wind.common.WindConstants;
import com.wind.common.enums.ConfigFileType;
import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 配置存储服务
 *
 * @author wuxp
 * @date 2023-10-15 11:13
 **/
public interface ConfigRepository {

    PropertySourceLoader PROPERTY_SOURCE_LOADER = new PropertiesPropertySourceLoader();

    /**
     * 保存配置
     *
     * @param descriptor 配置描述
     * @param content    配置内容
     */
    void saveTextConfig(ConfigDescriptor descriptor, String content);

    /**
     * @return 配置来源名称
     */
    String getConfigSourceName();

    /**
     * 获取文本格式的配置
     *
     * @param descriptor 配置描述
     * @return 配置内容
     */
    String getTextConfig(ConfigDescriptor descriptor);

    /**
     * 获取配置，默认使用 {@link PropertiesPropertySourceLoader} 解析配置
     *
     * @param descriptor 配置描述
     * @return 配置内容
     */
    default List<PropertySource<?>> getConfigs(ConfigDescriptor descriptor) {
        try {
            String config = getTextConfig(descriptor);
            return PROPERTY_SOURCE_LOADER.load(descriptor.getName(), new ByteArrayResource(config.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new BaseException(DefaultExceptionCode.COMMON_ERROR, String.format("load config name = %s failure", descriptor.getName()),
                    exception);
        }
    }

    /**
     * 配置监听
     *
     * @param descriptor 配置描述符
     * @param listener   监听配置
     * @return 配置订阅实例
     */
    default ConfigSubscription onChange(ConfigDescriptor descriptor, TextConfigListener listener) {
        throw new UnsupportedOperationException("un support listen config");
    }

    default ConfigSubscription onChange(ConfigDescriptor descriptor, PropertyConfigListener listener) {
        throw new UnsupportedOperationException("un support listen config");
    }

    interface TextConfigListener {

        void change(String text);
    }

    interface PropertyConfigListener {

        void change(List<PropertySource<?>> configs);
    }

    /**
     * 配置订阅
     */
    interface ConfigSubscription {

        ConfigDescriptor getConfigDescriptor();

        /**
         * 取消订阅
         */
        void unsubscribe();

        static ConfigSubscription empty(ConfigDescriptor descriptor) {
            return new ConfigSubscription() {
                @Override
                public ConfigDescriptor getConfigDescriptor() {
                    return descriptor;
                }

                @Override
                public void unsubscribe() {

                }
            };
        }
    }


    interface ConfigDescriptor {

        /**
         * @return 配置名称，不包含文件扩展名称
         */
        String getName();

        /**
         * 配置分组，可能是目录、标签等
         *
         * @return 配置分组
         */
        String getGroup();

        default ConfigFileType getFileType() {
            if (getName().contains(WindConstants.DOT)) {
                return ConfigFileType.parse(getName());
            }
            return ConfigFileType.PROPERTIES;
        }

        /**
         * 配置在{@link #getGroup()}的唯一标识，用于加载配置
         *
         * @return 配置标识
         */
        default String getConfigId() {
            if (StringUtils.hasLength(getLabel())) {
                return String.format("%s-%s.%s", getName(), getLabel(), getFileType().getFileExtension());
            }
            return String.format("%s.%s", getName(), getFileType().getFileExtension());
        }

        /**
         * @return 配置标签，多个用逗号分隔
         */
        @Nullable
        default String getLabel() {
            return null;
        }

        /**
         * 是否可刷新
         *
         * @return if true 允许刷新
         */
        default boolean isRefreshable() {
            return true;
        }

        /**
         * 创建一个不可变的 ConfigDescriptor
         *
         * @param name  配置名称
         * @param group 配置分组
         * @return ConfigDescriptor 实例
         */
        static ConfigDescriptor immutable(String name, String group) {
            return immutable(name, group, ConfigFileType.PROPERTIES);
        }

        static ConfigDescriptor immutable(String name, String group, ConfigFileType fileType) {
            return new ConfigDescriptor() {
                @Override
                public String getName() {
                    return name;
                }

                @Override
                public String getGroup() {
                    return group;
                }

                @Override
                public ConfigFileType getFileType() {
                    return fileType;
                }
            };
        }
    }
}
