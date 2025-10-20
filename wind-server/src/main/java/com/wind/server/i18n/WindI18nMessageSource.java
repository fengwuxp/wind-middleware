package com.wind.server.i18n;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wind.common.WindConstants;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.support.AbstractResourceBasedMessageSource;
import org.springframework.core.env.PropertyResolver;
import org.springframework.lang.Nullable;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;

/**
 * 从配置中心加载国际化配置文件
 *
 * @author wuxp
 * @date 2023-10-30 08:43
 **/
@Slf4j
public class WindI18nMessageSource extends AbstractResourceBasedMessageSource {

    private final Map<Locale, PropertyResolver> localPropertyResolvers;

    private final Cache<@NotNull String, MessageFormat> messageFormatCache;

    public WindI18nMessageSource(WindI18nMessageSupplier supplier, WindMessageSourceProperties properties) {
        this.localPropertyResolvers = supplier.get();
        this.messageFormatCache = Caffeine.newBuilder()
                .maximumSize(3000)
                .initialCapacity(500)
                .expireAfterAccess(properties.getCacheDuration())
                .build();
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
                String text = convertSlf4jPlaceholders(code);
                // 如果 code 中有占位符，直接返回 MessageFormat
                return text.contains("{0}") ? new MessageFormat(text, locale) : null;
            }
            // 转换 Slf4j {} 格式为 MessageFormat {0}、{1}...
            String convertedMessage = convertSlf4jPlaceholders(message);
            return new MessageFormat(convertedMessage, locale);
        });
    }

    /**
     * 将 Slf4j {} 占位符 转为 MessageFormat 占位符 {0}, {1}, ...
     */
    private String convertSlf4jPlaceholders(String text) {
        if (!text.contains("{}")) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        int index = 0;
        int argIndex = 0;
        while (index < text.length()) {
            int bracePos = text.indexOf("{}", index);
            if (bracePos == -1) {
                sb.append(text.substring(index));
                break;
            }
            sb.append(text, index, bracePos).append(WindConstants.DELIM_START).append(argIndex++).append(WindConstants.DELIM_END);
            index = bracePos + 2;
        }
        return sb.toString();
    }

}
