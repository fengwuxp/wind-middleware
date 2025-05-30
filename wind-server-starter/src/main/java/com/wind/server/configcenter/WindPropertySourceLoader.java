package com.wind.server.configcenter;

import com.google.common.collect.ImmutableMap;
import com.wind.common.WindConstants;
import com.wind.common.enums.ConfigFileType;
import com.wind.common.enums.WindMiddlewareType;
import com.wind.common.exception.AssertUtils;
import com.wind.common.jul.WindJulLogFactory;
import com.wind.configcenter.core.ConfigRepository;
import com.wind.configcenter.core.ConfigRepository.ConfigDescriptor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.wind.common.WindConstants.SPRING_APPLICATION_NAME;
import static com.wind.common.WindConstants.SPRING_REDISSON_CONFIG_NAME;
import static com.wind.common.WindConstants.WIND_MIDDLEWARE_SHARE_NAME;
import static com.wind.common.WindConstants.WIND_REDISSON_PROPERTY_SOURCE_NAME;
import static com.wind.common.WindConstants.WIND_SERVER_USED_MIDDLEWARE;

/**
 * 配置中心配置加载器
 * 配置相关参见：https://www.yuque.com/suiyuerufeng-akjad/wind/lb2kacr9ch1l70td
 *
 * @author wuxp
 * @date 2023-10-15 12:30
 **/
@AllArgsConstructor
@Slf4j
public class WindPropertySourceLoader {

    private static final Logger LOGGER = WindJulLogFactory.getLogger(WindPropertySourceLoader.class);

    private final ConfigRepository repository;

    private final WindConfigCenterProperties properties;

    /**
     * 加载全局配置
     *
     * @param environment spring environment
     */
    public void loadGlobalConfigs(ConfigurableEnvironment environment) {
        CompositePropertySource globalProperties = new CompositePropertySource(WindConstants.GLOBAL_CONFIG_NAME);
        ConfigDescriptor descriptor = ConfigDescriptor.immutable(WindConstants.GLOBAL_CONFIG_NAME, WindConstants.GLOBAL_CONFIG_GROUP);
        List<PropertySource<?>> configs = repository.getConfigs(descriptor);
        configs.forEach(globalProperties::addFirstPropertySource);
        environment.getPropertySources().addLast(globalProperties);
    }

    /**
     * 加载应用、中间件配置
     *
     * @param environment spring environment
     */
    public void loadConfigs(ConfigurableEnvironment environment) {
        environment.getPropertySources().addLast(locateConfigs(environment));
    }

    private PropertySource<?> locateConfigs(Environment environment) {
        CompositePropertySource result = new CompositePropertySource(repository.getConfigSourceName());
        String applicationName = environment.getProperty(SPRING_APPLICATION_NAME);
        AssertUtils.hasText(applicationName, () -> String.format("%s must not empty", SPRING_APPLICATION_NAME));
        // 中间件配置共享模式下的名称
        String middlewareShareName = environment.getProperty(WIND_MIDDLEWARE_SHARE_NAME, applicationName);
        // 加载中间件配置
        for (WindMiddlewareType type : getUsedMiddlewareTypes(environment)) {
            String name = environment.getProperty(type.getConfigName(), middlewareShareName);
            AssertUtils.notNull(name, type.getConfigName() + " must not empty");
            if (Objects.equals(type, WindMiddlewareType.REDIS) && WindMiddlewareDetector.useRedisson()) {
                // redisson 配置支持
                loadRedissonConfig(name, result);
            } else if (Objects.equals(type, WindMiddlewareType.DYNAMIC_TP)) {
                // dynamic-tp 使用 yaml
                WindDynamicTpConfigDescriptorDetector.getConfigDescriptors(middlewareShareName).forEach(descriptor -> loadConfigs(descriptor,
                        result));
            } else {
                SimpleConfigDescriptor descriptor = buildDescriptor(name + WindConstants.DASHED + type.name().toLowerCase(), type.name());
                loadConfigs(descriptor, result);
            }
        }
        // 加载应用配置
        loadConfigs(buildDescriptor(applicationName, WindConstants.APP_CONFIG_GROUP), result);
        if (!ObjectUtils.isEmpty(properties.getAppShareConfigs())) {
            // 加载应用间的共享配置
            properties.getAppShareConfigs().forEach(name -> loadConfigs(buildDescriptor(name, WindConstants.APP_SHARE_CONFIG_GROUP), result));
        }
        if (!ObjectUtils.isEmpty(properties.getExtensionConfigs())) {
            // 加载额外的自定义配置
            properties.getExtensionConfigs().forEach(descriptor -> loadConfigs(descriptor, result));
        }
        return result;
    }

    private static List<WindMiddlewareType> getUsedMiddlewareTypes(Environment environment) {
        String config = environment.getProperty(WIND_SERVER_USED_MIDDLEWARE);
        List<WindMiddlewareType> result = new ArrayList<>(WindMiddlewareDetector.getDependenciesMiddlewareTypes());
        if (StringUtils.hasLength(config)) {
            Set<WindMiddlewareType> middlewareTypes =
                    Arrays.stream(config.split(WindConstants.COMMA)).map(WindMiddlewareType::valueOf).collect(Collectors.toSet());
            result.addAll(middlewareTypes);
        }
        return result;
    }

    private SimpleConfigDescriptor buildDescriptor(String name, String group) {
        return buildDescriptor(name, group, properties.getConfigFileType());
    }

    private SimpleConfigDescriptor buildDescriptor(String name, String group, ConfigFileType fileType) {
        SimpleConfigDescriptor result = SimpleConfigDescriptor.of(name, group);
        if (result.getFileType() == null) {
            result.setFileType(fileType);
        }
        // 开启 RefreshScope 支持
        result.setRefreshable(true);
        return result;
    }

    private void loadConfigs(ConfigDescriptor descriptor, CompositePropertySource result) {
        if (log.isDebugEnabled()) {
            log.debug("load config，id = {}, group = {}, refreshable = {}", descriptor.getConfigId(), descriptor.getGroup(),
                    descriptor.isRefreshable());
        }
        repository.getConfigs(descriptor).forEach(result::addFirstPropertySource);
    }

    private void loadRedissonConfig(String redissonName, CompositePropertySource result) {
        if (StringUtils.hasLength(redissonName)) {
            // TODO 增加凭据替换支持
            String name = String.format("%s%s%s", redissonName, WindConstants.DASHED, WindConstants.REDISSON_NAME);
            ConfigDescriptor descriptor = ConfigDescriptor.immutable(name, WindMiddlewareType.REDIS.name(), ConfigFileType.YAML);
            Map<String, Object> source = ImmutableMap.of(SPRING_REDISSON_CONFIG_NAME, repository.getTextConfig(descriptor));
            result.addFirstPropertySource(new MapPropertySource(WIND_REDISSON_PROPERTY_SOURCE_NAME, source));
        }
    }
}
