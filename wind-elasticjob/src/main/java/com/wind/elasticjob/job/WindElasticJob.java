package com.wind.elasticjob.job;

import com.wind.common.WindConstants;

import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

/**
 * wind elastic job 具有一定的自描述能力
 *
 * @author wuxp
 * @date 2024-12-15 18:19
 **/
public interface WindElasticJob {

    /**
     * 任务 cron 表达式
     */
    String getCron();

    /**
     * 任务名称
     */
    String getName();

    /**
     * @return sharding items and sharding parameters.
     */
    default String getShardingItemParameters() {
        return WindConstants.EMPTY;
    }

    /**
     * @return 任务分片数
     */
    default Integer getShardingTotalCount() {
        return 1;
    }

    /**
     * @return 忽略执行的环境
     */
    @NotNull
    default List<String> getIgnoreEnvs() {
        return Collections.emptyList();
    }

    /**
     * @return 作业启动时是否覆盖本地配置到注册中心
     */
    default boolean isOverwrite() {
        return true;
    }

    /**
     * @return 故障转移
     */
    default boolean isFailover() {
        return true;
    }

    /**
     * @return 任务错过后是否重新执行
     */
    default boolean isMisFire() {
        return true;
    }
}
