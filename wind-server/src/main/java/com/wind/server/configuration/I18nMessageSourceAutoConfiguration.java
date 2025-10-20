package com.wind.server.configuration;

import com.wind.common.WindConstants;
import com.wind.common.i18n.SpringI18nMessageUtils;
import com.wind.configcenter.core.ConfigRepository;
import com.wind.server.i18n.WindI18nLanguageSupplier;
import com.wind.server.i18n.WindI18nMessageSource;
import com.wind.server.i18n.WindMessageSourceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.wind.common.WindConstants.WIND_I18N_MESSAGE_PREFIX;

/**
 * 国际化配置创建
 *
 * @author wuxp
 * @date 2023-10-30 09:43
 **/
@Configuration
@ConditionalOnProperty(prefix = WIND_I18N_MESSAGE_PREFIX, name = WindConstants.ENABLED_NAME, havingValue = WindConstants.TRUE)
@Slf4j
public class I18nMessageSourceAutoConfiguration {

    /**
     * i18n 配置分组
     */
    private static final String I18N_GROUP = "I18N";

    @Bean
    @ConfigurationProperties(prefix = WIND_I18N_MESSAGE_PREFIX)
    public WindMessageSourceProperties windMessageSourceProperties() {
        return new WindMessageSourceProperties();
    }

    @Bean
    @Primary
    @ConditionalOnBean({WindI18nLanguageSupplier.class, WindMessageSourceProperties.class})
    public WindI18nMessageSource windI18nMessageSource(WindI18nLanguageSupplier supplier, WindMessageSourceProperties properties) {
        WindI18nMessageSource result = new WindI18nMessageSource(supplier, properties);
        if (properties.getEncoding() != null) {
            result.setDefaultEncoding(properties.getEncoding().name());
        }
        result.setFallbackToSystemLocale(properties.isFallbackToSystemLocale());
        Duration cacheDuration = properties.getCacheDuration();
        if (cacheDuration != null) {
            result.setCacheMillis(cacheDuration.toMillis());
        }
        result.setAlwaysUseMessageFormat(properties.isAlwaysUseMessageFormat());
        result.setUseCodeAsDefaultMessage(properties.isUseCodeAsDefaultMessage());
        if (StringUtils.hasText(properties.getI18nMessageKeyPrefix())) {
            SpringI18nMessageUtils.setI18nKeyMatcher(text -> text.startsWith(properties.getI18nMessageKeyPrefix()));
        } else {
            SpringI18nMessageUtils.setI18nKeyMatcher(text -> true);
        }
        return result;
    }

    /**
     * 配置中心国际化语言提供者
     */
    @Bean
    @ConditionalOnMissingBean(WindI18nLanguageSupplier.class)
    @ConditionalOnBean({ConfigRepository.class, WindMessageSourceProperties.class})
    public WindI18nLanguageSupplier configCenterWindI18nLanguageSupplier(ConfigRepository repository, WindMessageSourceProperties properties) {
        return new WindI18nLanguageSupplier() {
            @Override
            public Map<Locale, PropertyResolver> get() {
                final Map<Locale, PropertyResolver> result = new ConcurrentHashMap<>();
                for (Locale locale : properties.getLocales()) {
                    String name = String.format("%s-%s", properties.getName(), locale);
                    ConfigRepository.ConfigDescriptor descriptor = ConfigRepository.ConfigDescriptor.immutable(name, I18N_GROUP, properties.getFileType());
                    result.put(locale, buildPropertyResolver(repository.getConfigs(descriptor)));
                    // 监听配置变化
                    repository.onChange(descriptor, (ConfigRepository.PropertyConfigListener) configs -> {
                        log.info("i18n message refresh dataId = {}", descriptor.getConfigId());
                        result.put(locale, buildPropertyResolver(configs));
                    });
                }
                return result;
            }

            private PropertyResolver buildPropertyResolver(List<PropertySource<?>> configs) {
                MutablePropertySources propertySources = new MutablePropertySources();
                configs.forEach(propertySources::addFirst);
                return new PropertySourcesPropertyResolver(propertySources);
            }
        };
    }
}
