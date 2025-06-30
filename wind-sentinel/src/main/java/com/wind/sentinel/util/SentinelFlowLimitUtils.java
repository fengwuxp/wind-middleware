package com.wind.sentinel.util;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;
import com.wind.sentinel.SentinelResource;
import io.micrometer.core.instrument.Tags;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * sentinel 限流工具类
 *
 * @author wuxp
 * @date 2024-06-19 10:24
 **/
public final class SentinelFlowLimitUtils {

    private SentinelFlowLimitUtils() {
        throw new AssertionError();
    }

    /**
     * 资源流控
     *
     * @param resource 限流资源
     * @param func     执行的 方法
     */
    public static void limit(SentinelResource resource, Runnable func) {
        limit(resource, () -> {
            func.run();
            return null;
        });
    }

    /**
     * 资源流控
     *
     * @param resource 限流资源
     * @param func     执行的方法
     * @return func 执行结果
     */
    public static <T> T limit(SentinelResource resource, Supplier<T> func) {
        ContextUtil.enter(resource.getContextName(), resource.getOrigin());
        Entry entry = null;
        Throwable throwable = null;
        try {
            entry = SphU.entry(resource.getName(), resource.getResourceType(), resource.getEntryType(), getArgs(resource));
            return func.get();
        } catch (Throwable exception) {
            throwable = exception;
            if (exception instanceof BlockException) {
                throw new BaseException(DefaultExceptionCode.TO_MANY_REQUESTS, "request to many", exception);
            } else if (exception instanceof BaseException baseException) {
                throw baseException;
            } else {
                throw new BaseException(DefaultExceptionCode.COMMON_ERROR, exception.getMessage(), exception);
            }
        } finally {
            exit(entry, throwable);
        }
    }

    /**
     * 资源流控
     *
     * @param resource 限流资源
     * @return 流控后置处理函数
     */
    public static Consumer<Throwable> limit(SentinelResource resource) throws BlockException {
        ContextUtil.enter(resource.getContextName(), resource.getOrigin());
        Entry entry = SphU.entry(resource.getName(), resource.getResourceType(), resource.getEntryType(), getArgs(resource));
        return throwable -> exit(entry, throwable);
    }

    private static Object[] getArgs(SentinelResource resource) {
        List<Object> args = new ArrayList<>(resource.getArgs());
        if (!ObjectUtils.isEmpty(resource.getMetricsTags())) {
            args.add(Tags.of(resource.getMetricsTags()));
        }
        return args.toArray(new Object[0]);
    }

    private static void exit(Entry entry, Throwable throwable) {
        if (entry != null) {
            entry.exit();
        }
        if (throwable != null) {
            Tracer.traceEntry(throwable, entry);
        }
        ContextUtil.exit();
    }

}
