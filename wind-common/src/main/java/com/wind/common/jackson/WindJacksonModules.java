package com.wind.common.jackson;

import tools.jackson.databind.JacksonModule;

/**
 * 对 Jackson 模块的兼容入口。
 *
 * @author wuxp
 * @date 2026-03-02 11:04
 * @deprecated 使用 {@link com.wind.jackson.WindJacksonModules}
 **/
@Deprecated(since = "4.0.0", forRemoval = true)
public final class WindJacksonModules {

    private WindJacksonModules() {
        throw new AssertionError();
    }


    /**
     * 创建 like ISO8601 时间格式的 jackson 模块
     *
     * @return 模块
     */
    public static JacksonModule iso8601LikeJavaTimeModule() {
        return com.wind.jackson.WindJacksonModules.iso8601LikeJavaTimeModule();
    }

    /**
     * 创建 api 模块
     *
     * @return 模块
     */
    public static JacksonModule apiModule() {
        return com.wind.jackson.WindJacksonModules.apiModule();
    }
}
