package com.wind.trace.reactor;


import com.wind.trace.WindTracer;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

import java.util.Objects;

import static com.wind.common.WindConstants.CURRENT_TRACE_THEAD_ID_NAME;

/**
 * @author wuxp
 * @date 2025-05-29 15:07
 **/
@Slf4j
public final class WindTracerContextHooks {

    private static final String CONTEXT_SUBSCRIBER_VARIABLE_NAME = WindTracerContextHooks.class.getName();

    private static final String HOOK_KEY = WindTracerContextHooks.class.getName();

    private WindTracerContextHooks() {
        throw new AssertionError();
    }

    public static void registerHook() {
        Hooks.onEachOperator(HOOK_KEY, Operators.liftPublisher((scannable, subscriber) -> new TraceContextSubscriber<>(subscriber)));
        log.info("Registered WindTracer Hooks.onEachOperator");
    }

    public static void resetHook() {
        Hooks.resetOnEachOperator(HOOK_KEY);
        log.info("Reset WindTracer Hooks.onEachOperator");
    }


    static class TraceContextSubscriber<T> implements CoreSubscriber<T> {

        private final CoreSubscriber<? super T> actual;

        TraceContextSubscriber(CoreSubscriber<? super T> actual) {
            this.actual = actual;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            actual.onSubscribe(subscription);
        }

        @Override
        public void onNext(T t) {
            restoreContext();
            actual.onNext(t);
        }

        @Override
        public void onError(Throwable throwable) {
            restoreContext();
            actual.onError(throwable);
        }

        @Override
        public void onComplete() {
            restoreContext();
            actual.onComplete();
        }

        @Override
        public Context currentContext() {
            return actual.currentContext();
        }

        private void restoreContext() {
            Object variable = WindTracer.TRACER.getContextVariable(CONTEXT_SUBSCRIBER_VARIABLE_NAME);
            long threadId = Thread.currentThread().getId();
            if (!Objects.equals(variable, actual) || !Objects.equals(threadId, WindTracer.TRACER.getContextVariable(CURRENT_TRACE_THEAD_ID_NAME))) {
                // 上线文或线程发生了变化，通过 Context 恢复 contextVariables
                Context context = currentContext();
                context.forEach((key, value) -> WindTracer.TRACER.putVariable((String) key, value));
                WindTracer.TRACER.putVariable(CONTEXT_SUBSCRIBER_VARIABLE_NAME, actual);
                WindTracer.TRACER.putVariable(CURRENT_TRACE_THEAD_ID_NAME, threadId);
            }
        }
    }
}
