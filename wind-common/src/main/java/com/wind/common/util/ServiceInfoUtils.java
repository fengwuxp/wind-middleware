package com.wind.common.util;

import com.google.common.collect.ImmutableSet;
import com.wind.common.WindConstants;
import com.wind.common.spring.SpringApplicationContextUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * 服务信息相关工具
 *
 * @author wuxp
 * @date 2023-10-02 09:17
 **/
public final class ServiceInfoUtils {

    private static final Set<String> ONLINE_ENVS = ImmutableSet.of(WindConstants.PRE, WindConstants.PROD);

    private ServiceInfoUtils() {
        throw new AssertionError();
    }

    /**
     * @return 是否线上环境
     */
    public static boolean isOnline() {
        return ONLINE_ENVS.contains(getSpringProfilesActive());
    }

    /**
     * @return 获取应用名称
     */
    public static String getApplicationName() {
        return getProperty(WindConstants.SPRING_APPLICATION_NAME);
    }

    /**
     * @return 当前启动环境
     */
    public static String getSpringProfilesActive() {
        return getProperty(WindConstants.SPRING_PROFILES_ACTIVE);
    }

    @Nullable
    public static String getSystemProperty(String key) {
        return System.getProperty(key, System.getenv(key));
    }

    @Nullable
    private static String getProperty(String key) {
        String result = getSystemProperty(key);
        if (StringUtils.hasText(result)) {
            return result;
        }
        return SpringApplicationContextUtils.getProperty(key);
    }

}
