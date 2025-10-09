package com.wind.trace.thread;

import com.wind.trace.WindTracer;
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

import static com.wind.common.WindConstants.LOCAL_HOST_IP_V4;
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
        Assertions.assertNotNull(WindTracer.TRACER.getContextVariable(LOCAL_HOST_IP_V4));
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
            executorService.execute(TraceContextTask.of().decorate(() -> {
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
        WindTracer.TRACER.trace(null, contextVariables);
        Assertions.assertEquals("test", WindTracer.TRACER.getContextVariable("a"));
        WindTracer.TRACER.removeVariable("a");
        Assertions.assertNull(WindTracer.TRACER.getContextVariable("a"));
    }
}
