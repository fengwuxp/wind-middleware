package com.wind.api.core;

import org.springframework.lang.Nullable;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * Api 统一响应
 *
 * @param <T>
 * @author wuxp
 */
public interface ApiResponse<T> extends Serializable {

    /**
     * {@link #isSuccess()} 如果返回false,则表明业务处理失败，该数据则为业务失败时的数据
     *
     * @return 本次响应的数据
     */
    @Nullable
    T getData();

    /**
     * 本次请求业务上是否成功
     *
     * @return <code>true</code> 业务处理成功
     */
    boolean isSuccess();

    /**
     * @return 错误码
     */
    @NotBlank
    String getErrorCode();

    /**
     * 获取消息描述
     *
     * @return 业务处理失败时返回的消息描述
     */
    @Nullable
    String getErrorMessage();

    /**
     * 请求 traceId
     *
     * @return traceId
     */
    @NotBlank
    String getTraceId();
}
