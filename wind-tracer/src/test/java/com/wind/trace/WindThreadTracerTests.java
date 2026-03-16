package com.wind.trace;

import com.wind.trace.task.ContextPropagationTaskDecorator;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.wind.common.WindConstants.LOCALHOST_IP_V4;
import static com.wind.common.WindConstants.TRACE_ID_NAME;

/**
 * @author wuxp
 * @date 2025-02-25 10:34
 **/
class WindThreadTracerTests {

    @Test
    void testGetTranceId() {
        Assertions.assertNotNull(WindTracer.TRACER.getContextVariables());
        String traceId = WindTracer.TRACER.getTraceId();
        Assertions.assertNotNull(traceId);
        WindTracer.TRACER.clear();
        WindTracer.TRACER.trace();
        Assertions.assertNotEquals(traceId, WindTracer.TRACER.getTraceId());
        Assertions.assertNotNull(WindTracer.TRACER.getContextVariable(LOCALHOST_IP_V4));
    }

    @Test
    void testTrace() {
        String traceId = "test";
        WindTracer.TRACER.trace(traceId);
        Assertions.assertEquals(traceId, WindTracer.TRACER.getTraceId());
        WindTracer.TRACER.trace("2");
        Assertions.assertEquals("2", WindTracer.TRACER.getTraceId());
    }

    @Test
    void testTraceVariables() {
        HashMap<String, Object> contextVariables = new HashMap<>();
        contextVariables.put("key1", "1");
        contextVariables.put("key2", 2);
        contextVariables.put("key3", null);
        WindTracer.TRACER.trace("test001", contextVariables);
        Assertions.assertEquals("1", WindTracer.TRACER.getContextVariable("key1"));
        Assertions.assertEquals(2, (Integer) WindTracer.TRACER.getContextVariable("key2"));
        WindTracer.TRACER.putVariable("key3", false);
        Assertions.assertEquals(false, Objects.requireNonNull(WindTracer.TRACER.getContextVariable("key3")));
    }

    @Test
    void testTraceClear() {
        String traceId = "test";
        WindTracer.TRACER.trace(traceId);
        Assertions.assertEquals(traceId, WindTracer.TRACER.getTraceId());
        WindTracer.TRACER.clear();
        Assertions.assertNotEquals(traceId, WindTracer.TRACER.getTraceId());
        WindTracer.TRACER.clear();
    }

    @Test
    void testTraceNewThreadCopy() throws Exception {
        Map<String, Object> contextVariables = new HashMap<>();
        contextVariables.put("a", "test");
        WindTracer.TRACER.trace(RandomStringUtils.secure().nextAlphabetic(32), contextVariables);
        String traceId = WindTracer.TRACER.getTraceId();
        Assertions.assertEquals(traceId, MDC.get(TRACE_ID_NAME));
        CountDownLatch downLatch = new CountDownLatch(1);
        try (ExecutorService executorService = Executors.newSingleThreadExecutor()) {
            executorService.execute(ContextPropagationTaskDecorator.of().decorate(() -> {
                try {
                    String actual = WindTracer.TRACER.getTraceId();
                    Assertions.assertEquals(traceId, actual);
                    Assertions.assertEquals(traceId, MDC.get(TRACE_ID_NAME));
                    Assertions.assertEquals("test", WindTracer.TRACER.requireContextVariable("a"));
                    Assertions.assertEquals("test", MDC.get("a"));
                } finally {
                    downLatch.countDown();
                }
            }));
            downLatch.await();
            Assertions.assertEquals(traceId, WindTracer.TRACER.getTraceId());
        }
    }

    @Test
    void testTraceNewThreadNoCopy() throws Exception {
        String traceId = WindTracer.TRACER.getTraceId();
        Assertions.assertEquals(traceId, MDC.get(TRACE_ID_NAME));
        CountDownLatch downLatch = new CountDownLatch(1);
        try (ExecutorService executorService = Executors.newSingleThreadExecutor()) {
            executorService.execute(() -> {
                try {
                    WindTracer.TRACER.trace();
                    Assertions.assertNotEquals(traceId, WindTracer.TRACER.getTraceId());
                    Assertions.assertNotEquals(traceId, MDC.get(TRACE_ID_NAME));
                } finally {
                    WindTracer.TRACER.clear();
                    downLatch.countDown();
                }
            });
            downLatch.await();
            Assertions.assertEquals(traceId, WindTracer.TRACER.getTraceId());
        }
    }

    @Test
    void testRemoveVariable() {
        Map<String, Object> contextVariables = new HashMap<>();
        contextVariables.put("a", "test");
        WindTraceContext parent = WindTraceContext.tryTrace(null);
        parent.putVariables(contextVariables);
        WindTracer.TRACER.runWithContext(parent, () -> {
            Assertions.assertEquals("test", WindTracer.TRACER.getContextVariable("a"));
            WindTracer.TRACER.removeVariable("a");
            Assertions.assertNull(WindTracer.TRACER.getContextVariable("a"));
        });

    }

    @Test
    void testRequireTraceId() {
        WindTracer.TRACER.run(() -> {
            String traceId = WindTracer.TRACER.requireTraceId();
            Assertions.assertNotNull(traceId);
        });
    }

    @Test
    void testMultipleTraces() {
        WindTracer.TRACER.run(() -> {
            String traceId = WindTracer.TRACER.requireTraceId();
            WindTracer.TRACER.run(() -> {
                Assertions.assertEquals(traceId, WindTracer.TRACER.requireTraceId());
            });
        });
    }

    @Test
    void testRunWithNewContext() {
        WindTracer.TRACER.run(() -> {
            String traceId = WindTracer.TRACER.requireTraceId();
            WindTracer.TRACER.runWithNewContext(() -> {
                Assertions.assertNotEquals(traceId, WindTracer.TRACER.requireTraceId());
            });
        });
    }

    @Test
    void testPutVariable() {
        String testKey = RandomStringUtils.secure().nextAlphanumeric(12);
        WindTracer.TRACER.run(() -> {
            String val = RandomStringUtils.secure().nextAlphanumeric(32);
            WindTracer.TRACER.putVariable(testKey, val);
            WindTracer.TRACER.run(() -> {
                Assertions.assertEquals(val, WindTracer.TRACER.getContextVariable(testKey));
                WindTracer.TRACER.removeVariable(testKey);
                Assertions.assertNull(WindTracer.TRACER.getContextVariable(testKey));
            });
            Assertions.assertEquals(val, WindTracer.TRACER.getContextVariable(testKey));
            WindTracer.TRACER.removeVariable(testKey);
            Assertions.assertNull(WindTracer.TRACER.getContextVariable(testKey));
        });
    }

}
