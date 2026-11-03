package com.wind.common.util;

import com.wind.common.WindConstants;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * 服务信息相关工具
 *
 * @author wuxp
 * @date 2023-10-02 09:17
 **/
public final class ServiceInfoUtils {

    private static final AtomicReference<Function<String, String>> APPLICATION_PROPERTY_SUPPLIER = new AtomicReference<>(k -> null);

    private static final Set<String> ONLINE_ENVS = Set.of(WindConstants.PRE, WindConstants.PROD);

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
        return APPLICATION_PROPERTY_SUPPLIER.get().apply(key);
    }

    public static void configure(Function<String, String> propertySupplier) {
        APPLICATION_PROPERTY_SUPPLIER.set(propertySupplier);
    }
}
