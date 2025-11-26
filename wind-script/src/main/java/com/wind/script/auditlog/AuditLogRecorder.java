package com.wind.script.auditlog;


import org.jspecify.annotations.Nullable;

/**
 * 操作日志记录者
 *
 * @author wxup
 */
public interface AuditLogRecorder {


    /**
     * 记录日志
     *
     * @param content   日志内容
     * @param throwable 请求异常，没有则为空
     */
    void write(AuditLogContent content, @Nullable Throwable throwable);


}
