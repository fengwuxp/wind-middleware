package com.wind.middleware.idempotent;

import com.wind.common.exception.AssertUtils;
import jakarta.validation.constraints.NotNull;

import java.util.function.Supplier;

/**
 * 幂等方法执行包装器
 * 支持 1 ~ 6 个参数方法的幂等执行，包括返回值和 void 方法
 *
 * @author wuxp
 * @date 2025-10-13
 */
public record WindIdempotentMethodInvoker(String idempotentKey) {

    public WindIdempotentMethodInvoker {
        AssertUtils.hasText(idempotentKey, "argument idempotentKey must not empty");
    }

    /**
     * 创建一个幂等方法执行器
     *
     * @param idempotentKey 幂等 key
     * @return 幂等方法执行器
     */
    public static WindIdempotentMethodInvoker key(@NotNull String idempotentKey) {
        return new WindIdempotentMethodInvoker(idempotentKey);
    }

    /**
     * 创建一个幂等方法执行器
     *
     * @param supplier 幂等 key 的生成器
     * @return 幂等方法执行器
     */
    public static WindIdempotentMethodInvoker key(@NotNull Supplier<String> supplier) {
        return key(supplier.get());
    }

    // ========= 1~6 参数带返回值 =========
    public <P, R> Idempotent1Invoker<P, R> wrapper(Idempotent1Invoker<P, R> invoker) {
        return param -> WindIdempotentExecuteUtils.execute(idempotentKey, () -> invoker.invoke(param));
    }

    public <P1, P2, R> Idempotent2Invoker<P1, P2, R> wrapper(Idempotent2Invoker<P1, P2, R> invoker) {
        return (p1, p2) -> WindIdempotentExecuteUtils.execute(idempotentKey, () -> invoker.invoke(p1, p2));
    }

    public <P1, P2, P3, R> Idempotent3Invoker<P1, P2, P3, R> wrapper(Idempotent3Invoker<P1, P2, P3, R> invoker) {
        return (p1, p2, p3) -> WindIdempotentExecuteUtils.execute(idempotentKey,
                () -> invoker.invoke(p1, p2, p3));
    }

    public <P1, P2, P3, P4, R> Idempotent4Invoker<P1, P2, P3, P4, R> wrapper(Idempotent4Invoker<P1, P2, P3, P4, R> invoker) {
        return (p1, p2, p3, p4) -> WindIdempotentExecuteUtils.execute(idempotentKey,
                () -> invoker.invoke(p1, p2, p3, p4));
    }

    public <P1, P2, P3, P4, P5, R> Idempotent5Invoker<P1, P2, P3, P4, P5, R> wrapper(Idempotent5Invoker<P1, P2, P3, P4, P5, R> invoker) {
        return (p1, p2, p3, p4, p5) -> WindIdempotentExecuteUtils.execute(idempotentKey,
                () -> invoker.invoke(p1, p2, p3, p4, p5));
    }

    public <P1, P2, P3, P4, P5, P6, R> Idempotent6Invoker<P1, P2, P3, P4, P5, P6, R> wrapper(Idempotent6Invoker<P1, P2, P3, P4, P5, P6, R> invoker) {
        return (p1, p2, p3, p4, p5, p6) -> WindIdempotentExecuteUtils.execute(idempotentKey,
                () -> invoker.invoke(p1, p2, p3, p4, p5, p6));
    }

    // ========= 1~6 参数 void =========
    public <P> Idempotent1WithVoidInvoker<P> wrapper(Idempotent1WithVoidInvoker<P> invoker) {
        return param -> WindIdempotentExecuteUtils.execute(idempotentKey, () -> invoker.invoke(param));
    }

    public <P1, P2> Idempotent2WithVoidInvoker<P1, P2> wrapper(Idempotent2WithVoidInvoker<P1, P2> invoker) {
        return (p1, p2) -> WindIdempotentExecuteUtils.execute(idempotentKey, () -> invoker.invoke(p1, p2));
    }

    public <P1, P2, P3> Idempotent3WithVoidInvoker<P1, P2, P3> wrapper(Idempotent3WithVoidInvoker<P1, P2, P3> invoker) {
        return (p1, p2, p3) -> WindIdempotentExecuteUtils.execute(idempotentKey, () -> invoker.invoke(p1, p2, p3));
    }

    public <P1, P2, P3, P4> Idempotent4WithVoidInvoker<P1, P2, P3, P4> wrapper(Idempotent4WithVoidInvoker<P1, P2, P3, P4> invoker) {
        return (p1, p2, p3, p4) -> WindIdempotentExecuteUtils.execute(idempotentKey, () -> invoker.invoke(p1, p2, p3, p4));
    }

    public <P1, P2, P3, P4, P5> Idempotent5WithVoidInvoker<P1, P2, P3, P4, P5> wrapper(Idempotent5WithVoidInvoker<P1, P2, P3, P4, P5> invoker) {
        return (p1, p2, p3, p4, p5) -> WindIdempotentExecuteUtils.execute(idempotentKey, () -> invoker.invoke(p1, p2, p3, p4, p5));
    }

    public <P1, P2, P3, P4, P5, P6> Idempotent6WithVoidInvoker<P1, P2, P3, P4, P5, P6> wrapper(Idempotent6WithVoidInvoker<P1, P2, P3, P4, P5, P6> invoker) {
        return (p1, p2, p3, p4, p5, p6) -> WindIdempotentExecuteUtils.execute(idempotentKey, () -> invoker.invoke(p1, p2, p3, p4, p5, p6));
    }

    // ================= Functional Interfaces =================

    @FunctionalInterface
    public interface Idempotent1Invoker<P, R> {
        R invoke(P param);
    }

    @FunctionalInterface
    public interface Idempotent1WithVoidInvoker<P> {
        void invoke(P param);
    }

    @FunctionalInterface
    public interface Idempotent2Invoker<P1, P2, R> {
        R invoke(P1 p1, P2 p2);
    }

    @FunctionalInterface
    public interface Idempotent2WithVoidInvoker<P1, P2> {
        void invoke(P1 p1, P2 p2);
    }

    @FunctionalInterface
    public interface Idempotent3Invoker<P1, P2, P3, R> {
        R invoke(P1 p1, P2 p2, P3 p3);
    }

    @FunctionalInterface
    public interface Idempotent3WithVoidInvoker<P1, P2, P3> {
        void invoke(P1 p1, P2 p2, P3 p3);
    }

    @FunctionalInterface
    public interface Idempotent4Invoker<P1, P2, P3, P4, R> {
        R invoke(P1 p1, P2 p2, P3 p3, P4 p4);
    }

    @FunctionalInterface
    public interface Idempotent4WithVoidInvoker<P1, P2, P3, P4> {
        void invoke(P1 p1, P2 p2, P3 p3, P4 p4);
    }

    @FunctionalInterface
    public interface Idempotent5Invoker<P1, P2, P3, P4, P5, R> {
        R invoke(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5);
    }

    @FunctionalInterface
    public interface Idempotent5WithVoidInvoker<P1, P2, P3, P4, P5> {
        void invoke(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5);
    }

    @FunctionalInterface
    public interface Idempotent6Invoker<P1, P2, P3, P4, P5, P6, R> {
        R invoke(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6);
    }

    @FunctionalInterface
    public interface Idempotent6WithVoidInvoker<P1, P2, P3, P4, P5, P6> {
        void invoke(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6);
    }
}
