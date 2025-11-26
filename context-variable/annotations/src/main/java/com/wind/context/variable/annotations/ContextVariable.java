package com.wind.context.variable.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 上下文变量注入
 *
 * @author wuxp
 * @date 2023-10-24 18:56
 **/
@Target({FIELD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface ContextVariable {

     String OVERRIDE_NAME = "override";

    /**
     * @return 上下文参数变量名称
     */
    String name() default "";

    /**
     * 默认支持 spring expression，例如：#name
     * 优先使用 {@link #name()}
     *
     * @return 取值表达
     */
    String expression() default "";

    /**
     * @return 注入时是否覆盖原有值
     */
    boolean override() default true;
}
