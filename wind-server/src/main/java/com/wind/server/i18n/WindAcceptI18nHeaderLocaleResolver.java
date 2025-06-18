package com.wind.server.i18n;

import com.wind.common.exception.AssertUtils;
import org.apache.tomcat.util.http.parser.AcceptLanguage;
import org.jetbrains.annotations.NotNull;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

/**
 * @author wuxp
 * @date 2024-07-16 11:20
 * @see org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver
 **/
public class WindAcceptI18nHeaderLocaleResolver implements LocaleResolver {

    private final Locale defaultLocale;

    private final List<String> headerNames;

    public WindAcceptI18nHeaderLocaleResolver(List<String> headerNames, Locale defaultLocale) {
        AssertUtils.notNull(defaultLocale, "argument defaultLocale must not null");
        AssertUtils.notEmpty(headerNames, "argument headerNames must not empty");
        this.headerNames = headerNames;
        this.defaultLocale = defaultLocale;
    }

    public WindAcceptI18nHeaderLocaleResolver(List<String> headerNames) {
        this(headerNames, Locale.SIMPLIFIED_CHINESE);
    }

    public WindAcceptI18nHeaderLocaleResolver() {
        this(Collections.singletonList("Accept-Language"), Locale.SIMPLIFIED_CHINESE);
    }

    @Override
    public Locale resolveLocale(@NotNull HttpServletRequest request) {
        String requestLocal = getLanguageHeader(request);
        return parseLocale(getLanguageHeader(request));
    }

    @Nullable
    private Locale findSupportedLocale(HttpServletRequest request, List<Locale> supportedLocales) {
        Enumeration<Locale> requestLocales = request.getLocales();
        Locale languageMatch = null;
        while (requestLocales.hasMoreElements()) {
            Locale locale = requestLocales.nextElement();
            if (supportedLocales.contains(locale)) {
                if (languageMatch == null || languageMatch.getLanguage().equals(locale.getLanguage())) {
                    // Full match: language + country, possibly narrowed from earlier language-only match
                    return locale;
                }
            } else if (languageMatch == null) {
                // Let's try to find a language-only match as a fallback
                for (Locale candidate : supportedLocales) {
                    if (!StringUtils.hasLength(candidate.getCountry()) &&
                            candidate.getLanguage().equals(locale.getLanguage())) {
                        languageMatch = candidate;
                        break;
                    }
                }
            }
        }
        return languageMatch;
    }

    @Override
    public void setLocale(@NotNull HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Locale locale) {
        throw new UnsupportedOperationException(
                "Cannot change HTTP Accept-Language header - use a different locale resolution strategy");
    }

    private String getLanguageHeader(HttpServletRequest request) {
        return headerNames.stream()
                .map(request::getHeader)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    @Nullable
    private Locale parseLocale(String value) {
        if (StringUtils.hasText(value)) {
            try {
                List<AcceptLanguage> acceptLanguages = AcceptLanguage.parse(new StringReader(value));
                return CollectionUtils.isEmpty(acceptLanguages) ? defaultLocale : acceptLanguages.get(0).getLocale();
            } catch (IOException e) {
                // Mal-formed headers are ignore. Do the same in the unlikely event
                // of an IOException.
            }
        }
        return defaultLocale;
    }

}
