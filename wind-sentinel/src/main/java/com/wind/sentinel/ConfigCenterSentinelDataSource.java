package com.wind.sentinel;

import com.alibaba.csp.sentinel.datasource.AbstractDataSource;
import com.wind.configcenter.core.ConfigRepository;
import com.wind.jackson.WindJson;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于配置中心的规则配置数据源，支持从多个配置加载限流规则，并合并
 *
 * @author wuxp
 * @date 2024-03-12 10:04
 **/
@Slf4j
public class ConfigCenterSentinelDataSource<T> extends AbstractDataSource<String, List<T>> {

    private final ConfigRepository configRepository;

    private final List<ConfigRepository.ConfigDescriptor> descriptors;

    private final Map<ConfigRepository.ConfigDescriptor, ConfigRepository.ConfigSubscription> subscriptions = new ConcurrentHashMap<>();

    private final Class<T> configType;

    public ConfigCenterSentinelDataSource(ConfigRepository configRepository, ConfigRepository.ConfigDescriptor descriptor, Class<T> configType) {
        this(configRepository, Collections.singletonList(descriptor), configType);
    }

    public ConfigCenterSentinelDataSource(ConfigRepository configRepository, List<ConfigRepository.ConfigDescriptor> descriptors,
                                          Class<T> configType) {
        super(source -> WindJson.parseArray(source, configType));
        this.configRepository = configRepository;
        this.configType = configType;
        this.descriptors = descriptors;
        for (ConfigRepository.ConfigDescriptor descriptor : descriptors) {
            ConfigRepository.ConfigSubscription subscription = configRepository.onChange(descriptor, (ConfigRepository.TextConfigListener) config -> {
                // 有配置发生变更，重新加载所有的配置
                loadAllConfig();
            });
            subscriptions.put(descriptor, subscription);
        }
        SentinelRuleListenRegister.registerListen(configType, this);
        loadAllConfig();
    }


    @Override
    public String readSource() throws Exception {
        List<T> result = new ArrayList<>();
        descriptors.stream().map(configRepository::getTextConfig)
                .map(text -> WindJson.parseArray(text, this.configType))
                .forEach(result::addAll);
        return WindJson.toJsonString(result);
    }

    @Override
    public void close() throws Exception {
        subscriptions.values().forEach(ConfigRepository.ConfigSubscription::unsubscribe);
    }

    private void loadAllConfig() {
        try {
            updateConfig(loadConfig());
        } catch (Exception exception) {
            log.error("load sentinel config exception", exception);
        }
    }

    private void updateConfig(List<T> newValue) {
        if (newValue == null) {
            log.warn("sentinel config is null, no be update");
            return;
        }
        getProperty().updateValue(newValue);
    }
}
