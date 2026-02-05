package com.wind.logging.logback.middleware;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import com.wind.common.WindConstants;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.slf4j.Marker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.wind.common.WindConstants.MIDDLEWARE_MDC_VARIABLE_NAME;

/**
 * 标记是否为中间件 Filter
 *
 * @author wuxp
 * @date 2026-02-05 09:28
 **/
public class MiddlewareTagTurboFilter extends TurboFilter {

    private static final Map<String, String> PACKAGE_MAPPING = new ConcurrentHashMap<>(Map.of(
            "com.aliyun.oss", "oss",
            "com.aliyun.kms", "kms",
            "org.apache.shardingsphere.elasticjob", "elasticjob",
            "org.redisson", "redisson"
    ));

    /**
     * 添加中间件包映射
     *
     * @param packageName    包名
     * @param middlewareType 中间件类型
     */
    public static void mapping(@NonNull String packageName, @NonNull String middlewareType) {
        PACKAGE_MAPPING.put(packageName, middlewareType);
    }

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        String loggerName = logger.getName();
        for (var entry : PACKAGE_MAPPING.entrySet()) {
            if (loggerName.startsWith(entry.getKey())) {
                MDC.put(MIDDLEWARE_MDC_VARIABLE_NAME, entry.getValue());
                return FilterReply.NEUTRAL;
            }
        }
        MDC.put(MIDDLEWARE_MDC_VARIABLE_NAME, WindConstants.UNKNOWN);
        return FilterReply.NEUTRAL;
    }
}
