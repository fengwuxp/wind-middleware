package com.wind.trace;

import com.wind.core.ReadonlyContextVariables;

import jakarta.validation.constraints.NotBlank;

/**
 * trace context
 *
 * @author wuxp
 * @date 2023-10-20 10:31
 **/
public interface WindTraceContext extends ReadonlyContextVariables {

    /**
     * 获取 trace id
     *
     * @return traceId
     */
    @NotBlank
    String getTraceId();

}
