package com.wind.nacos;


import com.alibaba.cloud.nacos.NacosConfigProperties;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.wind.common.exception.AssertUtils;
import com.wind.configcenter.core.ConfigFunctionEvaluator;
import com.wind.configcenter.core.ConfigRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.bootstrap.ConfigurableBootstrapContext;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.bootstrap.BootstrapApplicationListener;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;
import static org.springframework.core.env.StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME;

/**
 * TODO 待优化
 * 在 bootstrap 阶段向容器注入 nacos 相关 bean
 *
 * @author wuxp
 * @date 2023-10-18 13:16
 **/
public class WindNacosBootstrapRegistryInitializer implements ApplicationListener<@NonNull ApplicationEnvironmentPreparedEvent>, Ordered {

    public static final String NACOS_CONFIG_PREFIX = "spring.cloud.nacos.config";

    private static final Logger LOGGER = Logger.getLogger(WindNacosBootstrapRegistryInitializer.class.getName());

    static final AtomicReference<ConfigService> CONFIG_SERVICE = new AtomicReference<>();

    static final AtomicReference<ConfigRepository> CONFIG_REPOSITORY = new AtomicReference<>();

    @Override
    public void onApplicationEvent(@NonNull ApplicationEnvironmentPreparedEvent event) {
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
        loadNacosPropertySources(environment);
        NacosConfigProperties result = Binder.get(environment)
                .bind(NACOS_CONFIG_PREFIX, NacosConfigProperties.class)
                // TODO 待优化 没有则返回一个空对象，兼容 process-aot 阶段
                .orElseGet(NacosConfigProperties::new);
        result.setEnvironment(environment);
        if (result.getUsername() == null || result.getPassword() == null) {
            // TODO process-aot 阶段，忽略 nacos 配置初始化
            return result;
        }
        result.init();
        return result;
    }


    private void loadNacosPropertySources(ConfigurableEnvironment environment) {
        LOGGER.info("load nacos config properties");
        environment.getPropertySources().remove(SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);

        // 重新手动加载
        environment.getPropertySources().addLast(ConfigFunctionEvaluator.getInstance().eval(new MapPropertySource(SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME,
                environment.getSystemProperties())));
        environment.getPropertySources().addLast(ConfigFunctionEvaluator.getInstance().eval(new SystemEnvironmentPropertySource(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                environment.getSystemEnvironment())));
    }

}
