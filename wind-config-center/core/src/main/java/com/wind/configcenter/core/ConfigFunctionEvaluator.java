package com.wind.configcenter.core;

import com.wind.common.WindConstants;
import com.wind.common.jul.WindJulLogFactory;
import com.wind.script.spring.SpringExpressionEvaluator;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.springframework.core.env.StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME;

/**
 * 用于执行配置中的 spring expression 函数表达式，用于解析或替换配置中的敏感内容或配置
 * 支持如下语法：
 * # 配置解密
 * example.username=@{DEC('xxxx')}
 * # 通过 KMS 等方式提供配置
 * example.username=@{CRE('xxxx')}
 *
 * @author wuxp
 * @date 2025-03-11 10:17
 **/
public record ConfigFunctionEvaluator(Object rootObject) {

    private static final Logger LOGGER = WindJulLogFactory.getLogger(ConfigFunctionEvaluator.class);

    private static final Set<String> REQUIRES_DECRYPT_NAMES = Set.of(SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);

    private static final SpringExpressionEvaluator EVALUATOR = new SpringExpressionEvaluator(new TemplateParserContext("@{", "}"));

    private static final AtomicReference<ConfigFunctionEvaluator> INSTANCE = new AtomicReference<>();

    /**
     * 执行配置内容中的函数
     *
     * @param name    配置名称
     * @param content 配置内容
     * @return 执行过函数替换后的内容
     */
    public String eval(String name, String content) {
        try {
            return EVALUATOR.eval(content, new StandardEvaluationContext(rootObject));
        } catch (Exception exception) {
            LOGGER.info("eval config content exception, config name = " + name + ", content = " + content + ", message = " + exception.getMessage());
            return content;
        }
    }

    /**
     * 执行配置内容中的函数
     *
     * @param source 配置数据
     * @return 执行过函数替换后的配置数据
     */
    public PropertySource<?> eval(PropertySource<?> source) {
        if (requiresDecrypt(source)) {
            Map<String, Object> keyValues = ((MapPropertySource) source).getSource();
            Map<String, Object> result = new HashMap<>(keyValues);
            keyValues.forEach((key, value) -> result.put(key, eval(source.getName() + WindConstants.SHARP + key, asText(value))));
            return new MapPropertySource(source.getName(), Collections.unmodifiableMap(result));
        }
        return source;
    }

    private String asText(Object val) {
        if (val instanceof OriginTrackedValue trackedValue) {
            Object value = trackedValue.getValue();
            if (value instanceof String str) {
                return str;
            }
        }
        if (val instanceof String str) {
            return str;
        }
        return String.valueOf(val);
    }

    private boolean requiresDecrypt(PropertySource<?> source) {
        String name = source.getName();
        if (REQUIRES_DECRYPT_NAMES.contains(name)) {
            return true;
        }
        return source instanceof MapPropertySource;
    }

    public static ConfigFunctionEvaluator getInstance() {
        if (INSTANCE.get() == null) {
            synchronized (ConfigFunctionEvaluator.class) {
                if (INSTANCE.get() == null) {
                    INSTANCE.set(new ConfigFunctionEvaluator(new ConfigFunctionRootObject()));
                }
            }
        }
        return INSTANCE.get();
    }
}
