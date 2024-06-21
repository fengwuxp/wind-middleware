package com.wind.common.spring;

import com.wind.common.spring.event.SpringTransactionEvent;
import com.wind.common.util.ExecutorServiceUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * spring event {@link org.springframework.context.ApplicationEvent} publish utils
 *
 * @author wuxp
 * @date 2024-06-21 09:54
 **/
@Slf4j
public final class SpringEventPublishUtils {

    // TODO 使用 Dynamic-TP 监控执行
    private static final ThreadPoolExecutor EXECUTOR = ExecutorServiceUtils.newExecutor("Spring-Event-", 1, 2, 256);

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
        SpringApplicationContextUtils.requireApplicationContext().publishEvent(event);
    }

    /**
     * 异步发送事件
     *
     * @param event 事件对象
     */
    public static void publishAsync(Object event) {
        EXECUTOR.execute(() -> publishEvent(event));
    }

    /**
     * 如果在事务内，事件推迟到事务提交后发送
     *
     * @param event 事件对象
     */
    public static void publishEventIfInTransaction(Object event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // 在事务中，通过注册回调的方式发送消息
            if (event instanceof SpringTransactionEvent) {
                String eventId = ((SpringTransactionEvent) event).getEventId();
                Set<String> eventIds = TRANSACTION_EVENT_IDS.get();
                if (CollectionUtils.isEmpty(eventIds) && eventIds.contains(eventId)) {
                    // 如果该事件已注册，clear 已注册的事件
                    TransactionSynchronizationManager.clearSynchronization();
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
                        TRANSACTION_EVENT_IDS.set(new HashSet<>());
                    }
                }
            });
        } else {
            publishEvent(event);
        }
    }

}
