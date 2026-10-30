package com.wind.common.spring;

import com.wind.common.exception.AssertUtils;
import com.wind.common.spring.event.SpringTransactionEvent;
import com.wind.common.util.ExecutorServiceUtils;
import com.wind.core.event.EventPersistence;
import com.wind.core.event.PersistentEvent;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Spring 事件发布工具，支持可选持久化和事务提交后投递。
 * {@link PersistentEvent} 在原线程保存，业务事务提交后才投递或提交到线程池。
 * 自动结果记录要求监听器同步完成并向外传播异常；提交后的写库与结果更新需要独立可提交的事务。
 * 未配置 {@link EventPersistence} 时使用 {@link EventPersistence#NONE}，继续投递但不提供持久化恢复能力。
 *
 * @author wuxp
 * @date 2024-06-21 09:54
 **/
@Slf4j
public final class SpringEventPublishUtils {

    private static final ExecutorService EXECUTOR = ExecutorServiceUtils.custom("spring-event-publish-", 1, 4, 256);

    private static final AtomicReference<ApplicationEventPublisher> PUBLISHER = new AtomicReference<>();

    /**
     * 上下文中已注册的事务回调事件 ids
     */
    private static final ThreadLocal<Set<String>> TRANSACTION_EVENT_IDS = ThreadLocal.withInitial(HashSet::new);

    private SpringEventPublishUtils() {
        throw new AssertionError();
    }

    /**
     * 同步发布事件，普通事件立即投递；持久化事件先保存，有事务时提交后投递。
     * 无事务时，监听器处理或结果保存的异常向调用方传播。
     *
     * @param event 事件对象
     */
    public static void publishEvent(Object event) {
        if (log.isDebugEnabled()) {
            log.debug("publish event = {}", event);
        }
        if (!(event instanceof PersistentEvent persistentEvent)) {
            publishDirectly(event);
            return;
        }
        EventPersistence persistence = SpringApplicationContextUtils.getBean(EventPersistence.class, () -> EventPersistence.NONE);
        persistence.save(persistentEvent);
        runAfterCommitOrImmediately(persistentEvent.getEventId(), () -> publishAndRecordResult(persistentEvent, persistence));
    }

    /**
     * 异步发布事件，在调用线程保存持久化事件并捕获追踪上下文，有事务时提交后入队。
     * 线程池拒绝任务时记录投递失败；无事务时拒绝异常向调用方传播。
     *
     * @param event 事件对象
     */
    public static void publishAsync(Object event) {
        if (!(event instanceof PersistentEvent persistentEvent)) {
            EXECUTOR.execute(() -> publishDirectly(event));
            return;
        }
        EventPersistence persistence = SpringApplicationContextUtils.getBean(EventPersistence.class, () -> EventPersistence.NONE);
        persistence.save(persistentEvent);
        Runnable task = () -> {
            try {
                publishAndRecordResult(persistentEvent, persistence);
            } catch (RuntimeException exception) {
                log.error("异步事件投递或结果记录失败，eventId = {}", persistentEvent.getEventId(), exception);
            }
        };
        runAfterCommitOrImmediately(persistentEvent.getEventId(), () -> submitAsync(persistentEvent.getEventId(), persistence, task));
    }

    /**
     * 在事务提交后发布事件，无事务时立即发布。
     * 持久化事件在当前线程保存；其他 {@link SpringTransactionEvent} 在事务内按标识去重，保留首次事件。
     *
     * @param event 事件对象
     */
    public static void publishWithTransactionCommitOrImmediately(@NonNull Object event) {
        if (TransactionSynchronizationManager.isSynchronizationActive() && TransactionSynchronizationManager.isActualTransactionActive()) {
            // 在事务中，通过注册回调的方式发送消息
            if (event instanceof SpringTransactionEvent ev) {
                String eventId = ev.getEventId();
                Set<String> eventIds = TRANSACTION_EVENT_IDS.get();
                if (eventIds.contains(eventId)) {
                    // 忽略重复事件
                    return;
                }
                eventIds.add(eventId);
            }

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        publishEvent(event);
                    } finally {
                        // Reset ThreadLocal
                        TRANSACTION_EVENT_IDS.remove();
                    }
                }
            });
        } else {
            publishEvent(event);
        }
    }

    /**
     * 直接向 Spring 发布事件，不保存事件、不记录结果，不主动切换线程或注册事务回调。
     * 监听器仍遵循 Spring 自身的执行语义。
     *
     * @param event 事件对象
     */
    public static void publishDirectly(@NonNull Object event) {
        if (log.isDebugEnabled()) {
            log.debug("directly publish event = {}", event);
        }
        ApplicationEventPublisher publisher = PUBLISHER.get();
        AssertUtils.notNull(publisher, "application event publisher no init");
        publisher.publishEvent(event);
    }

    /**
     * 仅安排已保存事件的执行时机，提交后失败不阻断其他事件的回调。
     */
    private static void runAfterCommitOrImmediately(String eventId, Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive() || !TransactionSynchronizationManager.isActualTransactionActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    action.run();
                } catch (RuntimeException exception) {
                    log.error("提交后事件投递或结果记录失败，eventId={}", eventId, exception);
                }
            }
        });
    }

    /**
     * 在线程池提交处处理拒绝异常，此时工作任务尚未开始执行。
     */
    private static void submitAsync(String eventId, EventPersistence persistence, Runnable task) {
        try {
            EXECUTOR.execute(task);
        } catch (RejectedExecutionException exception) {
            recordFailure(eventId, persistence, exception);
            throw exception;
        }
    }

    /**
     * 记录监听器的实际处理结果，成功标记写入失败不转为处理失败。
     */
    private static void publishAndRecordResult(PersistentEvent event, EventPersistence persistence) {
        String eventId = event.getEventId();
        try {
            publishDirectly(event);
        } catch (Throwable exception) {
            recordFailure(eventId, persistence, exception);
            throw exception;
        }
        persistence.markSucceeded(eventId);
    }

    /**
     * 保留最初的投递或处理异常，并附加失败记录时的异常。
     */
    private static void recordFailure(String eventId, EventPersistence persistence, Throwable failure) {
        try {
            persistence.markFailed(eventId, failure);
        } catch (Throwable recordException) {
            if (recordException != failure) {
                failure.addSuppressed(recordException);
            }
        }
    }

    static void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
        PUBLISHER.set(publisher);
    }

}
