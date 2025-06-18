package com.wind.script.javet;

import com.alibaba.fastjson2.JSON;
import com.wind.common.exception.AssertUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.lang.Nullable;

import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

/**
 * js 执行 java 方法的 Invoker
 *
 * @author wuxp
 * @version JsExecJavaMethodInvoker.java, v 0.1 2023年06月08日 09:50 wuxp
 */
public abstract class JsExecJavaMethodInvoker {

    /**
     * 注册到 js 上下文中的变量名称
     */
    private static final String DEFAULT_JS_VARIABLE_NAME = "$java";

    /**
     * 执行 java 方法
     *
     * @param execMethodId 执行 java 方法标识或指令标识（用于分发调用）
     * @param parameter    入参，json 格式
     * @return 方法返回值 json 格式
     */
    @Nullable
    public String exec(@NotNull String execMethodId, @Nullable String parameter) {
        AssertUtils.hasLength(execMethodId, "parameter execMethodId must not empty");
        return toJsonString(execInternal(execMethodId, parameter));
    }

    protected abstract Object execInternal(@NotNull String execMethodId, @Nullable String parameter);

    /**
     * 注册到 js 上下文中的变量名称，方法调用时将使用该变量
     * 例如：$java#exec('a','{}')
     */
    public String getJsObjectName() {
        return DEFAULT_JS_VARIABLE_NAME;
    }

    /**
     * 获取需要注册的方法描述信息列表
     */
    public List<JavaMethodDescriptor> getJavaMethodDescriptors() {
        String name = "exec";
        return Collections.singletonList(JavaMethodDescriptor.of(name, name, new Class<?>[]{String.class, String.class}));
    }

    @Nullable
    protected String toJsonString(Object result) {
        if (result == null) {
            return null;
        }
        return JSON.toJSONString(result);
    }

    @Getter
    @AllArgsConstructor
    public static class JavaMethodDescriptor {

        private final String javaMethodName;

        private final String jsFunctionName;

        private final Class<?>[] parameterTypes;

        public static JavaMethodDescriptor of(String javaMethodName, String jsFunctionName, Class<?>[] parameterTypes) {
            return new JavaMethodDescriptor(javaMethodName, jsFunctionName, parameterTypes);
        }
    }
}