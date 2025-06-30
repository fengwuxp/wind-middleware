package com.wind.common.spring;

import com.wind.common.exception.AssertUtils;
import com.wind.common.spring.event.SpringTransactionEvent;
import com.wind.common.util.ExecutorServiceUtils;
import com.wind.trace.thread.TraceContextTask;
import lombok.extern.slf4j.Slf4j;
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

    // TODO 使用 Dynamic-TP 监控执行
    private static final ExecutorService EXECUTOR = ExecutorServiceUtils.newExecutor("Spring-Event-", 1, 2, 256);

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
        EXECUTOR.execute(TraceContextTask.of().decorate(() -> publishEvent(event)));
    }

    /**
     * 如果在事务内，事件推迟到事务提交后发送。如果 {@param event} 实现了 {@link SpringTransactionEvent} 接口，
     * 无论事务中发送了多少次相同的{@link SpringTransactionEvent#getEventId()}事件，在事务结束后只会发送最后一次事件。
     *
     * @param event 事件对象
     */
    public static void publishEventIfInTransaction(Object event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // 在事务中，通过注册回调的方式发送消息
            if (event instanceof SpringTransactionEvent ev) {
                String eventId = ev.getEventId();
                Set<String> eventIds = TRANSACTION_EVENT_IDS.get();
                if (eventIds == null) {
                    eventIds = new HashSet<>();
                    TRANSACTION_EVENT_IDS.set(eventIds);
                }
                if (eventIds.contains(eventId)) {
                    // 如果该事件已注册，则不重复注册
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
                        TRANSACTION_EVENT_IDS.set(new HashSet<>());
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
