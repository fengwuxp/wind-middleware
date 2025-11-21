package com.wind.server.i18n;

import com.wind.common.i18n.SpringI18nMessageUtils;
import org.apache.tomcat.util.http.parser.AcceptLanguage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author wuxp
 * @date 2025-11-20 20:49
 **/
class WindI18nMessageSourceTests {

    private WindI18nMessageSource source;

    private final Locale enUs = Locale.forLanguageTag("en-US");

    @BeforeEach
    void setup() {
        WindMessageSourceProperties properties = new WindMessageSourceProperties();
        source = new WindI18nMessageSource(() -> {
            MutablePropertySources propertySources = new MutablePropertySources();
            propertySources.addFirst(new MapPropertySource("en-messages.properties", Map.of(
                    "测试", "test",
                    "我是{0}，这个是{1}哈哈哈", "We {0}, This is {1} hhh"
            )));
            return Map.of(
                    enUs,
                    new PropertySourcesPropertyResolver(propertySources)
            );
        }, properties);
        // en-US,en
        SpringI18nMessageUtils.setLocaleSupplier(() -> {
            WindAcceptI18nHeaderLocaleResolver resolver = new WindAcceptI18nHeaderLocaleResolver();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Accept-Language", "en-US,en");
            return resolver.resolveLocale(request);
        });
        SpringI18nMessageUtils.setI18nKeyMatcher(text -> true);
        SpringI18nMessageUtils.setMessageSource(source);
    }

    @Test
    void testI18n() {
        String message = source.getMessage("我是{}，这个是{}哈哈哈", new Object[]{"wuxp", "spring"}, Locale.forLanguageTag("en-US"));
        Assertions.assertEquals("We wuxp, This is spring hhh", message);
        String msg = SpringI18nMessageUtils.getMessage("我是{}，这个是{}哈哈哈", new Object[]{"wuxp", "spring"});
        Assertions.assertEquals(message, msg);
    }
}
