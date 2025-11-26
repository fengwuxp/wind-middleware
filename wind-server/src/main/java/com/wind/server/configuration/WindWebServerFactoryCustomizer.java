package com.wind.server.configuration;

import com.wind.common.WindConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.apache.coyote.http11.Http11Nio2Protocol;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * web server 工厂自定义配置
 *
 * @author wuxp
 * @date 2024-09-20 20:10
 **/
@Configuration
@Import({WindWebServerFactoryCustomizer.TomcatEnableHttp1Ni2Customizer.class})
@Slf4j
public class WindWebServerFactoryCustomizer {

    /**
     * tomcat 开启 nio2
     */
    @Configuration
    @ConditionalOnProperty(prefix = "wind.server.tomcat.http1-nio2", name = WindConstants.ENABLED_NAME)
    public static class TomcatEnableHttp1Ni2Customizer implements WebServerFactoryCustomizer<@NonNull TomcatServletWebServerFactory> {

        @Override
        public void customize(TomcatServletWebServerFactory factory) {
            log.info("enable tomcat http1 nio2 handle model");
            factory.setProtocol(Http11Nio2Protocol.class.getName());
            factory.addConnectorCustomizers(connector -> {
                ProtocolHandler protocol = connector.getProtocolHandler();
                log.info("Tomcat({}) -- maxConnection = {}, maxThreads = {}, mainSpareThreads = {} ", protocol.getClass().getName(),
                        ((AbstractHttp11Protocol<?>) protocol).getMaxConnections(),
                        ((AbstractHttp11Protocol<?>) protocol).getMaxThreads(),
                        ((AbstractHttp11Protocol<?>) protocol).getMinSpareThreads());

            });
        }
    }


}
