package com.wind.common.spring;

import com.wind.common.exception.AssertUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * spring 上下文 utils ，非常规用法，为了简化编码
 * 请谨慎使用
 *
 * @author wuxp
 * @date 2023-12-21 18:19
 **/
@Slf4j
public class SpringApplicationContextUtils implements ApplicationContextAware {

    private static final AtomicReference<ApplicationContext> CONTEXT_HOLDER = new AtomicReference<>();

    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    public static String getProperty(String key) {
        return requireApplicationContext().getEnvironment().getProperty(key, String.class);
    }

    /**
     * Resolve ${...} placeholders in the given text.
     *
     * @param text placeholder expression
     * @return the resolved String (never {@code null})
     */
    public static String resolvePlaceholders(String text) {
        return requireApplicationContext().getEnvironment().resolvePlaceholders(text);
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        AssertUtils.notNull(applicationContext, "argument applicationContext must not null");
        if (!STARTED.get()) {
            // 设置 ApplicationEventPublisher
            SpringEventPublishUtils.setApplicationEventPublisher(applicationContext);
        }
        log.info("set spring application context, contextType = {}, start date = {}", applicationContext.getClass().getName(), new Date(applicationContext.getStartupDate()));
        CONTEXT_HOLDER.set(applicationContext);
    }

    public static void markStarted() {
        STARTED.set(true);
    }

    private static ApplicationContext requireApplicationContext() {
        ApplicationContext result = CONTEXT_HOLDER.get();
        AssertUtils.notNull(result, "spring application context not init");
        return result;
    }
}
