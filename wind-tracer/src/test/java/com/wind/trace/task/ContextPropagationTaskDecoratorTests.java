package com.wind.trace.task;

import com.wind.trace.WindTraceContext;
import com.wind.trace.WindTracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证默认任务执行、新作用域扩展和旧租户钩子的兼容行为。
 *
 * @author wuxp
 * @since 2026-11-04
 */
class ContextPropagationTaskDecoratorTests {

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

    @AfterEach
    void clearTenant() {
        TENANT_ID.remove();
    }

    @Test
    void testDefaultDecoratorRunsTask() {
        AtomicInteger executions = new AtomicInteger();
        Runnable task = ContextPropagationTaskDecorator.of().decorate(executions::incrementAndGet);

        task.run();

        assertEquals(1, executions.get());
        assertTrue(WindTracer.TRACER.currentContext().isEmpty());
    }

    /**
     * 模拟 Capte 的旧子类只覆写三个钩子，租户可为空，执行后线程可继续复用。
     */
    @ParameterizedTest
    @NullSource
    @ValueSource(longs = 42L)
    void testLegacyHooksPropagateTenantAcrossThreads(Long tenantId) throws Exception {
        TENANT_ID.set(tenantId);
        AtomicInteger executions = new AtomicInteger();
        Runnable task = new LegacyTenantDecorator().decorate(() -> {
            assertEquals(tenantId, TENANT_ID.get());
            executions.incrementAndGet();
        });

        try (var executor = Executors.newSingleThreadExecutor()) {
            executor.submit(task).get(5, TimeUnit.SECONDS);
            assertNull(executor.submit(TENANT_ID::get).get(5, TimeUnit.SECONDS));
        }
        assertEquals(1, executions.get());
        assertEquals(tenantId, TENANT_ID.get());
    }

    @Test
    void testLegacyHooksClearTenantAfterTaskFailure() {
        TENANT_ID.set(42L);
        IllegalStateException failure = new IllegalStateException("任务执行失败");
        Runnable task = new LegacyTenantDecorator().decorate(() -> {
            assertEquals(42L, TENANT_ID.get());
            throw failure;
        });
        TENANT_ID.remove();

        assertSame(failure, assertThrows(IllegalStateException.class, task::run));
        assertNull(TENANT_ID.get());
        assertTrue(WindTracer.TRACER.currentContext().isEmpty());
    }

    /**
     * 提交方作用域结束后，目标线程仍能读取父 Trace 和业务变量。
     */
    @Test
    void testTraceContextSurvivesSubmittingScopeExit() throws Exception {
        WindTraceContext parent = WindTraceContext.root(Map.of("workspace", "workspace-a"));
        Runnable task = WindTracer.TRACER.callWithContext(parent, () -> {
            WindTraceContext submitting = WindTracer.TRACER.requireContext();
            return ContextPropagationTaskDecorator.of().decorate(() -> {
                assertEquals(submitting.traceId(), WindTracer.TRACER.requireTraceId());
                assertEquals(submitting.spanId(), WindTracer.TRACER.requireContext().parentSpanId());
                assertEquals("workspace-a", WindTracer.TRACER.getContextVariable("workspace"));
            });
        });

        try (var executor = Executors.newSingleThreadExecutor()) {
            executor.submit(task).get(5, TimeUnit.SECONDS);
            assertFalse(executor.submit(() -> WindTracer.TRACER.currentContext().isPresent()).get(5, TimeUnit.SECONDS));
        }
        assertTrue(WindTracer.TRACER.currentContext().isEmpty());
    }

    @Test
    void testRunWithSnapshotCanBoundTaskContext() {
        ContextPropagationTaskDecorator decorator = new ContextPropagationTaskDecorator() {
            @Override
            protected Map<String, Object> snapshotContextVariables() {
                return Map.of("tenantId", 42L);
            }

            @Override
            protected void runWithSnapshot(Map<String, Object> snapshotContext, Runnable action) {
                Long previous = TENANT_ID.get();
                try {
                    TENANT_ID.set((Long) snapshotContext.get("tenantId"));
                    action.run();
                } finally {
                    TENANT_ID.set(previous);
                }
            }
        };
        TENANT_ID.set(99L);
        IllegalStateException failure = new IllegalStateException("作用域内任务失败");
        Runnable task = decorator.decorate(() -> {
            assertEquals(42L, TENANT_ID.get());
            throw failure;
        });

        assertSame(failure, assertThrows(IllegalStateException.class, task::run));
        assertEquals(99L, TENANT_ID.get());
        assertTrue(WindTracer.TRACER.currentContext().isEmpty());
    }

    @SuppressWarnings("removal")
    private static final class LegacyTenantDecorator extends ContextPropagationTaskDecorator {

        @Override
        protected Map<String, Object> snapshotContextVariables() {
            return Collections.singletonMap("tenantId", TENANT_ID.get());
        }

        @Override
        protected void restoreContextVariables(Map<String, Object> contextVariables) {
            Long tenantId = (Long) contextVariables.get("tenantId");
            if (tenantId != null) {
                TENANT_ID.set(tenantId);
            }
        }

        @Override
        protected void clearContextVariables() {
            TENANT_ID.remove();
        }
    }
}
