package com.wind.elasticjob;

import com.wind.common.util.ServiceInfoUtils;
import com.wind.elasticjob.enums.ElasticJobErrorHandlerType;
import com.wind.elasticjob.enums.ElasticJobListenerType;
import com.wind.elasticjob.enums.ElasticJobShardingStrategyType;
import com.wind.elasticjob.job.WindElasticDataFlowJob;
import com.wind.elasticjob.job.WindElasticJob;
import com.wind.elasticjob.job.WindElasticSimpleJob;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.elasticjob.api.ElasticJob;
import org.apache.shardingsphere.elasticjob.api.JobConfiguration;
import org.apache.shardingsphere.elasticjob.lite.api.bootstrap.impl.ScheduleJobBootstrap;
import org.apache.shardingsphere.elasticjob.reg.base.CoordinatorRegistryCenter;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一注册 ElasticJob
 *
 * @author wuxp
 * @date 2024-12-15
 */
@Slf4j
public class WindElasticJobRegistrar {

    private final CoordinatorRegistryCenter registryCenter;

    private final List<ScheduleJobBootstrap> bootstraps = new ArrayList<>();

    public WindElasticJobRegistrar(CoordinatorRegistryCenter registryCenter) {
        this.registryCenter = registryCenter;
    }

    @EventListener
    public void onApplicationStartedEvent(ApplicationStartedEvent event) {
        ConfigurableApplicationContext applicationContext = event.getApplicationContext();
        // 注册简单任务
        applicationContext.getBeansOfType(WindElasticSimpleJob.class).values().forEach(this::registerJob);
        // 注册 data flow job
        applicationContext.getBeansOfType(WindElasticDataFlowJob.class).values().forEach(this::registerJob);
        log.info("register elastic jobs completed, size = {}", bootstraps.size());
    }

    @EventListener
    public void onContextClosedEvent(ContextClosedEvent event) {
        log.info("shutdown elastic job, size = {}", bootstraps.size());
        bootstraps.forEach(ScheduleJobBootstrap::shutdown);
    }

    /**
     * 注册任务
     *
     * @param job 任务
     */
    public void registerJob(WindElasticJob job) {
        String profilesActive = ServiceInfoUtils.getSpringProfilesActive();
        if (job.getIgnoreEnvs().contains(profilesActive)) {
            log.info("elastic job = {} ignore register, spring profiles active = {}", profilesActive, job.getName());
        } else {
            ScheduleJobBootstrap bootstrap = new ScheduleJobBootstrap(registryCenter, (ElasticJob) job, createJobConfiguration(job));
            bootstrap.schedule();
            bootstraps.add(bootstrap);
        }
    }

    private JobConfiguration createJobConfiguration(WindElasticJob job) {
        return JobConfiguration.newBuilder(job.getName(), job.getShardingTotalCount())
                .shardingItemParameters(job.getShardingItemParameters())
                .jobShardingStrategyType(ElasticJobShardingStrategyType.ROUND_ROBIN.name())
                .jobErrorHandlerType(ElasticJobErrorHandlerType.LOG.name())
                // 统一开启任务的日志 trace
                .jobListenerTypes(ElasticJobListenerType.LOG_TRACE.getTypeName())
                .cron(job.getCron())
                // 作业启动时是否覆盖本地配置到注册中心
                .overwrite(job.isOverwrite())
                // 故障转移
                .failover(job.isFailover())
                // 任务错过后是否重新执行
                .misfire(job.isMisFire())
                .build();
    }
}
