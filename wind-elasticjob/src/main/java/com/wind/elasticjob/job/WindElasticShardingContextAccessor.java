package com.wind.elasticjob.job;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import org.apache.shardingsphere.elasticjob.api.ShardingContext;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * ShardingContext 的访问器与参数解析门面
 * 用于屏蔽 ElasticJob 参数的字符串 / JSON 细节
 *
 * @author wuxp
 * @date 2025-05-23 13:46
 **/
public final class WindElasticShardingContextAccessor {

    private final ShardingContext context;

    private WindElasticShardingContextAccessor(@NonNull ShardingContext context) {
        this.context = context;
    }

    public static WindElasticShardingContextAccessor of(@NonNull ShardingContext context) {
        return new WindElasticShardingContextAccessor(context);
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

    /**
     * 获取任务参数
     *
     * @param clazz 类类型
     * @return 参数对象
     */
    @Nullable
    public <T> T asJobParameter(@NonNull Class<T> clazz) {
        return asJobParameter(clazz, null);
    }

    /**
     * 转换任务参数为对象
     *
     * @param clazz        类类型
     * @param defaultValue 默认值
     * @return 参数实例
     */
    @Nullable
    public <T> T asJobParameter(@NonNull Class<T> clazz, @Nullable T defaultValue) {
        if (StringUtils.hasText(context.getJobParameter())) {
            return JSON.parseObject(context.getJobParameter(), clazz);
        }
        return defaultValue;
    }

    /**
     * 获取任务参数
     *
     * @param clazz 类类型
     * @return 参数列表
     */
    public <T> List<T> asJobParameters(@NonNull Class<T> clazz) {
        if (StringUtils.hasText(context.getJobParameter())) {
            return JSON.parseArray(context.getJobParameter(), clazz);
        }
        return Collections.emptyList();
    }

    /**
     * 获取任务参数变量
     *
     * @param variableName 变量名称
     * @return 变量值
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getJobParameterVariable(@NonNull String variableName) {
        return (T) getJobParameters().get(variableName);
    }

    /**
     * 获取任务参数
     *
     * @return 任务参数
     */
    @NonNull
    public Map<String, Object> getJobParameters() {
        if (StringUtils.hasText(context.getJobParameter())) {
            return JSON.parseObject(context.getJobParameter(), new TypeReference<>() {
            });
        }
        return Collections.emptyMap();
    }

    /**
     * 获取任务参数变量
     *
     * @param variableName 变量名称
     * @return 变量值
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getJobShardingParameterVariable(@NonNull String variableName) {
        return (T) getJobShardingParameters().get(variableName);
    }

    /**
     * 获取任务共享参数
     *
     * @return 共享参数
     */
    @NonNull
    public Map<String, Object> getJobShardingParameters() {
        if (StringUtils.hasText(context.getShardingParameter())) {
            return JSON.parseObject(context.getShardingParameter(), new TypeReference<>() {
            });
        }
        return Collections.emptyMap();
    }

    /**
     * 获取任务共享参数
     *
     * @param clazz 类类型
     * @return 列表
     */
    public <T> List<T> asJobShardingParameters(@NonNull Class<T> clazz) {
        if (StringUtils.hasText(context.getShardingParameter())) {
            return JSON.parseArray(context.getShardingParameter(), clazz);
        }
        return Collections.emptyList();
    }

    /**
     * 转换任务共享参数为对象
     *
     * @param clazz 类类型
     * @return 参数对象
     */
    @Nullable
    public <T> T asJobShardingParameter(@NonNull Class<T> clazz) {
        return asJobShardingParameter(clazz, null);
    }

    /**
     * 转换共享任务参数为对象
     *
     * @param clazz        类类型
     * @param defaultValue 默认值
     * @return 参数值
     */
    @Nullable
    public <T> T asJobShardingParameter(@NonNull Class<T> clazz, @Nullable T defaultValue) {
        if (StringUtils.hasText(context.getShardingParameter())) {
            return JSON.parseObject(context.getShardingParameter(), clazz);
        }
        return defaultValue;
    }
}
