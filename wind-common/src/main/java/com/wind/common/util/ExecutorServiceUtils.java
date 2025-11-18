package com.wind.common.util;

import com.wind.common.exception.AssertUtils;
import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;
import com.wind.trace.task.WindTaskDecorators;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 线程池创建工具
 *
 * @author wuxp
 * @date 2023-12-26 10:35
 **/
@Slf4j
public final class ExecutorServiceUtils {

    @VisibleForTesting
    static final String VIRTUAL_THREAD_MDC_KEY = "virtualThreadId";

    private static final int DEFAULT_WORK_QUEUE_SIZE = 256;

    private static final AtomicReference<TaskDecorator> TASK_DECORATOR = new AtomicReference<>(WindTaskDecorators.CONTEXT_PROPAGATION);

    private ExecutorServiceUtils() {
        throw new AssertionError();
    }

    /**
     * 创建虚拟线程的线程池（每个任务一个线程，适用于高并发阻塞型任务）
     *
     * @param threadName 线程名（JDK 21 无法自定义虚拟线程名，但可以手动 wrap）
     * @return ExecutorService 实例
     * @since JDK 21+
     */
    public static ExecutorService virtual(String threadName) {
        return named(threadName)
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
        return single(threadNamePrefix, DEFAULT_WORK_QUEUE_SIZE);
    }

    /**
     * 创建一个单线程的线程池（支持  {@link TaskDecorator} ）
     *
     * @param threadNamePrefix 线程池名称前缀
     * @param workQueueSize    等待队列大小
     * @return 线程池
     */
    public static ExecutorService single(String threadNamePrefix, int workQueueSize) {
        return fixed(threadNamePrefix, 1, workQueueSize);
    }

    /**
     * 创建一个固定大小的线程池（支持  {@link TaskDecorator} ）
     *
     * @param threadNamePrefix 线程池名称前缀
     * @param poolSize         线程数
     * @return 线程池
     */
    public static ExecutorService fixed(String threadNamePrefix, int poolSize) {
        return fixed(threadNamePrefix, poolSize, DEFAULT_WORK_QUEUE_SIZE);
    }

    /**
     * 创建一个固定大小的线程池（支持  {@link TaskDecorator} ）
     *
     * @param threadNamePrefix 线程池名称前缀
     * @param poolSize         线程数
     * @param workQueueSize    等待队列大小
     * @return 线程池
     */
    public static ExecutorService fixed(String threadNamePrefix, int poolSize, int workQueueSize) {
        return custom(threadNamePrefix, poolSize, poolSize, workQueueSize);
    }

    /**
     * 创建支持  {@link TaskDecorator} 的线程池 {@link #TASK_DECORATOR}
     *
     * @param threadNamePrefix 线程池名称前缀
     * @param corePoolSize     核心线程数
     * @param maximumPoolSize  最大线程数
     * @param workQueueSize    等待队列大小 默认使用 {@link ArrayBlockingQueue}
     * @return 线程池
     */
    public static ExecutorService custom(String threadNamePrefix, int corePoolSize, int maximumPoolSize, int workQueueSize) {
        return custom(threadNamePrefix, corePoolSize, maximumPoolSize, new ArrayBlockingQueue<>(workQueueSize));
    }

    /**
     * 创建支持  {@link TaskDecorator} 的线程池 {@link #TASK_DECORATOR}
     *
     * @param threadNamePrefix 线程池名称前缀
     * @param corePoolSize     核心线程数
     * @param maximumPoolSize  最大线程数
     * @param workQueue        等待队列
     * @return 线程池
     */
    public static ExecutorService custom(String threadNamePrefix, int corePoolSize, int maximumPoolSize, BlockingQueue<Runnable> workQueue) {
        return named(threadNamePrefix)
                .corePoolSize(corePoolSize)
                .maximumPoolSize(maximumPoolSize)
                .workQueue(workQueue)
                .build();
    }

    /**
     * 创建线程池构建器
     *
     * @param threadNamePrefix 线程名前缀
     * @return 线程池构建器
     */
    public static ExecutorBuilder named(@NotBlank String threadNamePrefix) {
        AssertUtils.hasText(threadNamePrefix, "argument threadNamePrefix must not empty");
        ExecutorBuilder result = new ExecutorBuilder();
        result.threadNamePrefix = threadNamePrefix;
        return result;
    }

    public static void configureTraceTaskDecorator(@NonNull TaskDecorator decorator) {
        AssertUtils.notNull(decorator, "argument decorator must not null");
        TASK_DECORATOR.set(decorator);
    }

    /**
     * 线程池构建器
     */
    public static class ExecutorBuilder {

        private String threadNamePrefix;

        private int corePoolSize = 1;

        private int maximumPoolSize = 1;

        /**
         * 默认线程存活时间 1 分钟
         */
        private Duration keepAlive = Duration.ofMinutes(1);

        private BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(DEFAULT_WORK_QUEUE_SIZE);

        private RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();

        private boolean useVirtualThreads = false;

        /**
         * 在 JVM 退出时关闭线程池
         */
        private boolean shutdownOnJvmExit = false;

        private ExecutorBuilder() {
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

        public ExecutorBuilder workQueueSize(int workQueue) {
            return workQueue(new ArrayBlockingQueue<>(workQueue));
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

        public ExecutorBuilder shutdownOnJvmExit() {
            this.shutdownOnJvmExit = true;
            return this;
        }

        /**
         * 创建线程池，不使用 {@link TaskDecorator} 装饰任务
         *
         * @return 线程池
         */
        public ExecutorService nativeBuild() {
            ExecutorService result = createRawExecutor();
            if (shutdownOnJvmExit) {
                registerShutdownHook(threadNamePrefix, result);
            }
            return result;
        }

        /**
         * 创建执行任务时自动通过 {@link #TASK_DECORATOR} 装饰任务的线程池
         *
         * @return 线程池
         */
        public ExecutorService build() {
            return buildWithDecorator(TASK_DECORATOR.get());
        }

        /**
         * 创建执行任务时自动通过 {@link TaskDecorator} 装饰任务的线程池
         * 如果 useVirtualThreads=true，会自动把 virtual-thread 的上下文 decorator 合并进去
         *
         * @param decorator 任务装饰器
         * @return 线程池
         */
        @NotNull
        public ExecutorService buildWithDecorator(@NotNull TaskDecorator decorator) {
            TaskDecorator finalDecorator = decorator;
            if (useVirtualThreads) {
                // 这里把 VirtualThreadMdcTaskDecorator 放在最外层，确保 MDC/ScopedValue 在执行期间可见
                finalDecorator = VirtualThreadMdcTaskDecorator.composite(threadNamePrefix, finalDecorator);
            }
            ExecutorService result = new DecoratingExecutorServiceWrapper(createRawExecutor(), finalDecorator);
            if (shutdownOnJvmExit) {
                registerShutdownHook(threadNamePrefix, result);
            }
            return result;
        }

        /**
         * 创建原生线程池执行器
         *
         * @return 线程池
         */
        @NotNull
        private ExecutorService createRawExecutor() {
            if (useVirtualThreads) {
                // 原生虚拟线程 executor（无任何额外包装）
                return Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name(threadNamePrefix, 0).factory());
            }
            return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAlive.getSeconds(), TimeUnit.SECONDS, workQueue,
                    new CustomizableThreadFactory(threadNamePrefix), rejectedExecutionHandler);
        }

        private static void registerShutdownHook(String threadNamePrefix, ExecutorService executor) {
            String name = threadNamePrefix.endsWith("-") ? threadNamePrefix.substring(0, threadNamePrefix.length() - 1) : threadNamePrefix;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.debug("jvm exit hook, shutdown executor = {}", name);
                gracefulShutdown(executor);
            }, "executor-shutdown-hook-" + name));
        }
    }

    /**
     * 优雅关闭线程池
     *
     * @param executor 线程池
     */
    public static void gracefulShutdown(ExecutorService executor) {
        if (executor.isShutdown() || executor.isTerminated()) {
            return;
        }
        try {
            executor.shutdown();
            if (!executor.awaitTermination(7500, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 支持任务自动b包装的 ExecutorService Wrapper
     **/
    private record DecoratingExecutorServiceWrapper(ExecutorService delegate, TaskDecorator taskDecorator) implements ExecutorService {

        @Override
        public void execute(@NonNull Runnable command) {
            delegate.execute(taskDecorator.decorate(command));
        }

        @Override
        @NonNull
        public <T> Future<T> submit(@NonNull Callable<T> task) {
            return delegate.submit(wrap(task));
        }

        @Override
        @NonNull
        public Future<?> submit(@NonNull Runnable task) {
            return delegate.submit(taskDecorator.decorate(task));
        }

        @Override
        @NonNull
        public <T> Future<T> submit(@NonNull Runnable task, T result) {
            return delegate.submit(taskDecorator.decorate(task), result);
        }

        @Override
        @NonNull
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return delegate.invokeAll(tasks.stream().map(this::wrap).toList());
        }

        @Override
        @NonNull
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, @NonNull TimeUnit unit)
                throws InterruptedException {
            return delegate.invokeAll(tasks.stream().map(this::wrap).toList(), timeout, unit);
        }

        @Override
        @NonNull
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
                throws InterruptedException, ExecutionException {
            return delegate.invokeAny(tasks.stream().map(this::wrap).toList());
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                               long timeout, @NonNull TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return delegate.invokeAny(tasks.stream().map(this::wrap).toList(), timeout, unit);
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        @Override
        @NonNull
        public List<Runnable> shutdownNow() {
            return delegate.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, @NonNull TimeUnit unit)
                throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }

        private <T> Callable<T> wrap(Callable<T> original) {
            final AtomicReference<T> resultRef = new AtomicReference<>();
            Runnable runnable = taskDecorator.decorate(() -> {
                try {
                    resultRef.set(original.call());
                } catch (Exception ex) {
                    throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "trace wrap execute task exception", ex);
                }
            });
            return () -> {
                runnable.run();
                return resultRef.get();
            };
        }
    }

    private record VirtualThreadMdcTaskDecorator(String prefix) implements TaskDecorator {

        private static final AtomicLong VT_ID = new AtomicLong(1);

        public static TaskDecorator of(String prefix) {
            return new VirtualThreadMdcTaskDecorator(prefix);
        }

        /**
         * 组合：把 virtual thread decorator 放在最外层
         */
        public static TaskDecorator composite(String prefix, TaskDecorator delegate) {
            TaskDecorator decorator = VirtualThreadMdcTaskDecorator.of(prefix);
            return runnable -> decorator.decorate(delegate.decorate(runnable));
        }

        @Override
        @NotNull
        public Runnable decorate(@NonNull Runnable runnable) {
            String id = prefix + "-" + VT_ID.getAndIncrement();
            return () -> {
                MDC.put(VIRTUAL_THREAD_MDC_KEY, id);
                try {
                    runnable.run();
                } finally {
                    MDC.remove(VIRTUAL_THREAD_MDC_KEY);
                }
            };
        }
    }

}