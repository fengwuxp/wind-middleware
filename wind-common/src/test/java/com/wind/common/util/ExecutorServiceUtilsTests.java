package com.wind.common.util;

import com.wind.common.WindConstants;
import com.wind.trace.WindTracer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static com.wind.common.util.ExecutorServiceUtils.VIRTUAL_THREAD_MDC_KEY;

/**
 * @author wuxp
 * @date 2025-06-30 09:16
 **/
class ExecutorServiceUtilsTests {

    @Test
    void testVirtual() throws Exception {
        try (ExecutorService executor = ExecutorServiceUtils.virtual("example")) {
            Future<?> future = executor.submit((Callable<Object>) () -> {
                Assertions.assertEquals("example-0", MDC.get(VIRTUAL_THREAD_MDC_KEY));
                return 1;
            });
            Assertions.assertEquals(1, future.get());
            Assertions.assertNull(MDC.get(VIRTUAL_THREAD_MDC_KEY));
        }
    }

    @Test
    void testEnableTrace() {
        WindTracer.TRACER.run(() -> {
            final String traceId = WindTracer.TRACER.requireTraceId();
            ExecutorService traceExecutor = ExecutorServiceUtils.named("test-trace-").build();
            Future<?> f1 = traceExecutor.submit(() -> {
                Assertions.assertEquals(traceId, WindTracer.TRACER.requireTraceId());
            });
            Future<?> f2 = ExecutorServiceUtils.named("test-").nativeBuild().submit(() -> {
                Assertions.assertEquals(WindConstants.UNKNOWN, WindTracer.TRACER.currentTraceId().orElseGet(() -> WindConstants.UNKNOWN));
            });
            try {
                for (Future<?> future : Arrays.asList(f1, f2)) {
                    future.get();
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });

    }

    @Test
    @Disabled
    void testAutoShutdownOnJvmExit() throws Exception {
        ExecutorService executorService = ExecutorServiceUtils.named("test-").shutdownOnJvmExit().build();
        Future<?> future = executorService.submit(() -> {
            try {
                Thread.sleep(8 * 1000);
            } catch (InterruptedException e) {
                System.err.print("execute interrupted");
            }
        });
        CompletableFuture.runAsync(() -> System.exit(-1));
        future.get();
    }

}
