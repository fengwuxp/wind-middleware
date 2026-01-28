package com.wind.elasticjob.spi.listener;

import com.wind.trace.WindTracer;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.elasticjob.infra.listener.ElasticJobListener;
import org.apache.shardingsphere.elasticjob.infra.listener.ShardingContexts;

/**
 * 用于日志 trace 的监听器
 *
 * @author wuxp
 * @date 2024-06-25 10:23
 **/
@Slf4j
public class ElasticJobLogTraceListener implements ElasticJobListener {

    @Override
    public void beforeJobExecuted(ShardingContexts shardingContexts) {
        WindTracer.TRACER.trace();
        log.info("jobName = {} beforeJobExecuted, shardingContexts = {}", shardingContexts.getJobName(), shardingContexts);
    }

    @Override
    public void afterJobExecuted(ShardingContexts shardingContexts) {
        log.info("jobName = {} afterJobExecuted", shardingContexts.getJobName());
        WindTracer.TRACER.clear();
    }

    @Override
    public String getType() {
        return ElasticJobLogTraceListener.class.getSimpleName();
    }
}
