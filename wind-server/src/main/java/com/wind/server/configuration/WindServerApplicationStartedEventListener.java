package com.wind.server.configuration;

import com.wind.common.i18n.SpringI18nMessageUtils;
import com.wind.common.spring.SpringApplicationContextUtils;
import com.wind.common.util.WindClassUtils;
import com.wind.middleware.idempotent.WindIdempotentExecuteUtils;
import com.wind.middleware.idempotent.WindIdempotentKeyStorage;
import com.wind.server.i18n.WindAcceptI18nHeaderLocaleResolver;
import com.wind.server.initialization.SystemInitializer;
import com.wind.server.web.restful.FriendlyExceptionMessageConverter;
import com.wind.server.web.restful.RestfulApiRespFactory;
import com.wind.web.util.HttpServletRequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.web.context.ConfigurableWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSource;
import org.springframework.core.OrderComparator;
import org.springframework.util.StopWatch;
import org.springframework.web.servlet.LocaleResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * wind-server 应用启动监听器
 *
 * @author wuxp
 * @date 2025-10-13 13:46
 **/
@Slf4j
public class WindServerApplicationStartedEventListener implements ApplicationListener<ApplicationStartedEvent> {

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        ApplicationContext applicationContext = event.getApplicationContext();
        if (applicationContext instanceof ConfigurableWebServerApplicationContext) {
            log.info("Application started, begin execute WindServerApplicationStartedEventListener");
            // 仅在 Web 上下文中执行
            SpringI18nMessageSourceInitializer.initialize(applicationContext);
            IdempotentInitializer.initialize(applicationContext);
            ApplicationSystemInitializer.initialize(applicationContext);
        } else {
            log.info("Ignore execute none ConfigurableWebServerApplicationContext Started");
        }
    }


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
    private static class SpringI18nMessageSourceInitializer {

        /**
         * {@link Locale} 解析器
         */
        private static final AtomicReference<LocaleResolver> LOCALE_RESOLVER = new AtomicReference<>();

        private static void initialize(ApplicationContext applicationContext) {


            try {
                LOCALE_RESOLVER.set(new WindAcceptI18nHeaderLocaleResolver(Arrays.asList("Wind-Language", "Accept-Language")));
                SpringI18nMessageUtils.setMessageSource(applicationContext.getBean(MessageSource.class));
                SpringI18nMessageUtils.setLocaleSupplier(SpringI18nMessageSourceInitializer::getWebRequestLocal);
                RestfulApiRespFactory.configureFriendlyExceptionMessageConverter(FriendlyExceptionMessageConverter.i18n());
                log.info("enabled i18n supported");
            } catch (Exception ignore) {
                log.info("un enabled i18n supported");
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

    /**
     * 幂等 key 存储初始化器
     *
     * @author wuxp
     * @date 2025-10-13 10:33
     **/
    private static class IdempotentInitializer {

        private static void initialize(ApplicationContext applicationContext) {
            if (!WindClassUtils.isPresent("com.wind.middleware.idempotent.WindIdempotentExecuteUtils")) {
                log.info("unsupported wind idempotent");
                return;
            }
            try {
                WindIdempotentKeyStorage storage = applicationContext.getBean(WindIdempotentKeyStorage.class);
                WindIdempotentExecuteUtils.configureStorage(storage);
                log.info("enabled wind idempotent supported");
            } catch (Exception ignore) {
                log.info("un enabled wind idempotent supported");
            }
        }
    }

    private static class ApplicationSystemInitializer {

        private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

        private static void initialize(ApplicationContext applicationContext) {
            if (INITIALIZED.get()) {
                return;
            }
            INITIALIZED.set(true);
            // 标记应用已启动
            SpringApplicationContextUtils.markStarted();
            try {
                // 执行系统初始化器
                execSystemInitializers(applicationContext);
            } catch (Exception exception) {
                log.error("execute system initializers error", exception);
            }
        }

        private static void execSystemInitializers(ApplicationContext context) {
            log.info("begin execute SystemInitializer");
            StopWatch watch = new StopWatch();
            watch.start("system-initialization-task");
            List<SystemInitializer> initializers = new ArrayList<>(context.getBeansOfType(SystemInitializer.class).values());
            OrderComparator.sort(initializers);
            for (SystemInitializer initializer : initializers) {
                if (initializer.shouldInitialize()) {
                    try {
                        initializer.initialize();
                    } catch (Exception exception) {
                        log.error("execute initializer = {} error", initializer.getClass().getName(), exception);
                    }
                }
            }
            watch.stop();
            log.info("SystemInitializer execute end, use times = {} seconds", watch.getTotalTimeSeconds());
        }

    }

}
