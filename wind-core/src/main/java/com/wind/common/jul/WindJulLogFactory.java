package com.wind.common.jul;

import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

/**
 * java util logging logger 工厂，在应用未绑定 slf4j-log 实现时使用（例如：spring 应用启动阶段）
 *
 * @author wuxp
 * @date 2025-03-12 18:54
 **/
public final class WindJulLogFactory {

    private WindJulLogFactory() {
        throw new AssertionError();
    }

    public static Logger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    public static Logger getLogger(String name) {
        Logger result = Logger.getLogger(name);
        result.addHandler(new ConsoleHandler());
        return result;
    }
}
