package com.wind.server.initialization;

import org.springframework.core.Ordered;

/**
 * 系统初始化器，用于在系统启动时初始数据
 *
 * @author wuxp
 * @date 2023-10-22 07:48
 **/
public interface SystemInitializer extends Ordered {

    /**
     * 初始化
     */
    void initialize();

    /**
     * 是否需要初始化
     *
     * @return true 需要
     */
    default boolean requireInitialize() {
        return true;
    }

    @Override
    default int getOrder() {
        return 0;
    }
}
