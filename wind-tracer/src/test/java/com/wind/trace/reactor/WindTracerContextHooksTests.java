package com.wind.trace.reactor;

import com.wind.trace.WindTracer;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * @author wuxp
 * @date 2025-05-29 15:19
 **/
class WindTracerContextHooksTests {

    static {
        WindTracerContextHooks.registerHook();
    }

    @Test
    void testTrace() {
        String testKey = RandomStringUtils.randomAlphabetic(12);
        String testValue = RandomStringUtils.randomAlphabetic(12);
        Mono<String> m1 = Mono.just("test")
                .flatMap(Mono::just)
                .contextWrite(context -> context.put(testKey, testValue))
                .doFinally(signalType -> {
                    Assertions.assertEquals(testValue, WindTracer.TRACER.getContextVariable(testKey));
                    WindTracer.TRACER.clear();
                });
        String testValue2 = RandomStringUtils.randomAlphabetic(12);
        Mono<String> m2 = Mono.just("test")
                .contextWrite(context -> context.put(testKey, testValue2))
                .doFinally(signalType -> {
                    Assertions.assertEquals(testValue2, WindTracer.TRACER.getContextVariable(testKey));
                    WindTracer.TRACER.clear();
                });
        Mono.when(m1, m2).block();
    }

    @Test
    void testTraceSwitchThread() {
        String testKey = RandomStringUtils.randomAlphabetic(12);
        String testValue = RandomStringUtils.randomAlphabetic(12);
        Mono.just("test")
                .flatMap(Mono::just)
                .contextWrite(context -> context.put(testKey, testValue))
                .publishOn(Schedulers.newSingle("test-"))
                .doFinally(signalType -> {
                    Assertions.assertEquals(testValue, WindTracer.TRACER.getContextVariable(testKey));
                    WindTracer.TRACER.clear();
                })
                .block();
    }
}
