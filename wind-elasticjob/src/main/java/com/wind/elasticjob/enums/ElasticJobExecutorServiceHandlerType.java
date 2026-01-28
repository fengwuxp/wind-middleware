package com.wind.elasticjob.enums;

/**
 * elasticjob 执行器服务处理方式
 *
 * @author wuxp
 * @date 2026-01-28 12:00
 * @see org.apache.shardingsphere.elasticjob.infra.handler.threadpool.JobExecutorServiceHandler
 **/
public enum ElasticJobExecutorServiceHandlerType {

    /**
     * cpu 密集型
     */
    CPU,

    /**
     * 单线程
     */
    SINGLE_THREAD,

    /**
     * Wind Single
     */
    SINGLE;
}
