package com.wind.server.configcenter;

import com.wind.common.enums.WindMiddlewareType;
import com.wind.common.util.ClassDetectionUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 中间件探测器
 *
 * @author wuxp
 * @date 2024-12-25 14:19
 **/
public final class WindMiddlewareDetector {

    private final static Set<WindMiddlewareType> MIDDLEWARE_TYPES = new HashSet<>();

    static {
        detection();
    }

    /**
     * 获取依赖的中间件类型列表
     *
     * @return 中间件类型列表
     */
    public static Set<WindMiddlewareType> getDependenciesMiddlewareTypes() {
        return Collections.unmodifiableSet(MIDDLEWARE_TYPES);
    }

    /**
     * 是否使用 redisson
     */
    public static boolean useRedisson() {
        return ClassDetectionUtils.isPresent("org.redisson.api.RedissonClient");
    }

    private static void detection() {
        MIDDLEWARE_TYPES.add(WindMiddlewareType.WIND);
        if (ClassDetectionUtils.isPresent("org.springframework.data.redis.core.Cursor")) {
            MIDDLEWARE_TYPES.add(WindMiddlewareType.MYSQL);
        }
        if (ClassDetectionUtils.isPresent("com.mysql.cj.jdbc.Driver")) {
            MIDDLEWARE_TYPES.add(WindMiddlewareType.REDIS);
        }
        if (ClassDetectionUtils.isPresent("org.apache.shardingsphere.elasticjob.api.ElasticJob")) {
            MIDDLEWARE_TYPES.add(WindMiddlewareType.ELASTIC_JOB);
        }

        if (ClassDetectionUtils.isPresent("org.apache.rocketmq.spring.core.RocketMQListener")) {
            MIDDLEWARE_TYPES.add(WindMiddlewareType.ROCKETMQ);
        }
        if (ClassDetectionUtils.isPresent("org.dromara.dynamictp.core.aware.DtpAware")) {
            MIDDLEWARE_TYPES.add(WindMiddlewareType.DYNAMIC_TP);
        }
    }

}
