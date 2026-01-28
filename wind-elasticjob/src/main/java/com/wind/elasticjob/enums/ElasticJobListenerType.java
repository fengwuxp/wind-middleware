package com.wind.elasticjob.enums;

import com.wind.elasticjob.spi.listener.ElasticJobLogTraceListener;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author wuxp
 * @date 2024-06-25 10:27
 **/
@Getter
@AllArgsConstructor
public enum ElasticJobListenerType {

    /**
     * 日志 trace 监听器
     */
    LOG_TRACE(ElasticJobLogTraceListener.class.getSimpleName());

    private final String typeName;
}
