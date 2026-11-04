package com.wind.trace.task;

import com.wind.common.exception.AssertUtils;
import com.wind.trace.WindTraceContext;
import com.wind.trace.WindTracer;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.task.TaskDecorator;

import java.util.Collections;
import java.util.Map;

/**
 * 用于在线程切换时，将 trace 上下文信息复制到新的线程中
 *
 * @author wuxp
 * @date 2023-12-29 09:55
 **/
@Slf4j
public abstract class ContextPropagationTaskDecorator implements TaskDecorator {

    private final boolean printExceptionLog;

    protected ContextPropagationTaskDecorator(boolean printExceptionLog) {
        this.printExceptionLog = printExceptionLog;
    }

    protected ContextPropagationTaskDecorator() {
        // 默认不输出异常日志，由任务处理者输出
        this(false);
    }

    @Override
    @NonNull
    public Runnable decorate(@NonNull Runnable task) {
        AssertUtils.notNull(task, "argument task must not null");
        // 获取当前 Trace Context
        WindTraceContext parentContext = WindTracer.TRACER.currentContext().orElseGet(WindTraceContext::root);
        // 快照业务 Context
        Map<String, Object> snapshotContext = snapshotContextVariables();
        Runnable execution = () -> {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("task decorate, parent context = {}", parentContext);
                }
                restoreContextVariables(snapshotContext);
                // 恢复业务上下文，并执行任务
                runWithSnapshot(snapshotContext, task);
            } catch (Throwable throwable) {
                if (printExceptionLog) {
                    log.error("execute task exception, message = {}", throwable.getMessage(), throwable);
                }
                throw throwable;
            } finally {
                if (log.isDebugEnabled()) {
                    log.debug("task decorate, clear context contextVariables");
                }
                clearContextVariables();
            }
        };
        return WindTracer.wrap(parentContext, execution);
    }

    /**
     * 快照当前线程的上下文
     *
     * @return 上下文变量
     */
    protected Map<String, Object> snapshotContextVariables() {
        return Collections.emptyMap();
    }

    /**
     * 使用已快照的上下文执行任务。
     * 默认直接执行，兼容仅覆写恢复和清理钩子的子类。
     * 覆写实现应将上下文绑定限定在任务执行期间，并在正常返回或异常退出时恢复原上下文。
     *
     * @param snapshotContext 已捕获的上下文快照
     * @param action          在该上下文中执行的任务
     */
    protected void runWithSnapshot(Map<String, Object> snapshotContext, Runnable action) {
        action.run();
    }

    /**
     * 线程切换，恢复上下文 ，traceId 也在 contextVariables 中
     * 保留旧实现的恢复行为，新作用域实现可覆写 {@link #runWithSnapshot(Map, Runnable)}。
     *
     * @param contextVariables copy 的上下文
     */
    @Deprecated(forRemoval = true)
    protected void restoreContextVariables(Map<String, Object> contextVariables) {
        if (log.isDebugEnabled()) {
            log.debug("task decorate, restore contextVariables = {}", contextVariables);
        }
    }

    /**
     * 清除线程上下文
     */
    @Deprecated(forRemoval = true)
    protected void clearContextVariables() {
    }

    public static TaskDecorator of() {
        return new ContextPropagationTaskDecorator() {
        };
    }
}
