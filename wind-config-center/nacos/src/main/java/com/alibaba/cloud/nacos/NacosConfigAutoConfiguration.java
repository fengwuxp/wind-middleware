package com.alibaba.cloud.nacos;


import com.alibaba.cloud.nacos.refresh.NacosContextRefresher;
import com.alibaba.cloud.nacos.refresh.NacosRefreshHistory;
import com.alibaba.cloud.nacos.refresh.SmartConfigurationPropertiesRebinder;
import com.alibaba.cloud.nacos.refresh.condition.ConditionalOnNonDefaultBehavior;
import com.alibaba.nacos.api.config.ConfigService;
import com.wind.configcenter.core.ConfigRepository;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.cloud.context.properties.ConfigurationPropertiesBeans;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.alibaba.cloud.nacos.WindNacosBootstrapListener.NACOS_CONFIG_PREFIX;
import static com.wind.common.WindConstants.ENABLED_NAME;
import static com.wind.common.WindConstants.TRUE;

/**
 * @author juven.xuxb
 * @author freeman
 */
@Configuration(proxyBeanMethods = false)
@AllArgsConstructor
@ConditionalOnProperty(prefix = NACOS_CONFIG_PREFIX, name = ENABLED_NAME, havingValue = TRUE, matchIfMissing = true)
public class NacosConfigAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(value = NacosConfigProperties.class, search = SearchStrategy.CURRENT)
    public NacosConfigProperties nacosConfigProperties() {
        return new NacosConfigProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    public ConfigService configService() {
        // 使用同一个实例
        return WindNacosBootstrapListener.CONFIG_SERVICE.get();
    }

    @Bean
    @ConditionalOnMissingBean
    public ConfigRepository nacosConfigRepository() {
        return WindNacosBootstrapListener.CONFIG_REPOSITORY.get();
    }

    @Bean
    public NacosRefreshHistory nacosRefreshHistory() {
        return new NacosRefreshHistory();
    }

    @Bean
    public NacosContextRefresher nacosContextRefresher(NacosConfigManager nacosConfigManager, NacosRefreshHistory nacosRefreshHistory) {
        // Consider that it is not necessary to be compatible with the previous
        // configuration
        // and use the new configuration if necessary.
        return new NacosContextRefresher(nacosConfigManager, nacosRefreshHistory);
    }

    @Bean
    @ConditionalOnMissingBean(search = SearchStrategy.CURRENT)
    @ConditionalOnNonDefaultBehavior
    public ConfigurationPropertiesRebinder smartConfigurationPropertiesRebinder(ConfigurationPropertiesBeans beans) {
        // If using default behavior, not use SmartConfigurationPropertiesRebinder.
        // Minimize te possibility of making mistakes.
        return new SmartConfigurationPropertiesRebinder(beans);
    }

}
