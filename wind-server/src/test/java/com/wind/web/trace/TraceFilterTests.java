package com.wind.web.trace;

import com.wind.common.WindConstants;
import com.wind.trace.WindTracer;
import com.wind.web.util.HttpTraceVariableUtils;
import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.RequestContextFilter;
import org.springframework.web.util.WebUtils;

import java.util.concurrent.atomic.AtomicInteger;

import static com.wind.common.WindConstants.WIND_TRANCE_ID_HEADER_NAME;
import static com.wind.common.WindHttpConstants.HTTP_REQUEST_CLIENT_ID_HEADER_NAME;

/**
 * @author wuxp
 * @date 2026-07-30
 **/
class TraceFilterTests {

    @Test
    void rebindsTraceContextWhenRequestIsRedispatchedAsError() throws Exception {
        String previousProfile = System.setProperty(WindConstants.SPRING_PROFILES_ACTIVE, WindConstants.PROD);
        try {
            String traceId = "test-error-dispatch-trace-id";
            String deviceId = "test-device-id";
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/examples");
            request.setDispatcherType(DispatcherType.REQUEST);
            request.addHeader(WIND_TRANCE_ID_HEADER_NAME, traceId);
            request.addHeader(HTTP_REQUEST_CLIENT_ID_HEADER_NAME, deviceId);
            MockHttpServletResponse response = new MockHttpServletResponse();
            TraceFilter traceFilter = new TraceFilter();
            RequestContextFilter requestContextFilter = new RequestContextFilter();
            AtomicInteger dispatchCount = new AtomicInteger();

            requestContextFilter.doFilter(request, response,
                    (servletRequest, servletResponse) -> traceFilter.doFilter(servletRequest, servletResponse, (req, resp) -> {
                        Assertions.assertEquals(traceId, WindTracer.TRACER.requireTraceId());
                        Assertions.assertEquals(deviceId, HttpTraceVariableUtils.getRequestDeviceId());
                        dispatchCount.incrementAndGet();
                    }));

            Assertions.assertEquals(1, dispatchCount.get());
            Assertions.assertTrue(WindTracer.TRACER.currentContext().isEmpty());

            request.setDispatcherType(DispatcherType.ERROR);
            request.setRequestURI("/error");
            request.setAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE, "/api/examples");
            requestContextFilter.doFilter(request, response,
                    (servletRequest, servletResponse) -> traceFilter.doFilter(servletRequest, servletResponse, (req, resp) -> {
                        Assertions.assertEquals(traceId, WindTracer.TRACER.requireTraceId());
                        Assertions.assertEquals(deviceId, HttpTraceVariableUtils.getRequestDeviceId());
                        dispatchCount.incrementAndGet();
                    }));

            Assertions.assertEquals(2, dispatchCount.get());
            Assertions.assertEquals(traceId, response.getHeader(WIND_TRANCE_ID_HEADER_NAME));
            Assertions.assertTrue(WindTracer.TRACER.currentContext().isEmpty());
        } finally {
            if (previousProfile == null) {
                System.clearProperty(WindConstants.SPRING_PROFILES_ACTIVE);
            } else {
                System.setProperty(WindConstants.SPRING_PROFILES_ACTIVE, previousProfile);
            }
        }
    }
}
