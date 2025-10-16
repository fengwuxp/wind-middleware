package com.wind.elasticjob.enums;

/**
 * elasticjob 内置错误处理
 * 参见：https://shardingsphere.apache.org/elasticjob/current/cn/user-manual/configuration/built-in-strategy/error-handler/
 *
 * @author wuxp
 * @date 2023-11-01 10:46
 **/
public enum ElasticJobErrorHandlerType {

    /**
     * 记录作业异常日志，但不中断作业执行
     */
    LOG,

    /**
     * 抛出系统异常并中断作业执行。
     */
    THROW,

    /**
     * 忽略系统异常且不中断作业执行
     */
    IGNORE;

}
