package com.wind.server.i18n;

import com.wind.common.i18n.SpringI18nMessageUtils;
import com.wind.server.web.restful.FriendlyExceptionMessageConverter;
import com.wind.server.web.restful.RestfulApiRespFactory;
import com.wind.web.util.HttpServletRequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.web.context.ConfigurableWebServerApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSource;
import org.springframework.web.servlet.LocaleResolver;

import jakarta.annotation.Nonnull;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/**
 * spring i18n 国际化支持 初始化器
 * {@link  org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration.EnableWebMvcConfiguration#localeResolver()}
 *
 * @author wuxp
 * @date 2023-10-10 18:38
 * @see org.springframework.boot.autoconfigure.web.WebProperties.LocaleResolver#ACCEPT_HEADER
 * @see org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration
 * @see org.springframework.web.servlet.LocaleResolver
 * @see com.wind.server.configuration.I18nMessageSourceAutoConfiguration
 * @see SpringI18nMessageUtils
 **/
@Slf4j
public class SpringI18nMessageSourceInitializer implements ApplicationListener<ApplicationStartedEvent> {

    /**
     * {@link Locale} 解析器
     */
    private static final AtomicReference<LocaleResolver> LOCALE_RESOLVER = new AtomicReference<>();

    @Override
    public void onApplicationEvent(@Nonnull ApplicationStartedEvent event) {
        if (event.getApplicationContext() instanceof ConfigurableWebServerApplicationContext) {
            // 仅在 Web 上下文中执行
            try {
                LOCALE_RESOLVER.set(new WindAcceptI18nHeaderLocaleResolver(Arrays.asList("Wind-Language", "Accept-Language")));
                SpringI18nMessageUtils.setMessageSource(event.getApplicationContext().getBean(MessageSource.class));
                SpringI18nMessageUtils.setLocaleSupplier(SpringI18nMessageSourceInitializer::getWebRequestLocal);
                RestfulApiRespFactory.configureFriendlyExceptionMessageConverter(FriendlyExceptionMessageConverter.i18n());
                log.info("enabled i18n supported");
            } catch (Exception ignore) {
                log.info("un enabled i18n supported");
            }
        }
    }

    /**
     * @return 获取当前请求的 Locale
     */
    private static Locale getWebRequestLocal() {
        HttpServletRequest request = HttpServletRequestUtils.getContextRequestOfNullable();
        if (request == null) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        return LOCALE_RESOLVER.get().resolveLocale(request);
    }
}
