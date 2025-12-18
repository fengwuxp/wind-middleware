package com.wind.server.configcenter;

import com.wind.common.enums.WindMiddlewareType;
import com.wind.common.util.WindClassUtils;

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

    private static final Set<WindMiddlewareType> MIDDLEWARE_TYPES = new HashSet<>();

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
        return WindClassUtils.isPresent("org.redisson.api.RedissonClient");
    }

    private static void detection() {
        MIDDLEWARE_TYPES.add(WindMiddlewareType.WIND);
        if (WindClassUtils.isPresent("org.springframework.data.redis.core.Cursor")) {
            MIDDLEWARE_TYPES.add(WindMiddlewareType.MYSQL);
        }
        if (WindClassUtils.isPresent("com.mysql.cj.jdbc.Driver")) {
            MIDDLEWARE_TYPES.add(WindMiddlewareType.REDIS);
        }
        if (WindClassUtils.isPresent("org.apache.shardingsphere.elasticjob.api.ElasticJob")) {
            MIDDLEWARE_TYPES.add(WindMiddlewareType.ELASTIC_JOB);
        }

        if (WindClassUtils.isPresent("org.apache.rocketmq.spring.core.RocketMQListener")) {
            MIDDLEWARE_TYPES.add(WindMiddlewareType.ROCKETMQ);
        }
        if (WindClassUtils.isPresent("org.dromara.dynamictp.core.aware.DtpAware")) {
            MIDDLEWARE_TYPES.add(WindMiddlewareType.DYNAMIC_TP);
        }
    }

}
