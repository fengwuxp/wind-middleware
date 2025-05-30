package com.wind.server.configuration;

import com.wind.common.WindConstants;
import com.wind.common.i18n.SpringI18nMessageUtils;
import com.wind.configcenter.core.ConfigRepository;
import com.wind.server.i18n.WindI18nMessageSource;
import com.wind.server.i18n.WindMessageSourceProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import java.time.Duration;

import static com.wind.common.WindConstants.WIND_I18N_MESSAGE_PREFIX;

/**
 * @author wuxp
 * @date 2023-10-30 09:43
 **/
@Configuration
@ConditionalOnProperty(prefix = WindConstants.WIND_I18N_MESSAGE_PREFIX, name = WindConstants.ENABLED_NAME)
public class I18nMessageSourceAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = WindConstants.WIND_I18N_MESSAGE_PREFIX)
    public WindMessageSourceProperties windMessageSourceProperties() {
        return new WindMessageSourceProperties();
    }

    @Bean
    @Primary
    @ConditionalOnBean({ConfigRepository.class, WindMessageSourceProperties.class})
    public WindI18nMessageSource windI18nMessageSource(ConfigRepository repository, WindMessageSourceProperties properties) {
        WindI18nMessageSource result = new WindI18nMessageSource(repository, properties);
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
}
