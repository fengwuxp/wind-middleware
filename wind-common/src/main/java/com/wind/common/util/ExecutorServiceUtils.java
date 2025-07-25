package com.wind.common.util;

import com.wind.common.exception.AssertUtils;
import com.wind.common.exception.BaseException;
import com.wind.common.exception.DefaultExceptionCode;
import com.wind.trace.thread.TraceContextTask;
import jakarta.validation.constraints.NotBlank;
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
import java.util.concurrent.atomic.AtomicReference;

/**
 * 线程池创建工具
 *
 * @author wuxp
 * @date 2023-12-26 10:35
 **/
public final class ExecutorServiceUtils {

    private static final AtomicReference<TaskDecorator> TASK_DECORATOR = new AtomicReference<>(TraceContextTask.of());

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

    /**
     * 创建线程池
     *
     * @param threadNamePrefix 线程池名称前缀
     * @param corePoolSize     核心线程数
     * @param maximumPoolSize  最大线程数
     * @param workQueue        等待队列
     * @return 线程池
     */
    public static ExecutorService newExecutor(String threadNamePrefix, int corePoolSize, int maximumPoolSize, BlockingQueue<Runnable> workQueue) {
        return named(threadNamePrefix)
                .corePoolSize(corePoolSize)
                .maximumPoolSize(maximumPoolSize)
                .workQueue(workQueue)
                .build();
    }

    /**
     * 创建虚拟线程的线程池（每个任务一个线程，适用于高并发阻塞型任务）
     *
     * @param threadNamePrefix 线程名前缀（JDK 21 无法自定义虚拟线程名，但可以手动 wrap）
     * @return ExecutorService 线程池
     * @since JDK 21+
     */
    public static ExecutorService virtualDecorated(String threadNamePrefix) {
        return named(threadNamePrefix)
                .useVirtualThreads()
                .buildDecorated();
    }

    /**
     * 创建一个单线程，等待队列为 128 的线程池
     *
     * @param threadNamePrefix 线程池名称前缀
     * @return 线程池
     */
    public static ExecutorService withDecorated(String threadNamePrefix) {
        return withDecorated(threadNamePrefix, 128);
    }

    /**
     * 创建一个单线程的线程池
     *
     * @param threadNamePrefix 线程池名称前缀
     * @param workQueueSize    等待队列大小
     * @return 线程池
     */
    public static ExecutorService withDecorated(String threadNamePrefix, int workQueueSize) {
        return withDecorated(threadNamePrefix, 1, 1, workQueueSize);
    }

    public static ExecutorService withDecorated(String threadNamePrefix, int corePoolSize, int maximumPoolSize, int workQueueSize) {
        return withDecorated(threadNamePrefix, corePoolSize, maximumPoolSize, new ArrayBlockingQueue<>(workQueueSize));
    }

    private static ExecutorService withDecorated(String threadNamePrefix, int corePoolSize, int maximumPoolSize,
                                                 BlockingQueue<Runnable> workQueue) {
        return named(threadNamePrefix)
                .corePoolSize(corePoolSize)
                .maximumPoolSize(maximumPoolSize)
                .workQueue(workQueue)
                .buildDecorated();
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

        private BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(128);

        private RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();

        private boolean useVirtualThreads = false;

        private ExecutorBuilder() {
        }

        @Deprecated
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
                ExecutorService executorService = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name(threadNamePrefix, 0).factory());
                return new DecoratingExecutorServiceWrapper(executorService, VirtualThreadMdcTaskDecorator.composite(threadNamePrefix,
                        TASK_DECORATOR.get()));
            }
            return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAlive.getSeconds(), TimeUnit.SECONDS, workQueue,
                    new CustomizableThreadFactory(threadNamePrefix), rejectedExecutionHandler);
        }

        /**
         * 创建执行任务时自动通过 {@link #TASK_DECORATOR} 装饰任务的线程池
         *
         * @return 线程池
         */
        public ExecutorService buildDecorated() {
            return buildWithDecorator(TASK_DECORATOR.get());
        }

        /**
         * 创建执行任务时自动通过 {@link TaskDecorator} 装饰任务的线程池
         *
         * @param decorator 任务装饰器
         * @return 线程池
         */
        public ExecutorService buildWithDecorator(TaskDecorator decorator) {
            if (useVirtualThreads) {
                // 虚拟线程
                decorator = VirtualThreadMdcTaskDecorator.composite(threadNamePrefix, decorator);
            }
            return new DecoratingExecutorServiceWrapper(build(), decorator);
        }
    }

    /**
     * 支持任务自动b包装的 ExecutorService Wrapper
     **/
    private static class DecoratingExecutorServiceWrapper implements ExecutorService {

        private final ExecutorService delegate;

        private final TaskDecorator taskDecorator;

        private DecoratingExecutorServiceWrapper(ExecutorService delegate, TaskDecorator traceDecorator) {
            this.delegate = delegate;
            this.taskDecorator = traceDecorator;
        }

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
            return () -> {
                final AtomicReference<T> resultRef = new AtomicReference<>();
                Runnable decorated = taskDecorator.decorate(() -> {
                    try {
                        resultRef.set(original.call());
                    } catch (Exception ex) {
                        throw new BaseException(DefaultExceptionCode.COMMON_ERROR, "trace wrap execute task exception", ex);
                    }
                });
                decorated.run();
                return resultRef.get();
            };
        }
    }

    private record VirtualThreadMdcTaskDecorator(String virtualThreadName) implements TaskDecorator {

        private static final String VIRTUAL_THREAD_NAME = "virtualThreadName";

        public static TaskDecorator composite(String virtualThreadName, TaskDecorator decorator) {
            VirtualThreadMdcTaskDecorator virtual = new VirtualThreadMdcTaskDecorator(virtualThreadName);
            return runnable -> virtual.decorate(decorator.decorate(runnable));
        }

        @Override
        @NonNull
        public Runnable decorate(@NonNull Runnable runnable) {
            // 放入 MDC，线程上下文中 TODO 待优化
            MDC.put(VIRTUAL_THREAD_NAME, virtualThreadName);
            return () -> {
                try {
                    runnable.run();
                } finally {
                    MDC.remove(VIRTUAL_THREAD_NAME);
                }
            };
        }
    }
}