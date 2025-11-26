package com.wind.server.web.restful;

import com.wind.common.exception.BaseException;
import com.wind.common.i18n.SpringI18nMessageUtils;
import com.wind.server.web.supports.ApiResp;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.util.StringUtils;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * @author wuxp
 * @date 2024-09-23 10:07
 **/
class RestfulApiRespFactoryTests {

    private static final String TEST_MESSAGE = "测试";

    @BeforeEach
    void setup() {
        SpringI18nMessageUtils.setI18nKeyMatcher(StringUtils::hasText);
        SpringI18nMessageUtils.setMessageSource(new AbstractMessageSource() {
            @Override
            protected MessageFormat resolveCode(@NonNull String code, @NonNull Locale locale) {
                return new MessageFormat(Objects.equals(locale, Locale.CHINA) ? TEST_MESSAGE : "test");
            }
        });
        RestfulApiRespFactory.configureFriendlyExceptionMessageConverter(FriendlyExceptionMessageConverter.i18n());
    }

    @Test
    void testWithThrowable() {
        ApiResp<Object> resp = RestfulApiRespFactory.withThrowable(BaseException.common(TEST_MESSAGE));
        Assertions.assertEquals("测试", resp.getErrorMessage());
        SpringI18nMessageUtils.setLocaleSupplier(() -> Locale.US);
        resp = RestfulApiRespFactory.withThrowable(BaseException.common(TEST_MESSAGE));
        Assertions.assertEquals("test", resp.getErrorMessage());
    }
}
