package com.wind.server.i18n;

import org.springframework.core.env.PropertyResolver;

import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * i18n 语言提供者
 *
 * @author wuxp
 * @date 2025-10-20 10:45
 **/
public interface WindI18nLanguageSupplier extends Supplier<Map<Locale, PropertyResolver>> {
}
