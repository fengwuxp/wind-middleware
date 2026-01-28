package com.wind.elasticjob.spi.executor;

import com.wind.common.util.ExecutorServiceUtils;
import com.wind.elasticjob.enums.ElasticJobExecutorServiceHandlerType;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.elasticjob.infra.handler.threadpool.JobExecutorServiceHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Wind 单任务执行器服务处理方式
 *
 * @author wuxp
 * @date 2026-01-28 12:04
 **/
@Slf4j
public class WindSingleElasticJobExecutorServiceHandler implements JobExecutorServiceHandler {

    /**
     * 任务执行器服务
     *
     * @key jobName
     */
    private static final Map<String, ExecutorService> EXECUTOR_SERVICES = new ConcurrentHashMap<>();

    @Override
    public ExecutorService createExecutorService(String jobName) {
        return EXECUTOR_SERVICES.computeIfAbsent(jobName, key -> ExecutorServiceUtils.named("elastic-job-" + jobName)
                .rejectedExecutionHandler((r, executor) -> log.error("{} ExecutorService 已满, 拒绝新任务入队", jobName))
                .buildNativeExecutor());
    }

    @Override
    public String getType() {
        return ElasticJobExecutorServiceHandlerType.SINGLE.name();
    }

    /**
     * 销毁任务执行器服务工厂
     */
    public static void destroyExecutorFactory() {
        EXECUTOR_SERVICES.values().forEach(ExecutorService::shutdown);
        EXECUTOR_SERVICES.clear();
    }
}
