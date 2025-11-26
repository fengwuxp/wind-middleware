package com.wind.script.auditlog;

import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.util.Map;

import static com.wind.common.WindHttpConstants.HTTP_REQUEST_IP_ATTRIBUTE_NAME;
import static com.wind.common.WindHttpConstants.HTTP_USER_AGENT_HEADER_NAME;

/**
 * @author wuxp
 * @date 2023-09-23 09:43
 **/
@Getter
@Builder
public class AuditLogContent {

    /**
     * 日志内容
     */
    private final String log;

    /**
     * 操作日志业务类型
     */
    private final String type;

    /**
     * 操作类型，比如：CREATE
     */
    private final String operation;

    /**
     * 操作备注
     */
    private final String remark;

    /**
     * 请求参数
     */
    private final Object[] args;

    /**
     * 方法方法返回值
     */
    @Nullable
    private final Object resultValue;

    /**
     * 日志记录时的上下文变量
     * 例如：当前操作用户、请求 ip 等
     */
    private final Map<String, Object> contextVariables;

    /**
     * 审计日志操作分组
     */
    private final String group;

    /**
     * 审计操作资源类型，可以是操作对象的类名 OR 表名
     */
    private final String resourceType;

    /**
     * 操作的目标资源标识
     */
    @Nullable
    private final Object resourceId;

    /**
     * 异常
     */
    @Nullable
    private final Throwable throwable;

    /**
     * 获取上线文变量
     *
     * @param key 变量 key
     * @return 变量值
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getContextVariable(String key) {
        return (T) contextVariables.get(key);
    }

    /**
     * @return 请求来源 ip
     */
    @Nullable
    public String getRequestSourceIp() {
        return getContextVariable(HTTP_REQUEST_IP_ATTRIBUTE_NAME);
    }

    /**
     * @return 请求 UserAgent
     */
    @Nullable
    public String getRequestUserAgent() {
        return getContextVariable(HTTP_USER_AGENT_HEADER_NAME);
    }

}
