package com.wind.core.event;

import org.jspecify.annotations.NonNull;

/**
 * 保存事件快照并记录整个事件的投递或处理结果，具体存储和事务由实现方负责。
 * 结果由实际处理完成的一方报告，事件发布或异步入队成功不能作为处理成功的依据。
 * 未启用持久化时可使用 {@link EventPersistence#NONE}，其方法不保存任何数据。
 *
 * @author wuxp
 * @since 2026-10-29
 **/
public interface EventPersistence {

    EventPersistence NONE = new EventPersistence() {
        @Override
        public void save(@NonNull PersistentEvent event) {

        }

        @Override
        public void markSucceeded(@NonNull String eventId) {

        }

        @Override
        public void markFailed(@NonNull String eventId, @NonNull Throwable cause) {

        }
    };

    /**
     * 保存事件快照，供后续恢复并重新投递。
     * 同一标识的相同事件重复保存时不得新增记录或重置处理结果，内容冲突时应拒绝保存。
     * 存在业务事务时应参与同一事务，返回不代表事务已经提交；无事务时应在返回前完成持久化。
     *
     * @param event 需要保存的事件
     * @throws IllegalArgumentException 同一事件标识对应的内容冲突
     * @throws RuntimeException         事件保存失败
     */
    void save(@NonNull PersistentEvent event);

    /**
     * 将已有事件记录标记为处理成功，重复报告成功不改变已确认的结果。
     * 调用方必须确认整个事件的处理已经完成，相关业务事务已经提交。
     * 结果写入失败不代表事件处理失败，实现方应将写入异常交给调用方处理。
     *
     * @param eventId 事件标识，非空白
     * @throws RuntimeException 事件记录不存在或结果保存失败
     */
    void markSucceeded(@NonNull String eventId);

    /**
     * 记录已有事件本次投递或处理失败的原因，不得将已确认成功的事件改为失败。
     * 线程池拒绝任务属于投递失败，此时监听器尚未执行。
     * 实现方按需保存脱敏后的错误信息，不直接持久化异常对象。
     *
     * @param eventId 事件标识，非空白
     * @param cause   事件投递或处理失败的原因
     * @throws RuntimeException 事件记录不存在或结果保存失败
     */
    void markFailed(@NonNull String eventId, @NonNull Throwable cause);

}
