package com.wind.trace;

import com.wind.common.WindConstants;
import org.slf4j.MDC;

/**
 * @author wuxp
 * @date 2026-03-17 11:11
 **/
final class TraceMdcBridge {

    public TraceMdcBridge() {
        throw new AssertionError();
    }

    public static void rebind(WindTraceContext context) {
        clear();
        MDC.put(WindConstants.TRACE_ID_NAME, context.traceId());
        MDC.put(WindConstants.SPAND_ID_NAME, context.spanId());
        if (context.parentSpanId() != null) {
            MDC.put(WindConstants.PARENT_SPAND_ID_NAME, context.parentSpanId());
        }
        context.getContextVariables().forEach((key, value) -> MDC.put(key, String.valueOf(value)));
    }

    public static void clear() {
        MDC.clear();
    }
}
