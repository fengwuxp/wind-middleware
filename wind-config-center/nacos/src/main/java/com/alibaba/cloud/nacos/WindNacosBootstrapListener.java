package com.alibaba.cloud.nacos;


import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.google.common.base.CaseFormat;
import com.wind.common.WindConstants;
import com.wind.common.exception.AssertUtils;
import com.wind.configcenter.core.ConfigFunctionEvaluator;
import com.wind.configcenter.core.ConfigRepository;
import com.wind.nacos.NacosConfigRepository;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.cloud.bootstrap.BootstrapApplicationListener;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.springframework.cloud.bootstrap.BootstrapApplicationListener.BOOTSTRAP_PROPERTY_SOURCE_NAME;
import static org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;
import static org.springframework.core.env.StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME;

/**
 * 在 bootstrap 阶段向容器注入 nacos 相关 bean
 *
 * @author wuxp
 * @date 2023-10-18 13:16
 **/
public class WindNacosBootstrapListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

    public static final String NACOS_CONFIG_PREFIX = "spring.cloud.nacos.config";

    private static final Logger LOGGER = Logger.getLogger(WindNacosBootstrapListener.class.getName());

    static final AtomicReference<ConfigService> CONFIG_SERVICE = new AtomicReference<>();

    static final AtomicReference<ConfigRepository> CONFIG_REPOSITORY = new AtomicReference<>();

    @Override
    public void onApplicationEvent(@Nonnull ApplicationEnvironmentPreparedEvent event) {
        String enable = event.getEnvironment().getProperty(NACOS_CONFIG_PREFIX + ".enabled");
        if (Boolean.FALSE.toString().equals(enable)) {
            LOGGER.info("un enable nacos");
            return;
        }
        if (CONFIG_SERVICE.get() == null) {
            LOGGER.info("init nacos bean on bootstrap");
            NacosConfigProperties properties = createNacosProperties(event.getEnvironment());
            AssertUtils.notNull(properties, String.format("please check %s config", NACOS_CONFIG_PREFIX));
            CONFIG_SERVICE.set(buildConfigService(properties));
            CONFIG_REPOSITORY.set(new NacosConfigRepository(CONFIG_SERVICE.get(), properties));
        }
        ConfigurableBootstrapContext context = event.getBootstrapContext();
        context.registerIfAbsent(ConfigService.class, c -> CONFIG_SERVICE.get());
        context.registerIfAbsent(ConfigRepository.class, c -> CONFIG_REPOSITORY.get());
    }

    private ConfigService buildConfigService(NacosConfigProperties properties) {
        try {
            return NacosFactory.createConfigService(properties.assembleConfigServiceProperties());
        } catch (NacosException exception) {
            throw new BeanCreationException("create Nacos ConfigService error", exception);
        }
    }

    @Override
    public int getOrder() {
        return BootstrapApplicationListener.DEFAULT_ORDER + 3;
    }

    private NacosConfigProperties createNacosProperties(ConfigurableEnvironment environment) {
        // 尝试解密
        PropertySource<?> systemProperties = environment.getPropertySources().remove(SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().addAfter(BOOTSTRAP_PROPERTY_SOURCE_NAME, ConfigFunctionEvaluator.getInstance().eval(systemProperties));


        NacosConfigProperties result = new Binder(getNacosPropertySources(environment))
                .bind(NACOS_CONFIG_PREFIX, NacosConfigProperties.class)
                .orElseThrow(() -> new IllegalStateException("Cannot bind " + NACOS_CONFIG_PREFIX));
        result.setEnvironment(environment);
        return result;
    }

    @Nonnull
    private ConfigurationPropertySource getNacosPropertySources(ConfigurableEnvironment environment) {
        // 复制一份
        MutablePropertySources sources = new MutablePropertySources(environment.getPropertySources());
        if (!sources.contains(SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)) {
            // 如果不存在系统配置，手动加载
            sources.addLast(new MapPropertySource(SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, environment.getSystemProperties()));
            sources.addLast(new SystemEnvironmentPropertySource(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, environment.getSystemEnvironment()));
        }
        Map<String, Object> properties = new HashMap<>(environment.getSystemProperties());
        properties.putAll(environment.getSystemEnvironment());
        Map<String, Object> nacosProperties = new HashMap<>();
        PropertyResolver resolver = new PropertySourcesPropertyResolver(sources);
        properties.forEach((key, val) -> {
            if (key != null && key.startsWith(NACOS_CONFIG_PREFIX)) {
                String name = key.substring(NACOS_CONFIG_PREFIX.length() + 1);
                // 中划线转驼峰
                name = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name.replace(WindConstants.DASHED, WindConstants.UNDERLINE));
                nacosProperties.put(name, resolver.getProperty(key, String.class));
            }
        });
        return new MapConfigurationPropertySource(nacosProperties);
    }

}
