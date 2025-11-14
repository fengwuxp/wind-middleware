package com.wind.common.executor;

import com.wind.common.exception.BaseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.awaitility.Awaitility.await;

/**
 * @author wuxp
 * @date 2025-11-14 09:22
 **/
class RateLimitTaskDecoratorTests {


    @BeforeAll
    static void after() {
        RateLimitTaskDecorator.setThrowExceptionWithLimit(true);
    }

    @Test
    void testToken1Qps() {
        RateLimitTaskDecorator decorator = RateLimitTaskDecorator.token("test.token", 1, 1);
        for (int i = 0; i < 5; i++) {
            Runnable runnable = decorator.decorate(() -> System.out.println("执行任务"));
            if (i > 0) {
                BaseException exception = Assertions.assertThrows(BaseException.class, runnable::run);
                Assertions.assertEquals("resource key  = test.token rate limit exceeded", exception.getMessage());
            } else {
                runnable.run();
            }
        }
    }

    @Test
    void testLeaky1Qps() {
        RateLimitTaskDecorator decorator = RateLimitTaskDecorator.leaky("test.leaky", 1, Duration.ofMillis(1));
        for (int i = 0; i < 5; i++) {
            Runnable runnable = decorator.decorate(() -> System.out.println("执行任务"));
            if (i > 0) {
                BaseException exception = Assertions.assertThrows(BaseException.class, runnable::run);
                Assertions.assertEquals("resource key  = test.leaky rate limit exceeded", exception.getMessage());
            } else {
                // 等待下一个令牌补充完成
                await().atMost(Duration.ofSeconds(2))
                        // 延迟 1 秒开始执行
                        .pollDelay(Duration.ofSeconds(1))
                        .until(() -> {
                            runnable.run();
                            return true;
                        });
            }
        }
    }
}
