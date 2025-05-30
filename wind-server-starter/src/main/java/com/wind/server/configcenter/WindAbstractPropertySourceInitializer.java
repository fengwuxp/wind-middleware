package com.wind.server.configcenter;

import com.wind.configcenter.core.ConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.lang.NonNull;

import static com.wind.common.WindConstants.WIND_SERVER_CONFIG_CENTER_PROPERTIES_PREFIX;

/**
 * @author wuxp
 * @date 2023-12-29 11:28
 **/
@Slf4j
public abstract class WindAbstractPropertySourceInitializer implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

    @Override
    public void onApplicationEvent(@NonNull ApplicationEnvironmentPreparedEvent event) {
        WindPropertySourceLoader locator = getWindPropertySourceLocator(event);
        if (locator == null) {
            return;
        }
        load(locator, event.getEnvironment());
    }

    protected abstract void load(WindPropertySourceLoader loader, ConfigurableEnvironment environment);

    private WindPropertySourceLoader getWindPropertySourceLocator(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        ConfigurableBootstrapContext context = event.getBootstrapContext();
        try {
            WindConfigCenterProperties properties = new WindConfigCenterProperties();
            Binder.get(environment).bind(WIND_SERVER_CONFIG_CENTER_PROPERTIES_PREFIX, Bindable.ofInstance(properties));
            return new WindPropertySourceLoader(context.get(ConfigRepository.class), properties);
        } catch (IllegalStateException e) {
            log.info("unsupported config center");
        }
        return null;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
