package com.wind.common.util;

import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池创建工具
 *
 * @author wuxp
 * @date 2023-12-26 10:35
 **/
public final class ExecutorServiceUtils {

    private ExecutorServiceUtils() {
        throw new AssertionError();
    }

    /**
     * 创建虚拟线程的线程池（每个任务一个线程，适用于高并发阻塞型任务）
     *
     * @param threadNamePrefix 线程名前缀（JDK 21 无法自定义虚拟线程名，但可以手动 wrap）
     * @return ExecutorService 实例
     * @since JDK 21+
     */
    public static ExecutorService virtual(String threadNamePrefix) {
        return ExecutorBuilder.withThreadName(threadNamePrefix)
                .useVirtualThreads()
                .build();
    }

    /**
     * 创建一个单线程，等待队列为 128 的线程池
     *
     * @param threadNamePrefix 线程池名称前缀
     * @return 线程池
     */
    public static ExecutorService single(String threadNamePrefix) {
        return single(threadNamePrefix, 128);
    }

    /**
     * 创建一个单线程的线程池
     *
     * @param threadNamePrefix 线程池名称前缀
     * @param workQueueSize    等待队列大小
     * @return 线程池
     */
    public static ExecutorService single(String threadNamePrefix, int workQueueSize) {
        return newExecutor(threadNamePrefix, 1, 1, workQueueSize);
    }

    /**
     * 创建线程池
     *
     * @param threadNamePrefix 线程池名称前缀
     * @param corePoolSize     核心线程数
     * @param maximumPoolSize  最大线程数
     * @param workQueueSize    等待队列大小 默认使用 {@link ArrayBlockingQueue}
     * @return 线程池
     */
    public static ExecutorService newExecutor(String threadNamePrefix, int corePoolSize, int maximumPoolSize, int workQueueSize) {
        return newExecutor(threadNamePrefix, corePoolSize, maximumPoolSize, new ArrayBlockingQueue<>(workQueueSize));
    }

    private static ExecutorService newExecutor(String threadNamePrefix, int corePoolSize, int maximumPoolSize, BlockingQueue<Runnable> workQueue) {
        return ExecutorBuilder.withThreadName(threadNamePrefix)
                .corePoolSize(corePoolSize)
                .maximumPoolSize(maximumPoolSize)
                .workQueue(workQueue)
                .build();
    }

    public static class ExecutorBuilder {

        private String threadNamePrefix;

        private int corePoolSize = 1;

        private int maximumPoolSize = 1;

        /**
         * 默认线程存活时间 1 分钟
         */
        private Duration keepAlive = Duration.ofMinutes(1);

        private BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(128);

        private RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();

        private boolean useVirtualThreads = false;

        private ExecutorBuilder() {
        }

        public static ExecutorBuilder withThreadName(String threadNamePrefix) {
            ExecutorBuilder result = new ExecutorBuilder();
            result.threadNamePrefix = threadNamePrefix;
            return result;
        }

        public ExecutorBuilder corePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
            return this;
        }

        public ExecutorBuilder maximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
            return this;
        }

        public ExecutorBuilder keepAlive(Duration keepAlive) {
            this.keepAlive = keepAlive;
            return this;
        }

        public ExecutorBuilder workQueue(BlockingQueue<Runnable> workQueue) {
            this.workQueue = workQueue;
            return this;
        }

        public ExecutorBuilder rejectedExecutionHandler(RejectedExecutionHandler rejectedExecutionHandler) {
            this.rejectedExecutionHandler = rejectedExecutionHandler;
            return this;
        }

        public ExecutorBuilder useVirtualThreads() {
            this.useVirtualThreads = true;
            return this;
        }

        public ExecutorService build() {
            if (useVirtualThreads) {
                return Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name(threadNamePrefix, 0).factory());
            }

            return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAlive.getSeconds(), TimeUnit.SECONDS, workQueue,
                    new CustomizableThreadFactory(threadNamePrefix), rejectedExecutionHandler);
        }
    }
}
