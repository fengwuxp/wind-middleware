package com.wind.server.configuration;

import com.wind.common.exception.AssertUtils;
import com.wind.context.injection.MethodParameterInjector;
import com.wind.script.auditlog.AuditLogRecorder;
import com.wind.script.auditlog.ScriptAuditLogRecorder;
import com.wind.server.actuator.health.GracefulShutdownHealthIndicator;
import com.wind.server.aop.WindControllerMethodInterceptor;
import com.wind.server.logging.WebAuditLogRecorder;
import com.wind.server.web.exception.RestfulErrorAttributes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultBeanFactoryPointcutAdvisor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.NestedRuntimeException;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.wind.common.WindConstants.CONTROLLER_METHOD_ASPECT_NAME;
import static com.wind.common.WindConstants.ENABLED_NAME;
import static com.wind.common.WindConstants.TRUE;
import static com.wind.common.WindConstants.WIND_SERVER_PROPERTIES_PREFIX;

/**
 * @author wuxp
 * @date 2023-09-26 15:53
 **/
@Configuration
@EnableConfigurationProperties(value = {WindServerProperties.class})
@ConditionalOnProperty(prefix = WIND_SERVER_PROPERTIES_PREFIX, name = ENABLED_NAME, havingValue = TRUE, matchIfMissing = true)
@Slf4j
public class WindServerAutoConfiguration {

    /**
     * 只作用在 @Controller 或 @RestController 类上
     */
    private static final String ASPECT_CONTROLLER_CLASS_EXPRESSION = Stream.of(
            Controller.class,
            RestController.class
    ).map(clazz -> "@within(" + clazz.getName() + ")").collect(Collectors.joining(" || "));

    /**
     * 只拦截被 Web 请求映射注解标记的方法
     */
    private static final String ASPECT_CONTROLLER_METHOD_EXPRESSION = Stream.of(
            RequestMapping.class,
            GetMapping.class,
            PostMapping.class,
            PutMapping.class,
            PatchMapping.class,
            DeleteMapping.class
    ).map(clazz -> "@annotation(" + clazz.getName() + ")").collect(Collectors.joining(" || "));


    @Bean
    public RestfulErrorAttributes restfulErrorAttributes() {
        return new RestfulErrorAttributes(new DefaultErrorAttributes());
    }

    @Bean
    @ConditionalOnBean(AuditLogRecorder.class)
    @ConditionalOnMissingBean(ScriptAuditLogRecorder.class)
    public WebAuditLogRecorder webAuditLogRecorder(AuditLogRecorder recorder) {
        return new WebAuditLogRecorder(recorder);
    }

    @Bean
    @ConditionalOnProperty(prefix = CONTROLLER_METHOD_ASPECT_NAME, name = ENABLED_NAME, havingValue = TRUE, matchIfMissing = true)
    public WindControllerMethodInterceptor windControllerMethodInterceptor(ApplicationContext context,
                                                                           Collection<MethodParameterInjector> injectors) {
        ScriptAuditLogRecorder recorder = null;
        try {
            recorder = context.getBean(ScriptAuditLogRecorder.class);
        } catch (NestedRuntimeException exception) {
            log.info("un enable audit log");
        }
        return new WindControllerMethodInterceptor(recorder, MethodParameterInjector.composite(injectors));
    }

    @Bean
    @ConditionalOnBean(WindControllerMethodInterceptor.class)
    @ConditionalOnProperty(prefix = CONTROLLER_METHOD_ASPECT_NAME, name = "expression")
    public DefaultBeanFactoryPointcutAdvisor windControllerMethodAspectPointcutAdvisor(WindControllerMethodInterceptor advice,
                                                                                       WindServerProperties properties) {
        String expression = properties.getControllerMethodAspect().getExpression();
        AssertUtils.hasLength(expression, String.format("%s 未配置", CONTROLLER_METHOD_ASPECT_NAME));
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression(String.format("(%s) && (%s) && %s", ASPECT_CONTROLLER_CLASS_EXPRESSION, ASPECT_CONTROLLER_METHOD_EXPRESSION, expression));
        DefaultBeanFactoryPointcutAdvisor advisor = new DefaultBeanFactoryPointcutAdvisor();
        // 拦截优先级设置为最高
        advisor.setOrder(Ordered.HIGHEST_PRECEDENCE);
        advisor.setPointcut(pointcut);
        advisor.setAdvice(advice);
        return advisor;
    }

    @Bean
    @ConditionalOnProperty(prefix = WIND_SERVER_PROPERTIES_PREFIX + ".health.graceful-shutdown", name = ENABLED_NAME, havingValue = TRUE)
    public GracefulShutdownHealthIndicator gracefulShutdownHealthIndicator() {
        return new GracefulShutdownHealthIndicator();
    }

}
