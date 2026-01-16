package com.wind.common.spring;

import com.wind.common.exception.AssertUtils;
import com.wind.common.spring.event.SpringTransactionEvent;
import com.wind.common.util.ExecutorServiceUtils;
import com.wind.trace.task.ContextPropagationTaskDecorator;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

/**
 * spring event {@link org.springframework.context.ApplicationEvent} publish utils
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
     * 发送 spring event
     *
     * @param event 事件对象
     */
    public static void publishEvent(Object event) {
        log.debug("publish event = {}", event);
        ApplicationEventPublisher publisher = PUBLISHER.get();
        AssertUtils.notNull(publisher, "application event publisher no init");
        publisher.publishEvent(event);
    }

    /**
     * 异步发送事件，会传递线程上线文到新的线程
     *
     * @param event 事件对象
     */
    public static void publishAsync(Object event) {
        EXECUTOR.execute(ContextPropagationTaskDecorator.of().decorate(() -> publishEvent(event)));
    }


    @Deprecated(forRemoval = true)
    public static void publishEventIfInTransaction(@NonNull Object event) {
        publishWithTransactionCommitOrImmediately(event);
    }

    /**
     * 如果在事务内，事件推迟到事务提交后发送。如果 {@param event} 实现了 {@link SpringTransactionEvent} 接口，
     * 无论事务中发送了多少次相同的{@link SpringTransactionEvent#getEventId()}事件，在事务结束后只会发送最后一次事件。
     * 不在事物内，则立即发送事件
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

    static void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
        PUBLISHER.set(publisher);
    }

}
