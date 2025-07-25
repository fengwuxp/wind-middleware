package com.wind.common.spring;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import jakarta.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 手动注册 Bean 支持
 * 参见：https://mp.weixin.qq.com/s/gwX9KuVRCvdoKrOIwHRZ-Q
 *
 * @author wuxp
 * @date 2023-11-15 08:24
 **/
@Slf4j
public class WindBeanDefinitionRegistryPostProcessor implements ApplicationContextInitializer<ConfigurableApplicationContext>,
        BeanDefinitionRegistryPostProcessor {

    /**
     * @key bean name
     * @value bean definition supplier
     */
    private static final Map<String, Supplier<BeanDefinition>> BEAN_DEFINITIONS = new ConcurrentHashMap<>();

    static {
        BEAN_DEFINITIONS.put(SpringApplicationContextUtils.class.getName(), () -> new RootBeanDefinition(SpringApplicationContextUtils.class));
    }

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        log.info("Add WindBeanDefinitionRegistryPostProcessor");
        context.addBeanFactoryPostProcessor(this);
    }

    @Override
    public void postProcessBeanDefinitionRegistry(@Nonnull BeanDefinitionRegistry registry) throws BeansException {
        log.info("Register Wind BeanDefinition");
        register(registry);
    }

    @Override
    public void postProcessBeanFactory(@Nonnull ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

    private void register(BeanDefinitionRegistry registry) {
        BEAN_DEFINITIONS.forEach((name, supplier) -> {
            if (!registry.containsBeanDefinition(name)) {
                registry.registerBeanDefinition(name, supplier.get());
            }
        });
    }

    static void registerBean(String bean, Supplier<BeanDefinition> supplier) {
        BEAN_DEFINITIONS.put(bean, supplier);
    }

}
