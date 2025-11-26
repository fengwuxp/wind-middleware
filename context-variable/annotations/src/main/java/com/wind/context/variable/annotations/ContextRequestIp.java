package com.wind.context.variable.annotations;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static com.wind.context.variable.annotations.ContextVariable.OVERRIDE_NAME;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 注入当前上下文中的请求来源 Ip
 *
 * @author wuxp
 * @date 2023-10-24 20:55
 **/
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@ContextVariable(name = ContextVariableNames.REQUEST_IP, override = false)
public @interface ContextRequestIp {

    /**
     * {@link ContextVariable#override()}
     */
    @AliasFor(annotation = ContextVariable.class, attribute = OVERRIDE_NAME)
    boolean override() default false;

}