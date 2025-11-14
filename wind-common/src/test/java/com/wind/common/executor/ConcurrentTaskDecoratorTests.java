package com.wind.common.executor;

import com.wind.common.exception.BaseException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskDecorator;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author wuxp
 * @date 2025-11-14 11:12
 **/
class ConcurrentTaskDecoratorTests {

    private final int maxConcurrent = 2;

    private ExecutorService executor;

    @BeforeAll
    static void before() {
        RateLimitTaskDecorator.setThrowExceptionWithLimit(true);
    }

    @BeforeEach
    void setup() {
        executor = Executors.newFixedThreadPool(maxConcurrent);
    }

    @AfterEach
    void end() {
        executor.shutdown();
    }

    @Test
    void testWithConcurrencyTaskBasic() throws Exception {
        String resourceKey = randomResourceKey();
        Duration maxWait = Duration.ofMillis(10);
        TaskDecorator decorator = ConcurrentTaskDecorator.withConcurrency(resourceKey, maxWait, maxConcurrent);
        Runnable task = decorator.decorate(() -> {
            try {
                // 模拟任务执行
                Thread.sleep(20);
            } catch (InterruptedException ignored) {
            }
        });

        // 同时执行两个任务应该成功
        Future<?> f1 = executor.submit(task);
        Future<?> f2 = executor.submit(task);

        // 第三个任务立即执行应该抛异常
        Runnable task3 = decorator.decorate(() -> {
        });
        BaseException exception = Assertions.assertThrows(BaseException.class, task3::run);
        Assertions.assertEquals("resource key  = " + resourceKey + " concurrent limit exceeded", exception.getMessage());

        // 等待前两个任务完成，第三个任务通过 maxWait 获取许可
        Future<?> f3 = executor.submit(() -> {
            Runnable t = decorator.decorate(() -> {
            });
            t.run(); // 应该可以执行
        });

        f1.get();
        f2.get();
        f3.get();
    }

    @Test
    void testWithConcurrencyTaskMultipleThreads() throws Exception {
        String resourceKey = randomResourceKey();
        Duration maxWait = Duration.ofMillis(10);
        TaskDecorator decorator = ConcurrentTaskDecorator.withConcurrency(resourceKey, maxWait, maxConcurrent);
        int totalThreads = 5;
        CountDownLatch latch = new CountDownLatch(totalThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);
        for (int i = 0; i < totalThreads; i++) {
            executor.submit(() -> {
                Runnable task = decorator.decorate(() -> {
                    try {
                        Thread.sleep(20); // 模拟任务执行
                    } catch (InterruptedException ignored) {
                    }
                    successCount.incrementAndGet();
                });
                try {
                    task.run();
                } catch (RuntimeException e) {
                    failedCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();

        // 并发限制为 3，成功任务数应该 >= 3
        Assertions.assertTrue(successCount.get() >= maxConcurrent);
        Assertions.assertEquals(totalThreads, successCount.get() + failedCount.get());
    }

    @Test
    void testWithConcurrencyWithTokenByLimitRate() {
        String resourceKey = randomResourceKey();
        Duration maxWait = Duration.ofSeconds(1);
        int tokenPerSecond = 1;
        TaskDecorator composite = ConcurrentTaskDecorator.concurrentWithToken(resourceKey, maxWait, maxConcurrent, tokenPerSecond);
        AtomicInteger counter = new AtomicInteger(0);
        Runnable task = composite.decorate(() -> {
            try {
                // 模拟任务执行
                Thread.sleep(20);
                counter.incrementAndGet();
            } catch (InterruptedException ignored) {
            }
        });
        // 提交三个任务
        executor.execute(task);
        executor.execute(task);
        // 第三个任务可能因为 token 或并发限制失败
        BaseException exception = Assertions.assertThrows(BaseException.class, task::run);
        Assertions.assertEquals("resource key  = " + resourceKey + " rate limit exceeded", exception.getMessage());
    }

    @Test
    void testConcurrentWithTokenByWithConcurrencyLimit() {
        String resourceKey = randomResourceKey();
        Duration maxWait = Duration.ofMillis(10);
        int tokenPerSecond = 4;
        TaskDecorator composite = ConcurrentTaskDecorator.concurrentWithToken(resourceKey, maxWait, maxConcurrent, tokenPerSecond);
        AtomicInteger counter = new AtomicInteger(0);
        Runnable task = composite.decorate(() -> {
            try {
                // 模拟任务执行
                Thread.sleep(RandomUtils.secure().randomLong(40, 100));
                counter.incrementAndGet();
            } catch (InterruptedException ignored) {
            }
        });
        for (int i = 0; i < maxConcurrent; i++) {
            executor.execute(task);
        }
        try {
            // 模拟任务执行
            Thread.sleep(20);
        } catch (InterruptedException ignored) {
        }
        BaseException exception = Assertions.assertThrows(BaseException.class, task::run);
        Assertions.assertEquals("resource key  = " + resourceKey + " concurrent limit exceeded", exception.getMessage());
    }

    @Test
    void testConcurrentWithLeakyByWithConcurrencyLimit() {
        String resourceKey = randomResourceKey();
        Duration maxWait = Duration.ofMillis(10);
        int tokenPerSecond = 4;

        TaskDecorator composite = ConcurrentTaskDecorator.concurrentWithLeaky(resourceKey, maxWait, maxConcurrent, tokenPerSecond);
        AtomicInteger counter = new AtomicInteger(0);
        Runnable task = composite.decorate(() -> {
            try {
                // 模拟任务执行
                Thread.sleep(RandomUtils.secure().randomLong(40, 100));
                counter.incrementAndGet();
            } catch (InterruptedException ignored) {
            }
        });

        for (int i = 0; i < maxConcurrent; i++) {
            executor.execute(task);
        }
        try {
            // 模拟任务执行
            Thread.sleep(20);
        } catch (InterruptedException ignored) {
        }
        // 第三个任务可能因为 token 或并发限制失败
        BaseException exception = Assertions.assertThrows(BaseException.class, task::run);
        Assertions.assertEquals("resource key  = " + resourceKey + " concurrent limit exceeded", exception.getMessage());
    }

    private static String randomResourceKey() {
        return RandomStringUtils.secure().nextAlphanumeric(12);
    }
}
