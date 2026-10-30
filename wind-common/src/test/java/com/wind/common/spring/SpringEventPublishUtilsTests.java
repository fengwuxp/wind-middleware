package com.wind.common.spring;

import com.wind.common.spring.event.SpringTransactionEvent;
import com.wind.core.event.EventPersistence;
import com.wind.core.event.PersistentEvent;
import com.wind.trace.WindTracer;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证真实 Spring 广播与事务回调中的事件持久化边界，存储使用测试内存实现。
 *
 * @author wuxp
 * @since 2026-10-29
 **/
class SpringEventPublishUtilsTests {

    private GenericApplicationContext context;

    private final TransactionTemplate transaction = new TransactionTemplate(new TestTransactionManager());

    @BeforeEach
    void setup() {
        context = new GenericApplicationContext();
        context.registerBean(SpringApplicationContextUtils.class);
        context.refresh();
    }

    @AfterEach
    void cleanup() {
        context.close();
    }

    @Test
    void testPersistentEventRecordsSuccessfulHandling() {
        MemoryEventPersistence persistence = registerPersistence();
        List<Object> delivered = new CopyOnWriteArrayList<>();
        listen(delivered::add);
        SampleEvent event = new SampleEvent("success", "content");

        SpringEventPublishUtils.publishEvent(event);

        assertEquals(List.of(event), delivered);
        assertEquals(event, persistence.events.get(event.getEventId()));
        assertEquals("SUCCEEDED", persistence.statuses.get(event.getEventId()));
    }

    @ParameterizedTest
    @EnumSource(PublicationMode.class)
    void testPersistentEventWithoutPersistenceBean(PublicationMode mode) {
        List<Object> delivered = new CopyOnWriteArrayList<>();
        listen(delivered::add);
        SampleEvent event = new SampleEvent("none", "content");

        mode.publish.accept(event);

        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> assertEquals(List.of(event), delivered));
    }

    @ParameterizedTest
    @EnumSource(PublicationMode.class)
    void testPersistentEventWithoutBeanWaitsForCommit(PublicationMode mode) {
        List<Object> delivered = new CopyOnWriteArrayList<>();
        listen(delivered::add);
        SampleEvent event = new SampleEvent("none-transaction", "content");

        transaction.executeWithoutResult(status -> {
            mode.publish.accept(event);
            assertRemainsEmpty(delivered);
        });

        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> assertEquals(List.of(event), delivered));
    }

    @ParameterizedTest
    @EnumSource(PublicationMode.class)
    void testPersistentEventSavesInCallingTransactionAndWaitsForCommit(PublicationMode mode) {
        MemoryEventPersistence persistence = registerPersistence();
        List<Object> delivered = new CopyOnWriteArrayList<>();
        listen(delivered::add);
        SampleEvent event = new SampleEvent("committed", "content");

        transaction.executeWithoutResult(status -> {
            mode.publish.accept(event);
            assertSame(Thread.currentThread(), persistence.savingThreads.get(event.getEventId()));
            assertEquals(Boolean.TRUE, persistence.savingTransactions.get(event.getEventId()));
            assertFalse(persistence.events.containsKey(event.getEventId()));
            assertRemainsEmpty(delivered);
        });

        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            assertEquals(List.of(event), delivered);
            assertEquals(event, persistence.events.get(event.getEventId()));
            assertEquals("SUCCEEDED", persistence.statuses.get(event.getEventId()));
        });
    }

    @ParameterizedTest
    @EnumSource(PublicationMode.class)
    void testPersistentEventRollbackDiscardsSaveAndDelivery(PublicationMode mode) {
        MemoryEventPersistence persistence = registerPersistence();
        List<Object> delivered = new CopyOnWriteArrayList<>();
        listen(delivered::add);
        SampleEvent event = new SampleEvent("rolled-back", "content");

        transaction.executeWithoutResult(status -> {
            mode.publish.accept(event);
            assertSame(Thread.currentThread(), persistence.savingThreads.get(event.getEventId()));
            assertRemainsEmpty(delivered);
            status.setRollbackOnly();
        });

        assertRemainsEmpty(delivered);
        assertTrue(persistence.events.isEmpty());
        assertTrue(persistence.statuses.isEmpty());
    }

    @Test
    void testPersistentEventRecordsListenerFailureAndPropagatesIt() {
        MemoryEventPersistence persistence = registerPersistence();
        IllegalStateException failure = new IllegalStateException("listener failure");
        listen(event -> {
            throw failure;
        });
        SampleEvent event = new SampleEvent("failed", "content");

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> SpringEventPublishUtils.publishEvent(event));

        assertSame(failure, thrown);
        assertEquals("FAILED", persistence.statuses.get(event.getEventId()));
        assertSame(failure, persistence.failures.get(event.getEventId()));
    }

    @Test
    void testSaveFailurePreventsDelivery() {
        MemoryEventPersistence persistence = registerPersistence();
        persistence.saveFailure = new IllegalStateException("save failure");
        List<Object> delivered = new CopyOnWriteArrayList<>();
        listen(delivered::add);

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> SpringEventPublishUtils.publishEvent(new SampleEvent("save-failed", "content")));

        assertSame(persistence.saveFailure, thrown);
        assertTrue(delivered.isEmpty());
        assertTrue(persistence.events.isEmpty());
    }

    @Test
    void testSuccessRecordFailureDoesNotBecomeListenerFailure() {
        MemoryEventPersistence persistence = registerPersistence();
        persistence.successFailure = new IllegalStateException("success record failure");
        List<Object> delivered = new CopyOnWriteArrayList<>();
        listen(delivered::add);
        SampleEvent event = new SampleEvent("success-write-failed", "content");

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> SpringEventPublishUtils.publishEvent(event));

        assertSame(persistence.successFailure, thrown);
        assertEquals(List.of(event), delivered);
        assertEquals("PENDING", persistence.statuses.get(event.getEventId()));
        assertTrue(persistence.failures.isEmpty());
    }

    @Test
    void testFailureRecordErrorPreservesOriginalListenerFailure() {
        MemoryEventPersistence persistence = registerPersistence();
        persistence.failureWriteFailure = new IllegalStateException("failure record failure");
        IllegalArgumentException processingFailure = new IllegalArgumentException("listener failure");
        listen(event -> {
            throw processingFailure;
        });
        SampleEvent event = new SampleEvent("failure-write-failed", "content");

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> SpringEventPublishUtils.publishEvent(event));

        assertSame(processingFailure, thrown);
        assertArrayEquals(new Throwable[]{persistence.failureWriteFailure}, thrown.getSuppressed());
        assertEquals("PENDING", persistence.statuses.get(event.getEventId()));
    }

    @Test
    void testAmbiguousPersistenceBeansDoNotUseNoneFallback() {
        context.registerBean("first", EventPersistence.class, MemoryEventPersistence::new);
        context.registerBean("second", EventPersistence.class, MemoryEventPersistence::new);
        List<Object> delivered = new CopyOnWriteArrayList<>();
        listen(delivered::add);

        assertThrows(NoUniqueBeanDefinitionException.class,
                () -> SpringEventPublishUtils.publishEvent(new SampleEvent("ambiguous", "content")));

        assertTrue(delivered.isEmpty());
    }

    @Test
    void testPersistenceInitializationFailureDoesNotUseNoneFallback() {
        IllegalStateException failure = registerBrokenPersistence();
        List<Object> delivered = new CopyOnWriteArrayList<>();
        listen(delivered::add);

        BeanCreationException thrown = assertThrows(BeanCreationException.class,
                () -> SpringEventPublishUtils.publishEvent(new SampleEvent("broken-bean", "content")));

        assertSame(failure, thrown.getMostSpecificCause());
        assertTrue(delivered.isEmpty());
    }

    @Test
    void testPrimaryPersistenceBeanIsUsed() {
        MemoryEventPersistence primary = new MemoryEventPersistence();
        MemoryEventPersistence secondary = new MemoryEventPersistence();
        context.registerBean("primary", EventPersistence.class, () -> primary, bean -> bean.setPrimary(true));
        context.registerBean("secondary", EventPersistence.class, () -> secondary);
        List<Object> delivered = new CopyOnWriteArrayList<>();
        listen(delivered::add);
        SampleEvent event = new SampleEvent("primary", "content");

        SpringEventPublishUtils.publishEvent(event);

        assertEquals(List.of(event), delivered);
        assertEquals("SUCCEEDED", primary.statuses.get(event.getEventId()));
        assertTrue(secondary.events.isEmpty());
    }

    @Test
    void testPureDeliveryDoesNotResolvePersistenceBean() {
        registerBrokenPersistence();
        List<Object> delivered = new CopyOnWriteArrayList<>();
        listen(delivered::add);
        SampleEvent event = new SampleEvent("direct", "content");

        SpringEventPublishUtils.publishDirectly(event);

        assertEquals(List.of(event), delivered);
    }

    @Test
    void testPureDeliveryDoesNotSaveEventOrChangeExistingResult() {
        MemoryEventPersistence persistence = registerPersistence();
        SampleEvent saved = new SampleEvent("saved", "content");
        SampleEvent unsaved = new SampleEvent("unsaved", "content");
        persistence.save(saved);
        List<Object> delivered = new CopyOnWriteArrayList<>();
        listen(delivered::add);

        SpringEventPublishUtils.publishDirectly(saved);
        SpringEventPublishUtils.publishDirectly(unsaved);

        assertEquals(List.of(saved, unsaved), delivered);
        assertEquals(Map.of(saved.getEventId(), saved), persistence.events);
        assertEquals(Map.of(saved.getEventId(), "PENDING"), persistence.statuses);
    }

    @Test
    void testPureDeliveryPropagatesListenerFailureWithoutRecordingIt() {
        MemoryEventPersistence persistence = registerPersistence();
        SampleEvent event = new SampleEvent("direct-failure", "content");
        persistence.save(event);
        IllegalStateException failure = new IllegalStateException("listener failure");
        listen(payload -> {
            throw failure;
        });

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> SpringEventPublishUtils.publishDirectly(event));

        assertSame(failure, thrown);
        assertEquals("PENDING", persistence.statuses.get(event.getEventId()));
        assertTrue(persistence.failures.isEmpty());
    }

    @Test
    void testPureDeliveryDoesNotWaitForTransactionCommit() {
        List<Object> delivered = new CopyOnWriteArrayList<>();
        listen(delivered::add);
        SampleEvent event = new SampleEvent("direct-in-transaction", "content");

        transaction.executeWithoutResult(status -> {
            SpringEventPublishUtils.publishDirectly(event);
            assertEquals(List.of(event), delivered);
            status.setRollbackOnly();
        });

        assertEquals(List.of(event), delivered);
    }

    @Test
    void testAfterCommitFailureDoesNotPreventFollowingPersistentEvent() {
        MemoryEventPersistence persistence = registerPersistence();
        SampleEvent first = new SampleEvent("first", "content");
        SampleEvent second = new SampleEvent("second", "content");
        List<Object> delivered = new CopyOnWriteArrayList<>();
        IllegalStateException failure = new IllegalStateException("first failed");
        listen(event -> {
            if (event.equals(first)) {
                throw failure;
            }
            delivered.add(event);
        });

        transaction.executeWithoutResult(status -> {
            SpringEventPublishUtils.publishEvent(first);
            SpringEventPublishUtils.publishEvent(second);
        });

        assertEquals(List.of(second), delivered);
        assertEquals("FAILED", persistence.statuses.get(first.getEventId()));
        assertEquals("SUCCEEDED", persistence.statuses.get(second.getEventId()));
        assertSame(failure, persistence.failures.get(first.getEventId()));
    }

    @Test
    void testAsyncDeliveryPreservesTraceAndRecordsCompletion() {
        MemoryEventPersistence persistence = registerPersistence();
        AtomicReference<Thread> handlingThread = new AtomicReference<>();
        AtomicReference<String> actualTrace = new AtomicReference<>();
        AtomicReference<String> expectedTrace = new AtomicReference<>();
        listen(event -> {
            handlingThread.set(Thread.currentThread());
            actualTrace.set(WindTracer.TRACER.requireTraceId());
        });
        SampleEvent event = new SampleEvent("async-trace", "content");
        Thread caller = Thread.currentThread();

        WindTracer.TRACER.run(() -> {
            expectedTrace.set(WindTracer.TRACER.requireTraceId());
            SpringEventPublishUtils.publishAsync(event);
        });

        await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                assertEquals("SUCCEEDED", persistence.statuses.get(event.getEventId())));
        assertSame(caller, persistence.savingThreads.get(event.getEventId()));
        assertNotSame(caller, handlingThread.get());
        assertEquals(expectedTrace.get(), actualTrace.get());
    }

    @Test
    void testAsyncListenerFailureIsRecorded() {
        MemoryEventPersistence persistence = registerPersistence();
        IllegalStateException failure = new IllegalStateException("async listener failure");
        listen(event -> {
            throw failure;
        });
        SampleEvent event = new SampleEvent("async-failure", "content");

        SpringEventPublishUtils.publishAsync(event);

        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            assertEquals("FAILED", persistence.statuses.get(event.getEventId()));
            assertSame(failure, persistence.failures.get(event.getEventId()));
        });
    }

    @Test
    @Timeout(10)
    void testAcceptedAsyncTaskIsNotSuccessfulBeforeListenerCompletes() throws InterruptedException {
        MemoryEventPersistence persistence = registerPersistence();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        listen(event -> {
            started.countDown();
            try {
                release.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
        });
        SampleEvent event = new SampleEvent("async-in-progress", "content");

        SpringEventPublishUtils.publishAsync(event);

        try {
            assertTrue(started.await(3, TimeUnit.SECONDS));
            assertEquals("PENDING", persistence.statuses.get(event.getEventId()));
        } finally {
            release.countDown();
            await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                    assertEquals("SUCCEEDED", persistence.statuses.get(event.getEventId())));
        }
    }

    @Test
    @Timeout(15)
    void testQueueRejectionRecordsFailureWithoutExecutingListener() {
        MemoryEventPersistence persistence = registerPersistence();
        List<Object> delivered = new CopyOnWriteArrayList<>();
        SampleEvent event = new SampleEvent("rejected", "content");

        withSaturatedExecutor(delivered, () -> {
            RejectedExecutionException rejection = assertThrows(RejectedExecutionException.class,
                    () -> SpringEventPublishUtils.publishAsync(event));

            assertEquals(event, persistence.events.get(event.getEventId()));
            assertEquals("FAILED", persistence.statuses.get(event.getEventId()));
            assertSame(rejection, persistence.failures.get(event.getEventId()));
        });

        assertTrue(delivered.isEmpty());
    }

    @Test
    @Timeout(15)
    void testAfterCommitQueueRejectionDoesNotPreventFollowingEvent() {
        MemoryEventPersistence persistence = registerPersistence();
        List<Object> delivered = new CopyOnWriteArrayList<>();
        SampleEvent rejected = new SampleEvent("after-commit-rejected", "content");
        SampleEvent following = new SampleEvent("after-rejection", "content");

        withSaturatedExecutor(delivered, () -> transaction.executeWithoutResult(status -> {
            SpringEventPublishUtils.publishAsync(rejected);
            SpringEventPublishUtils.publishEvent(following);
        }));

        assertEquals("FAILED", persistence.statuses.get(rejected.getEventId()));
        assertInstanceOf(RejectedExecutionException.class, persistence.failures.get(rejected.getEventId()));
        assertEquals("SUCCEEDED", persistence.statuses.get(following.getEventId()));
        assertEquals(List.of(following), delivered);
    }

    @Test
    @Timeout(15)
    void testRejectionRecordFailurePreservesOriginalRejection() {
        MemoryEventPersistence persistence = registerPersistence();
        persistence.failureWriteFailure = new IllegalStateException("rejection record failure");
        List<Object> delivered = new CopyOnWriteArrayList<>();
        SampleEvent event = new SampleEvent("rejection-record-failed", "content");

        withSaturatedExecutor(delivered, () -> {
            RejectedExecutionException rejection = assertThrows(RejectedExecutionException.class,
                    () -> SpringEventPublishUtils.publishAsync(event));

            assertArrayEquals(new Throwable[]{persistence.failureWriteFailure}, rejection.getSuppressed());
            assertEquals(event, persistence.events.get(event.getEventId()));
            assertEquals("PENDING", persistence.statuses.get(event.getEventId()));
            assertTrue(persistence.failures.isEmpty());
        });

        assertTrue(delivered.isEmpty());
    }

    @Test
    @Timeout(15)
    void testQueueRejectionWithoutPersistenceBeanStillPropagates() {
        List<Object> delivered = new CopyOnWriteArrayList<>();
        SampleEvent event = new SampleEvent("none-rejected", "content");

        withSaturatedExecutor(delivered, () -> assertThrows(RejectedExecutionException.class,
                () -> SpringEventPublishUtils.publishAsync(event)));

        assertTrue(delivered.isEmpty());
    }

    @Test
    void testOrdinaryEventPublishesImmediatelyWithoutResolvingPersistence() {
        registerBrokenPersistence();
        List<Object> delivered = new CopyOnWriteArrayList<>();
        listen(delivered::add);

        transaction.executeWithoutResult(status -> {
            SpringEventPublishUtils.publishEvent("ordinary");
            assertEquals(List.of("ordinary"), delivered);
            status.setRollbackOnly();
        });

        assertEquals(List.of("ordinary"), delivered);
    }

    @Test
    void testOrdinaryListenerFailureStillPropagates() {
        IllegalStateException failure = new IllegalStateException("ordinary failure");
        listen(event -> {
            throw failure;
        });

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> SpringEventPublishUtils.publishEvent("ordinary"));

        assertSame(failure, thrown);
    }

    @Test
    void testOrdinaryTransactionEventKeepsFirstOccurrence() {
        List<Object> delivered = new CopyOnWriteArrayList<>();
        listen(delivered::add);
        TransactionEvent first = new TransactionEvent("same-id", "first");
        TransactionEvent second = new TransactionEvent("same-id", "second");

        transaction.executeWithoutResult(status -> {
            SpringEventPublishUtils.publishWithTransactionCommitOrImmediately(first);
            SpringEventPublishUtils.publishWithTransactionCommitOrImmediately(second);
            assertTrue(delivered.isEmpty());
        });

        assertEquals(List.of(first), delivered);
    }

    @Test
    void testInnerRollbackDoesNotClearOuterTransactionEventDeduplication() {
        List<Object> delivered = new CopyOnWriteArrayList<>();
        listen(delivered::add);
        TransactionEvent first = new TransactionEvent("outer-id", "first");
        TransactionEvent repeated = new TransactionEvent("outer-id", "repeated");
        TransactionEvent discarded = new TransactionEvent("inner-id", "discarded");
        TransactionTemplate requiresNew = new TransactionTemplate(transaction.getTransactionManager());
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        transaction.executeWithoutResult(status -> {
            SpringEventPublishUtils.publishWithTransactionCommitOrImmediately(first);
            requiresNew.executeWithoutResult(innerStatus -> {
                SpringEventPublishUtils.publishWithTransactionCommitOrImmediately(discarded);
                innerStatus.setRollbackOnly();
            });
            SpringEventPublishUtils.publishWithTransactionCommitOrImmediately(repeated);
            assertTrue(delivered.isEmpty());
        });

        assertEquals(List.of(first), delivered);
    }

    @Test
    void testOrdinaryTransactionEventCanBeRepublishedInNewTransactionAfterCommit() {
        List<Object> delivered = new CopyOnWriteArrayList<>();
        listen(delivered::add);
        TransactionEvent outer = new TransactionEvent("same-id-after-commit", "outer");
        TransactionEvent inner = new TransactionEvent("same-id-after-commit", "inner");
        TransactionTemplate requiresNew = new TransactionTemplate(transaction.getTransactionManager());
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        transaction.executeWithoutResult(status -> {
            SpringEventPublishUtils.publishWithTransactionCommitOrImmediately(outer);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    requiresNew.executeWithoutResult(innerStatus ->
                            SpringEventPublishUtils.publishWithTransactionCommitOrImmediately(inner));
                }
            });
        });

        assertEquals(List.of(outer, inner), delivered);
    }

    /**
     * 通过公开异步入口占满真实线程池，完成断言后释放并等待所有已受理任务结束。
     */
    private void withSaturatedExecutor(List<Object> delivered, Runnable action) {
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger finished = new AtomicInteger();
        AtomicInteger accepted = new AtomicInteger();
        listen(event -> {
            if (!(event instanceof String)) {
                delivered.add(event);
                return;
            }
            try {
                release.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            } finally {
                finished.incrementAndGet();
            }
        });

        try {
            boolean rejected = false;
            for (int i = 0; i < 1024; i++) {
                try {
                    SpringEventPublishUtils.publishAsync("blocking-" + i);
                    accepted.incrementAndGet();
                } catch (RejectedExecutionException exception) {
                    rejected = true;
                    break;
                }
            }
            assertTrue(rejected, "有界线程池应该拒绝超出容量的任务");
            action.run();
        } finally {
            release.countDown();
            await().atMost(Duration.ofSeconds(5)).until(() -> finished.get() == accepted.get());
        }
    }

    private static void assertRemainsEmpty(List<?> events) {
        await().during(Duration.ofMillis(100)).atMost(Duration.ofSeconds(2)).until(events::isEmpty);
    }

    private IllegalStateException registerBrokenPersistence() {
        IllegalStateException failure = new IllegalStateException("persistence initialization failed");
        context.registerBean("broken", EventPersistence.class, () -> {
            throw failure;
        }, bean -> bean.setLazyInit(true));
        return failure;
    }

    private MemoryEventPersistence registerPersistence() {
        MemoryEventPersistence persistence = new MemoryEventPersistence();
        context.registerBean(EventPersistence.class, () -> persistence);
        return persistence;
    }

    private void listen(Consumer<Object> handler) {
        context.addApplicationListener(event -> {
            if (event instanceof PayloadApplicationEvent<?> payloadEvent) {
                handler.accept(payloadEvent.getPayload());
            }
        });
    }

    private record SampleEvent(String eventId, String content) implements PersistentEvent {

        @Override
        @NonNull
        public String getEventId() {
            return eventId;
        }
    }

    private record TransactionEvent(String eventId, String content) implements SpringTransactionEvent {

        @Override
        @NonNull
        public String getEventId() {
            return eventId;
        }
    }

    private enum PublicationMode {

        SYNCHRONOUS(SpringEventPublishUtils::publishEvent),

        ASYNCHRONOUS(SpringEventPublishUtils::publishAsync),

        TRANSACTIONAL(SpringEventPublishUtils::publishWithTransactionCommitOrImmediately);

        private final Consumer<Object> publish;

        PublicationMode(Consumer<Object> publish) {
            this.publish = publish;
        }
    }

    /**
     * 仅模拟存储端口的提交可见性，不代替宿主数据库的事务验证。
     */
    private static class MemoryEventPersistence implements EventPersistence {

        private final Map<String, PersistentEvent> events = new ConcurrentHashMap<>();

        private final Map<String, String> statuses = new ConcurrentHashMap<>();

        private final Map<String, Throwable> failures = new ConcurrentHashMap<>();

        private final Map<String, Thread> savingThreads = new ConcurrentHashMap<>();

        private final Map<String, Boolean> savingTransactions = new ConcurrentHashMap<>();

        private RuntimeException saveFailure;

        private RuntimeException successFailure;

        private RuntimeException failureWriteFailure;

        @Override
        public void save(@NonNull PersistentEvent event) {
            if (saveFailure != null) {
                throw saveFailure;
            }
            savingThreads.put(event.getEventId(), Thread.currentThread());
            savingTransactions.put(event.getEventId(), TransactionSynchronizationManager.isActualTransactionActive());
            Runnable save = () -> {
                events.putIfAbsent(event.getEventId(), event);
                statuses.putIfAbsent(event.getEventId(), "PENDING");
            };
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        save.run();
                    }
                });
            } else {
                save.run();
            }
        }

        @Override
        public void markSucceeded(@NonNull String eventId) {
            if (successFailure != null) {
                throw successFailure;
            }
            requireEvent(eventId);
            statuses.put(eventId, "SUCCEEDED");
        }

        @Override
        public void markFailed(@NonNull String eventId, @NonNull Throwable cause) {
            if (failureWriteFailure != null) {
                throw failureWriteFailure;
            }
            requireEvent(eventId);
            if (!"SUCCEEDED".equals(statuses.get(eventId))) {
                statuses.put(eventId, "FAILED");
                failures.put(eventId, cause);
            }
        }

        private void requireEvent(String eventId) {
            if (!events.containsKey(eventId)) {
                throw new IllegalStateException("事件尚未提交：" + eventId);
            }
        }
    }

    /**
     * 使用 Spring 真实事务生命周期触发提交和回滚回调，不持有数据库资源。
     */
    private static class TestTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        @NonNull
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(@NonNull Object transaction, @NonNull TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(@NonNull DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(@NonNull DefaultTransactionStatus status) {
        }
    }
}
