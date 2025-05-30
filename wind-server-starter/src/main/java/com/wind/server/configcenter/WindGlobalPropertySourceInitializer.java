package com.wind.server.configcenter;

import com.wind.common.jul.WindJulLogFactory;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.logging.Logger;

/**
 * 从配置中心加载全局配置
 *
 * @author wuxp
 * @date 2023-12-29 11:26
 * @see org.springframework.boot.context.logging.LoggingApplicationListener
 **/
public class WindGlobalPropertySourceInitializer extends WindAbstractPropertySourceInitializer {

    private static final Logger LOGGER = WindJulLogFactory.getLogger(WindGlobalPropertySourceInitializer.class);

    // @see  org.springframework.cloud.bootstrap.BootstrapApplicationListener.BOOTSTRAP_PROPERTY_SOURCE_NAME
    public static final String SPRING_CLOUD_BOOTSTRAP_PROPERTY_SOURCE_NAME = "bootstrap";

    @Override
    protected void load(WindPropertySourceLoader loader, ConfigurableEnvironment environment) {
        // don't listen to events in a bootstrap context
        if (environment.getPropertySources().contains(SPRING_CLOUD_BOOTSTRAP_PROPERTY_SOURCE_NAME)) {
            // 由于在 LoggingApplicationListener 之前执行，不会输出日志，使用 jul 输出
            LOGGER.info("load global config");
            loader.loadGlobalConfigs(environment);
        }
    }

    @Override
    public int getOrder() {
        return LoggingApplicationListener.DEFAULT_ORDER - 2;
    }
}
