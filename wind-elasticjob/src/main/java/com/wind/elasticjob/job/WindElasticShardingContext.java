package com.wind.elasticjob.job;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.wind.common.exception.AssertUtils;
import org.jspecify.annotations.Nullable;
import jakarta.validation.constraints.Null;
import org.apache.shardingsphere.elasticjob.api.ShardingContext;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * wind-elasticjob 分片任务上下文，代理 ShardingContext, 增加获取任务参数的快捷方法
 *
 * @author wuxp
 * @date 2025-05-23 13:46
 **/
public record WindElasticShardingContext(ShardingContext context) {

    public WindElasticShardingContext {
        AssertUtils.notNull(context, "argument sharding context must not null");
    }

    public String getJobName() {
        return context.getJobName();
    }

    public String getTaskId() {
        return context.getTaskId();
    }

    public int getShardingTotalCount() {
        return context.getShardingTotalCount();
    }

    public int getShardingItem() {
        return context.getShardingItem();
    }

    public String getJobParameter() {
        return context.getJobParameter();
    }

    public String getShardingParameter() {
        return context.getShardingParameter();
    }

    @Nullable
    public <T> T asJobParameter(Class<T> clazz) {
        return asJobParameter(clazz, null);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getJobParameterVariable(String variableName) {
        if (context.getJobParameter() == null) {
            return null;
        }
        Map<String, Object> variables = JSON.parseObject(context.getJobParameter(), new TypeReference<Map<String, Object>>() {
        });
        return (T) variables.get(variableName);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getJobShardingParameterVariable(String variableName) {
        if (context.getShardingParameter() == null) {
            return null;
        }
        Map<String, Object> variables = JSON.parseObject(context.getShardingParameter(), new TypeReference<Map<String, Object>>() {
        });
        return (T) variables.get(variableName);
    }

    /**
     * 转换任务参数为对象
     *
     * @param clazz        类类型
     * @param defaultValue 默认值
     * @return 参数实例
     */
    @Nullable
    public <T> T asJobParameter(Class<T> clazz, @Null T defaultValue) {
        if (StringUtils.hasText(context.getJobParameter())) {
            return JSON.parseObject(context.getJobParameter(), clazz);
        }
        return defaultValue;
    }

    @Nullable
    public <T> T asJobShardingParameter(Class<T> clazz) {
        return asJobShardingParameter(clazz, null);
    }

    /**
     * 转换共享任务参数为对象
     *
     * @param clazz        类类型
     * @param defaultValue 默认值
     * @return 参数实例
     */
    @Nullable
    public <T> T asJobShardingParameter(Class<T> clazz, T defaultValue) {
        if (StringUtils.hasText(context.getShardingParameter())) {
            return JSON.parseObject(context.getShardingParameter(), clazz);
        }
        return defaultValue;
    }
}
