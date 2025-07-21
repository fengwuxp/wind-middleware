package com.wind.common.util;

import com.wind.trace.WindTracer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * @author wuxp
 * @date 2025-06-30 09:16
 **/
class ExecutorServiceUtilsTests {

    @Test
    void testVirtual() throws Exception {
        try (ExecutorService executor = ExecutorServiceUtils.virtual("example-")) {
            Future<?> future = executor.submit((Callable<Object>) () -> 1);
            Assertions.assertEquals(1, future.get());
        }
    }


    @Test
    void testEnableTrace() throws Exception {
        WindTracer.TRACER.trace();
        final String traceId = WindTracer.TRACER.getTraceId();
        ExecutorService traceExecutor = ExecutorServiceUtils.named("test-trace-").buildDecorated();
        Future<?> f1 = traceExecutor.submit(() -> {
            Assertions.assertEquals(traceId, WindTracer.TRACER.getTraceId());
        });
        Future<?> f2 = ExecutorServiceUtils.single("test-").submit(() -> {
            Assertions.assertNotEquals(traceId, WindTracer.TRACER.getTraceId());
        });
        for (Future<?> future : Arrays.asList(f1, f2)) {
            future.get();
        }
    }
}
