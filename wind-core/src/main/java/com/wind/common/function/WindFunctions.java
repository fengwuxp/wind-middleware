package com.wind.common.function;


/**
 * 一组允许抛出受检异常（Checked Exception）的函数式接口定义。
 * <p>
 * Java 标准函数式接口（如 {@link java.util.function.Supplier}、{@link java.lang.Runnable}）
 * 不支持抛出受检异常，本接口提供对应的增强版本，
 * 便于在 Lambda 表达式中优雅地处理和透传异常。
 * </p>
 *
 * <p>
 * 示例：
 * <pre>{@code
 * WindFunctions.ThrowsSupplier<String> supplier = () -> {
 *     if (new Random().nextBoolean()) {
 *         throw new IOException("failed");
 *     }
 *     return "OK";
 * };
 * }</pre>
 * </p>
 *
 * @author wuxp
 * @date 2025-10-13
 */
public interface WindFunctions {

    /**
     * 可抛出异常的 {@link Runnable} 定义。
     * <p>等价于：{@code () -> { ... }}，允许抛出任意异常。</p>
     */
    @FunctionalInterface
    interface ThrowsRunnable {

        /**
         * 执行操作，允许抛出受检异常
         *
         * @throws Throwable 任何执行过程中产生的异常
         */
        void run() throws Throwable;
    }

    /**
     * 可抛出异常的 {@link java.util.function.Supplier} 定义。
     * <p>用于在 Lambda 表达式中优雅地返回结果并抛出异常。</p>
     *
     * @param <T> 返回值类型
     */
    @FunctionalInterface
    interface ThrowsSupplier<T> {

        /**
         * 获取结果，允许抛出受检异常
         *
         * @return 结果值
         * @throws Throwable 任何执行过程中产生的异常
         */
        T get() throws Throwable;
    }

    @FunctionalInterface
    interface ThrowsFunction<T, R> {
        R apply(T t) throws Throwable;
    }

    @FunctionalInterface
    interface ThrowsConsumer<T> {
        void accept(T t) throws Throwable;
    }

}

