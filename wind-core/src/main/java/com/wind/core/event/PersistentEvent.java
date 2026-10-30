package com.wind.core.event;

import org.jspecify.annotations.NonNull;

/**
 * 声明需要持久化并支持重新投递的事件。
 * 事件标识与业务内容共同表示一次确定的事件事实，重新投递时应保持不变。
 *
 * @author wuxp
 * @since 2026-10-29
 **/
public interface PersistentEvent {

    /**
     * 获取事件标识，在事件创建时确定，首次发布和重新投递使用同一标识。
     * 不同事件应使用不同标识，不能仅使用业务对象标识或依赖当前线程上下文临时生成。
     *
     * @return 全局唯一且非空白的事件标识
     */
    @NonNull
    String getEventId();

}
