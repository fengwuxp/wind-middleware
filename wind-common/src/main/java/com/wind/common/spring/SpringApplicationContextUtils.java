package com.wind.common.spring;

import com.google.common.annotations.Beta;
import com.wind.common.exception.AssertUtils;
import com.wind.common.util.ServiceInfoUtils;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.GenericApplicationContext;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

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

    /**
     * 获取 Bean，非常规用法，使用者必须清楚 Spring Context 的生命周期，避免非预期的异常或不稳定的运行结果
     *
     * @param clazz bean class
     * @return bean
     */
    @NonNull
    @Beta
    static <T> T getBean(Class<T> clazz) {
        ApplicationContext applicationContext = requireApplicationContext();
        AssertUtils.isInstanceOf(GenericApplicationContext.class, applicationContext, "argument applicationContext must be GenericApplicationContext");
        return applicationContext.getBean(clazz);
    }

    /**
     * 获取指定类型的 Bean，仅在没有匹配 Bean 时使用默认值。
     * 候选不唯一或 Bean 创建失败时，仍向调用方传播异常。
     *
     * @param clazz           Bean 类型
     * @param defaultSupplier 默认值提供者
     * @param <T>             Bean 类型
     * @return 已注册的 Bean 或默认值
     */
    @NonNull
    @Beta
    static <T> T getBean(@NonNull Class<T> clazz, @NonNull Supplier<T> defaultSupplier) {
        return requireApplicationContext().getBeanProvider(clazz).getIfAvailable(defaultSupplier);
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
        ServiceInfoUtils.configure(SpringApplicationContextUtils::getProperty);
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
