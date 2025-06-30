package com.wind.script.javet;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.util.TypeUtils;
import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interop.V8Host;
import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.interop.callback.JavetCallbackContext;
import com.caoccao.javet.values.reference.V8ValueObject;
import com.wind.common.annotations.VisibleForTesting;
import com.wind.common.exception.AssertUtils;
import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 使用 V8 引擎执行 JavaScript 代码
 * 参见：<a href="https://github.com/caoccao/Javet">Javet</a>
 *
 * @author wuxp
 * @date 2024-06-07 17:22
 **/
public final class JavaScriptExecutor {

    /**
     * js 异常中的错误前缀
     */
    private static final String JS_ERROR_PREFIX = "Error: ";

    /**
     * 需要执行的 js lib 代码
     */
    private static final List<String> JS_LIB_SCRIPTS = loadJsLibScripts();

    /**
     * 保存 esnext 语法特性的 polyfill js 代码
     */
    @VisibleForTesting
    static final AtomicReference<String> POLYFILL_JS = new AtomicReference<>();

    private JavaScriptExecutor() {
        throw new AssertionError();
    }

    /**
     * 使用闭包的方式执行 js 函数
     * example:
     * <code>
     * ((...args)=>{
     * // do something
     * })(...args)
     * </code>
     * 如果需要将 js 返回值转换为具体对象，请使用
     * {@link #executeFunctionUseClosure(String, ParameterizedTypeReference, Object...)}
     * {@link #executeFunctionUseClosure(String, Class, Object...)}
     *
     * @param functionCode js 函数代码
     * @param invoker      JsExecJavaInvoker 对象
     * @param args         js 函数参数
     * @return js 方法执行结果 Map、List 、字符串、数字、布尔值
     */
    public static <T> T executeFunctionSupportJavaUseClosure(String functionCode, JsExecJavaMethodInvoker invoker, Object... args) {
        try {
            return executJavaScript(functionCode, invoker, args);
        } catch (Exception e) {
            if (e instanceof JavetException exception) {
                // 转换 js 脚本执行异常的 message
                String message = e.getMessage();
                // 剪切掉错误消息的前缀
                message = message != null && message.startsWith(JS_ERROR_PREFIX) ? message.substring(JS_ERROR_PREFIX.length()) : message;
                throw new BaseException(DefaultExceptionCode.COMMON_ERROR, message, exception);
            }
            throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "execute js error", e);
        }
    }

    private static <T> T executJavaScript(String functionCode, JsExecJavaMethodInvoker invoker, Object[] args) throws JavetException {
        StringBuilder code = new StringBuilder();
        code.append('(').append(functionCode).append(')').append('(');
        for (Object o : args) {
            if (ClassUtils.isPrimitiveOrWrapper(o.getClass())) {
                code.append(o);
            } else {
                code.append(JSON.toJSONString(o));
            }
            code.append(",");
        }
        if (args.length > 0) {
            code.deleteCharAt(code.length() - 1);
        }
        code.append(')');
        // 避免多线程执行出错，@see ：https://github.com/eclipsesource/J2V8/issues/330
        try (V8Runtime v8 = V8Host.getV8Instance().createV8Runtime()) {
            executeScript(v8, POLYFILL_JS.get());
            // 加载 js lib
            for (String js : JS_LIB_SCRIPTS) {
                executeScript(v8, js);
            }
            registerJsExecJavaInvoker(v8, invoker);
            return v8.getExecutor(code.toString()).executeObject();
        }
    }

    /**
     * 使用闭包的方式执行 js 函数
     *
     * @param functionCode js 函数代码
     * @param args         js 函数参数
     * @return js 方法执行结果 Map、List 、字符串、数字、布尔值
     */
    public static <T> T executeFunctionUseClosure(String functionCode, Object... args) {
        return executeFunctionSupportJavaUseClosure(functionCode, null, args);
    }

    private static void executeScript(V8Runtime v8, String script) throws JavetException {
        if (StringUtils.hasText(script)) {
            v8.getExecutor(script).executeVoid();
        }
    }


    @Nullable
    public static <T> T executeFunctionUseClosure(String functionCode, ParameterizedTypeReference<T> type, Object... args) {
        return executeFunctionAndParseResult(functionCode, type.getType(), args);
    }

    @Nullable
    public static <T> T executeFunctionUseClosure(String functionCode, Class<T> clazz, Object... args) {
        return executeFunctionAndParseResult(functionCode, clazz, args);
    }

    @Nullable
    private static <T> T executeFunctionAndParseResult(String functionCode, Type type, Object... args) {
        Object result = executeFunctionUseClosure(functionCode, args);
        if (result == null) {
            // TypeUtils#cast 对于基本数据类型在 result 为 null 时会返回默认值，所以这里提前返回
            return null;
        }
        return TypeUtils.cast(result, type);
    }

    private static List<String> loadJsLibScripts() {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources("classpath*:jslib/*.min.js");
            List<String> result = new ArrayList<>();
            for (Resource resource : resources) {
                // 由于文件打包后在 jar 中，使用流的方式读取
                try (InputStream inputStream = resource.getInputStream()) {
                    result.add(StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8));
                }
            }
            return result;
        } catch (IOException e) {
            throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "load js lib failure", e);
        }
    }

    private static void registerJsExecJavaInvoker(V8Runtime v8, @Nullable JsExecJavaMethodInvoker invoker) throws JavetException {
        if (invoker != null) {
            for (JsExecJavaMethodInvoker.JavaMethodDescriptor descriptor : invoker.getJavaMethodDescriptors()) {
                Method method = ReflectionUtils.findMethod(invoker.getClass(), descriptor.getJavaMethodName(), descriptor.getParameterTypes());
                AssertUtils.notNull(method, () -> "not found java invoker method: " + descriptor.getJavaMethodName());
                JavetCallbackContext context = new JavetCallbackContext(descriptor.getJavaMethodName(), invoker, method);
                try (V8ValueObject v8ValueObject = v8.createV8ValueObject()) {
                    v8ValueObject.bindFunction(context);
                    v8.getGlobalObject().set(invoker.getJsObjectName(), v8ValueObject);
                }
            }

        }
    }

    public static void configure(String polyfillJs) {
        POLYFILL_JS.set(polyfillJs);
    }
}
