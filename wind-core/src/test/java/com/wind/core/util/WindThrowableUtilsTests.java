package com.wind.core.util;

import com.wind.common.util.WindThrowableUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link WindThrowableUtils} 测试。
 *
 * <p>测试范围：</p>
 * <ul>
 *     <li>异常链类型判断：覆盖当前异常命中、cause 命中、未命中、空异常链。</li>
 *     <li>根因获取：覆盖无 cause、多层 cause、cause 链成环。</li>
 *     <li>异常类型查找：覆盖当前异常命中、cause 命中、未命中、空异常链和泛型返回。</li>
 *     <li>异常消息匹配：覆盖当前异常 message、cause message、null message、空白预期消息、未命中和 cause 链成环。</li>
 * </ul>
 *
 * <p>不测范围：</p>
 * <ul>
 *     <li>不递归检查 suppressed exception，因为当前工具类只声明处理 throwable 与 cause 链。</li>
 *     <li>不验证国际化、错误码或业务异常语义，message 匹配只作为兼容性工具能力。</li>
 * </ul>
 *
 * <p>核心断言：</p>
 * <ul>
 *     <li>异常链遍历必须包含当前异常本身。</li>
 *     <li>异常链成环时不能死循环。</li>
 *     <li>空白 expectedMessage 不应被当作命中。</li>
 *     <li>findCauseOfType 应返回具体异常类型，调用方不需要手动强转。</li>
 * </ul>
 */
class WindThrowableUtilsTests {

    @Test
    void isCausedByShouldReturnTrueWhenCurrentThrowableMatches() {
        IllegalArgumentException throwable = new IllegalArgumentException("bad request");

        assertTrue(WindThrowableUtils.isCausedBy(throwable, IllegalArgumentException.class));
    }

    @Test
    void isCausedByShouldReturnTrueWhenCauseMatches() {
        IllegalArgumentException root = new IllegalArgumentException("bad request");
        RuntimeException throwable = new RuntimeException("wrapped", root);

        assertTrue(WindThrowableUtils.isCausedBy(throwable, IllegalArgumentException.class));
    }

    @Test
    void isCausedByShouldReturnFalseWhenThrowableIsNull() {
        assertFalse(WindThrowableUtils.isCausedBy(null, IllegalArgumentException.class));
    }

    @Test
    void isCausedByShouldReturnFalseWhenNoCauseMatches() {
        RuntimeException throwable = new RuntimeException("wrapped", new IllegalStateException("state error"));

        assertFalse(WindThrowableUtils.isCausedBy(throwable, IllegalArgumentException.class));
    }

    @Test
    void getRootCauseShouldReturnSelfWhenNoCauseExists() {
        RuntimeException throwable = new RuntimeException("root");

        assertSame(throwable, WindThrowableUtils.getRootCause(throwable));
    }

    @Test
    void getRootCauseShouldReturnDeepestCause() {
        IllegalArgumentException root = new IllegalArgumentException("root");
        IllegalStateException middle = new IllegalStateException("middle", root);
        RuntimeException throwable = new RuntimeException("top", middle);

        assertSame(root, WindThrowableUtils.getRootCause(throwable));
    }

    @Test
    void getRootCauseShouldStopBeforeRevisitingThrowableWhenCauseChainHasCycle() {
        RuntimeException first = new RuntimeException("first");
        IllegalStateException second = new IllegalStateException("second");
        first.initCause(second);
        second.initCause(first);

        // second 是继续向下前发现会回到已访问节点时，异常链上最后一个可达节点。
        assertSame(second, WindThrowableUtils.getRootCause(first));
    }

    @Test
    void findCauseOfTypeShouldReturnCurrentThrowableWhenCurrentMatches() {
        IllegalArgumentException throwable = new IllegalArgumentException("bad request");

        IllegalArgumentException actual = WindThrowableUtils.findCauseOfType(throwable, IllegalArgumentException.class);

        assertSame(throwable, actual);
    }

    @Test
    void findCauseOfTypeShouldReturnFirstMatchedCause() {
        IllegalArgumentException root = new IllegalArgumentException("root");
        IllegalStateException middle = new IllegalStateException("middle", root);
        RuntimeException throwable = new RuntimeException("top", middle);

        IllegalStateException actual = WindThrowableUtils.findCauseOfType(throwable, IllegalStateException.class);

        assertSame(middle, actual);
    }

    @Test
    void findCauseOfTypeShouldReturnNullWhenThrowableIsNull() {
        assertNull(WindThrowableUtils.findCauseOfType(null, IllegalArgumentException.class));
    }

    @Test
    void findCauseOfTypeShouldReturnNullWhenNoCauseMatches() {
        RuntimeException throwable = new RuntimeException("top", new IllegalStateException("state error"));

        assertNull(WindThrowableUtils.findCauseOfType(throwable, IllegalArgumentException.class));
    }

    @Test
    void findCauseOfTypeShouldStopWhenCauseChainHasCycle() {
        RuntimeException first = new RuntimeException("first");
        IllegalStateException second = new IllegalStateException("second");
        first.initCause(second);
        second.initCause(first);

        IllegalStateException actual = WindThrowableUtils.findCauseOfType(first, IllegalStateException.class);

        assertSame(second, actual);
    }

    @Test
    void containsExceptionMessageShouldReturnTrueWhenCurrentMessageContainsExpectedMessage() {
        RuntimeException throwable = new RuntimeException("issuer fee assessment failed");

        assertTrue(WindThrowableUtils.containsExceptionMessage(throwable, "assessment failed"));
    }

    @Test
    void containsExceptionMessageShouldReturnTrueWhenCauseMessageContainsExpectedMessage() {
        IllegalArgumentException root = new IllegalArgumentException("customer_id is required");
        RuntimeException throwable = new RuntimeException("wrapped", root);

        assertTrue(WindThrowableUtils.containsExceptionMessage(throwable, "customer_id"));
    }

    @Test
    void containsExceptionMessageShouldReturnFalseWhenThrowableIsNull() {
        assertFalse(WindThrowableUtils.containsExceptionMessage(null, "customer_id"));
    }

    @Test
    void containsExceptionMessageShouldReturnFalseWhenExpectedMessageIsBlank() {
        RuntimeException throwable = new RuntimeException("any message");

        assertFalse(WindThrowableUtils.containsExceptionMessage(throwable, ""));
        assertFalse(WindThrowableUtils.containsExceptionMessage(throwable, " "));
    }

    @Test
    void containsExceptionMessageShouldReturnFalseWhenMessageIsNull() {
        RuntimeException throwable = new RuntimeException();

        assertFalse(WindThrowableUtils.containsExceptionMessage(throwable, "expected"));
    }

    @Test
    void containsExceptionMessageShouldReturnFalseWhenNoMessageMatches() {
        RuntimeException throwable = new RuntimeException("actual message");

        assertFalse(WindThrowableUtils.containsExceptionMessage(throwable, "expected"));
    }

    @Test
    void containsExceptionMessageShouldStopWhenCauseChainHasCycle() {
        RuntimeException first = new RuntimeException("first");
        IllegalStateException second = new IllegalStateException("second");
        first.initCause(second);
        second.initCause(first);

        assertTrue(WindThrowableUtils.containsExceptionMessage(first, "second"));
        assertFalse(WindThrowableUtils.containsExceptionMessage(first, "third"));
    }

    @Test
    void containsExceptionMessageShouldNotSearchSuppressedExceptions() {
        RuntimeException throwable = new RuntimeException("main message");
        throwable.addSuppressed(new IllegalStateException("suppressed message"));

        assertFalse(WindThrowableUtils.containsExceptionMessage(throwable, "suppressed"));
    }
}