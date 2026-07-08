package com.wind.common.spring;

import com.wind.common.exception.AssertUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 基于 Spring {@link TransactionTemplate} 的静态编程式事务执行器。
 *
 * <p>适用于非 Spring Bean 或静态工具类中需要显式事务边界的少量场景。
 * Spring Bean 内优先使用 {@code @Transactional} 或直接注入 {@link TransactionTemplate}。</p>
 *
 * @author wuxp
 * @date 2026-07-08 11:12
 */
public final class SpringProgrammaticTransactions {

    private static final AtomicReference<PlatformTransactionManager> transactionManager = new AtomicReference<>();

    private SpringProgrammaticTransactions() {
        throw new AssertionError();
    }

    /**
     * 使用 {@link TransactionDefinition#PROPAGATION_REQUIRED} 执行有返回值回调。
     *
     * <p>当前存在事务时加入当前事务；当前不存在事务时创建新事务。</p>
     *
     * @param action 事务内执行的回调
     * @param <T>    回调返回值类型
     * @return 回调执行结果
     */
    public static <T> T required(@NonNull Supplier<T> action) {
        return execute(TransactionDefinition.PROPAGATION_REQUIRED, action);
    }

    /**
     * 使用 {@link TransactionDefinition#PROPAGATION_REQUIRED} 执行无返回值回调。
     *
     * @param action 事务内执行的回调
     */
    public static void required(@NonNull Runnable action) {
        required(() -> {
            action.run();
            return null;
        });
    }

    /**
     * 使用 {@link TransactionDefinition#PROPAGATION_REQUIRES_NEW} 执行有返回值回调。
     *
     * <p>总是开启一个新事务；如果当前已存在事务，会先挂起当前事务。</p>
     *
     * @param action 事务内执行的回调
     * @param <T>    回调返回值类型
     * @return 回调执行结果
     */
    public static <T> T requiresNew(@NonNull Supplier<T> action) {
        return execute(TransactionDefinition.PROPAGATION_REQUIRES_NEW, action);
    }

    /**
     * 使用 {@link TransactionDefinition#PROPAGATION_REQUIRES_NEW} 执行无返回值回调。
     *
     * @param action 事务内执行的回调
     */
    public static void requiresNew(@NonNull Runnable action) {
        requiresNew(() -> {
            action.run();
            return null;
        });
    }

    /**
     * 按指定事务传播行为执行回调。
     *
     * @param propagationBehavior 事务传播行为
     * @param action              事务内执行的回调
     * @param <T>                 回调返回值类型
     * @return 回调执行结果
     */
    private static <T> T execute(int propagationBehavior, @NonNull Supplier<T> action) {
        TransactionTemplate template = new TransactionTemplate(getTransactionManager());
        template.setPropagationBehavior(propagationBehavior);
        return template.execute(status -> action.get());
    }

    /**
     * 获取已配置的 Spring 事务管理器。
     *
     * @return Spring 平台事务管理器
     */
    private static PlatformTransactionManager getTransactionManager() {
        PlatformTransactionManager result = transactionManager.get();
        AssertUtils.notNull(result, "PlatformTransactionManager has not been initialized");
        return result;
    }

    /**
     * 配置 Spring 事务管理器。
     *
     * <p>应在 Spring 容器启动完成后初始化一次，例如由 Spring Bean 注入后调用。</p>
     *
     * @param transactionManager Spring 平台事务管理器
     */
    public static void configureTransactionManager(PlatformTransactionManager transactionManager) {
        SpringProgrammaticTransactions.transactionManager.set(transactionManager);
    }


    static void initialize(ApplicationContext applicationContext) {
        configureTransactionManager(applicationContext.getBean(PlatformTransactionManager.class));
    }
}