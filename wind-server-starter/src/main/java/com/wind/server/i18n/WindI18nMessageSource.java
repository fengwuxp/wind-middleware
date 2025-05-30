package com.wind.server.i18n;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wind.configcenter.core.ConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.AbstractResourceBasedMessageSource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.lang.Nullable;

import javax.annotation.Nonnull;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 从配置中心加载国际化配置文件
 *
 * @author wuxp
 * @date 2023-10-30 08:43
 **/
@Slf4j
public class WindI18nMessageSource extends AbstractResourceBasedMessageSource {

    /**
     * i18n 配置分组
     */
    private static final String I18N_GROUP = "I18N";

    private final ConfigRepository repository;

    private final WindMessageSourceProperties properties;

    private final Map<Locale, PropertyResolver> localPropertyResolvers;

    private final Cache<String, MessageFormat> messageFormatCache;

    public WindI18nMessageSource(ConfigRepository repository, WindMessageSourceProperties properties) {
        this.repository = repository;
        this.properties = properties;
        this.localPropertyResolvers = loadLocales();
        this.messageFormatCache = Caffeine.newBuilder()
                .maximumSize(2000)
                .initialCapacity(200)
                .expireAfterAccess(properties.getCacheDuration())
                .build();
    }

    private Map<Locale, PropertyResolver> loadLocales() {
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

    /**
     * Resolves the given message code as key in the retrieved bundle files,
     * returning the value found in the bundle as-is (without MessageFormat parsing).
     */
    @Override
    protected String resolveCodeWithoutArguments(@Nonnull String code, @Nonnull Locale locale) {
        PropertyResolver resolver = localPropertyResolvers.get(locale);
        if (resolver == null) {
            return null;
        }
        return resolver.getProperty(code);
    }

    /**
     * Resolves the given message code as key in the retrieved bundle files,
     * using a cached MessageFormat instance per message code.
     */
    @Override
    @Nullable
    protected MessageFormat resolveCode(@Nonnull String code, @Nonnull Locale locale) {
        String key = String.format("%s@%s", code, locale);
        return messageFormatCache.get(key, k -> {
            String message = resolveCodeWithoutArguments(code, locale);
            if (message == null) {
                // 如果 code 中有占位符，直接返回 MessageFormat
                return code.contains("{0}") ? new MessageFormat(code, locale) : null;
            }
            return new MessageFormat(message, locale);
        });
    }


}
