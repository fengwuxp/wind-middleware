package com.wind.server.configcenter;


import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import static com.wind.server.configcenter.WindGlobalPropertySourceInitializer.SPRING_CLOUD_BOOTSTRAP_PROPERTY_SOURCE_NAME;


/**
 * 从配置中心加载中间件、应用等配置
 *
 * @author wuxp
 * @date 2023-10-18 12:55
 **/
@Slf4j
public class WindPropertySourceInitializer extends WindAbstractPropertySourceInitializer {

    @Override
    protected void load(WindPropertySourceLoader loader, ConfigurableEnvironment environment) {
        // don't listen to events in a bootstrap context
        if (environment.getPropertySources().contains(SPRING_CLOUD_BOOTSTRAP_PROPERTY_SOURCE_NAME)) {
            return;
        }
        loader.loadConfigs(environment);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 50;
    }
}
