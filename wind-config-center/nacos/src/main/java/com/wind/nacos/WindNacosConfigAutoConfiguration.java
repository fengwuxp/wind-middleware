package com.wind.nacos;


import com.alibaba.nacos.api.config.ConfigService;
import com.wind.configcenter.core.ConfigRepository;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@AllArgsConstructor
@Configuration(proxyBeanMethods = false)
public class WindNacosConfigAutoConfiguration {

    @Bean
    public ConfigService nacosConfigService() {
        return WindNacosBootstrapListener.CONFIG_SERVICE.get();
    }

    @Bean
    public ConfigRepository nacosConfigRepository() {
        return WindNacosBootstrapListener.CONFIG_REPOSITORY.get();
    }

}
