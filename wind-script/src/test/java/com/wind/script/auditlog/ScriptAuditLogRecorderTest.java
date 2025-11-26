package com.wind.script.auditlog;

import com.wind.common.exception.BaseException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.Nullable;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
class ScriptAuditLogRecorderTest {

    private final SimpleScriptAuditLogRecorder logAspectRecorder = new SimpleScriptAuditLogRecorder();

    private final ExampleService exampleService = new ExampleService();

    private final Method getUserMethod = ReflectionUtils.findMethod(ExampleService.class, "getUser", String.class, String.class, Integer.class);

    private final Method testMethod = ReflectionUtils.findMethod(ExampleService.class, "test", String.class, String.class, Integer.class);

    @Test
    void testEvalLogContent() {
        User user = exampleService.getUser("张三", "hhh", 22);
        Assertions.assertNotNull(getUserMethod);
        AuditLogContent content = logAspectRecorder.buildLogContent(new Object[]{"张三", "hhh", 22}, user, getUserMethod, null);
        Assertions.assertEquals("获取用户 张三，p1 = hhh , p2 = 22", content.getLog());
        Assertions.assertEquals(1L, content.getResourceId());
        Assertions.assertEquals("用户", content.getGroup());
    }

    @Test
    void testEvalLogContentWithNoneAnnotation() {
        AuditLogContent result = logAspectRecorder.buildLogContent(new Object[]{}, null, testMethod, null);
        Assertions.assertNull(result);
    }

    @Test
    void testEvalLogContentWithError() {
        String testError = "test error";
        AuditLogContent result = logAspectRecorder.buildLogContent(new Object[]{}, null, getUserMethod, BaseException.common(testError));
        Assertions.assertNotNull(result);
        Assertions.assertEquals(testError, result.getLog());
    }


    @Test
    void testEvalLogContentWithThrowable() {
        String errorMessage = "test";
        logAspectRecorder.recordLog(new Object[]{"", "", 2}, null, getUserMethod, BaseException.common(errorMessage));
        Throwable throwable = SimpleScriptAuditLogRecorder.THROWABLE.get();
        Assertions.assertEquals(errorMessage, throwable.getMessage());
    }

    static class SimpleScriptAuditLogRecorder extends ScriptAuditLogRecorder {

        private static final AtomicReference<AuditLogContent> AUDIT_LOG_CONTENT = new AtomicReference<>();

        private static final AtomicReference<Throwable> THROWABLE = new AtomicReference<>();

        public SimpleScriptAuditLogRecorder() {
            super(SimpleScriptAuditLogRecorder::mockRecord);
        }

        private static void mockRecord(AuditLogContent content, @Nullable Throwable throwable) {
            AUDIT_LOG_CONTENT.set(content);
            THROWABLE.set(throwable);
        }
    }

    public static class ExampleService {

        @AuditLog(value = "获取用户 {#name}，p1 = {#p1} , p2 = {#p2}", group = "用户", operation = "Query", resourceType = "USER", resourceId = "#result" +
                ".id", remark = "#p1")
        public User getUser(String name, String p1, Integer p2) {
            User result = new User();
            result.setId(1L);
            result.setName(name);
            return result;
        }

        public void test() {

        }
    }

    @Data
    static class User {
        private Long id;

        private String name;
    }
}