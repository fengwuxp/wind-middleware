package com.wind.trace;

import com.wind.common.WindConstants;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author wuxp
 * @date 2025-02-25 10:34
 **/
class WindThreadTracerTests {

    @Test
    void testRemoveVariable() {
        Map<String, Object> contextVariables = new HashMap<>();
        contextVariables.put("a", "test");
        WindTraceContext parent = WindTraceContext.withTrace(null);
        parent.writeView().putVariables(contextVariables);
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
    void testSpandId() {
        WindTracer.TRACER.run(() -> {
            String spanId = WindTracer.TRACER.requireContext().spanId();
            Assertions.assertEquals(spanId, WindTracer.TRACER.getContextVariable(WindConstants.SPAND_ID_NAME));
            Assertions.assertNull(WindTracer.TRACER.getContextVariable(WindConstants.PARENT_SPAND_ID_NAME));
        });
    }

    @Test
    void testParentSpanId() {
        WindTracer.TRACER.run(() -> {
            String spanId = WindTracer.TRACER.requireContext().spanId();
            WindTracer.TRACER.runWithContext(WindTracer.TRACER.requireContext(), () -> {
                Assertions.assertEquals(spanId, WindTracer.TRACER.requireContext().parentSpanId());
            });
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
