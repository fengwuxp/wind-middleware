package com.wind.common.spring.event;

import org.jspecify.annotations.NonNull;

/**
 * 用于在事务中发送事件，在事务中多次发送同一事件，只会发送一次（避免事件处理者感知到事务中的多少次变化）
 * {@link com.wind.common.spring.SpringEventPublishUtils#publishEventWithCommitFallback(Object)}
 *
 * @author wuxp
 * @date 2024-06-21 10:10
 **/
public interface SpringTransactionEvent {

    /**
     * @return 事件标识，需要保证全局唯一
     */
    @NonNull
    String getEventId();
}
