package com.wind.logging.logback.kafaka;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.spi.AppenderAttachableImpl;
import com.wind.logging.logback.kafaka.delivery.FailedDeliveryCallback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.serialization.ByteArraySerializer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author wuxp
 * @since 0.0.1
 */
public class KafkaAppender<E> extends KafkaAppenderConfig<E> {

    private static final String KAFKA_APPENDER_ENABLED_KEY = "spring.logging.logback.kafka-appender.enabled";

    /**
     * Kafka clients uses this prefix for its slf4j logging.
     * This appender defers appends of any Kafka logs since it could cause harmful infinite recursion/self feeding effects.
     */
    private static final String KAFKA_LOGGER_PREFIX = KafkaProducer.class.getPackage().getName().replaceFirst("\\.producer$", "");

    private final boolean enabled = isEnabled();

    private LazyProducer lazyProducer = null;
    private final AppenderAttachableImpl<E> aai = new AppenderAttachableImpl<>();
    private final ConcurrentLinkedQueue<E> queue = new ConcurrentLinkedQueue<>();
    private final FailedDeliveryCallback<E> failedDeliveryCallback = (evt, throwable) -> aai.appendLoopOnAppenders(evt);

    public KafkaAppender() {
        // setting these as config values sidesteps an unnecessary warning (minor bug in KafkaProducer)
        addProducerConfigValue(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        addProducerConfigValue(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
    }

    @Override
    public void doAppend(E e) {
        if (!enabled) {
            return;
        }
        ensureDeferredAppends();
        if (e instanceof ILoggingEvent ev && ev.getLoggerName().startsWith(KAFKA_LOGGER_PREFIX)) {
            deferAppend(e);
        } else {
            super.doAppend(e);
        }
    }

    @Override
    public void start() {
        if (!enabled) {
            return;
        }
        // only error free appenders should be activated
        if (!checkPrerequisites()) {
            return;
        }

        if (partition != null && partition < 0) {
            partition = null;
        }

        lazyProducer = new LazyProducer();
        super.start();
    }

    @Override
    public void stop() {
        if (!enabled) {
            return;
        }
        super.stop();
        if (lazyProducer != null && lazyProducer.isInitialized()) {
            try {
                lazyProducer.get().close();
            } catch (KafkaException e) {
                this.addWarn("Failed to shut down kafka producer: " + e.getMessage(), e);
            }
            lazyProducer = null;
        }
    }

    @Override
    public void addAppender(Appender<E> newAppender) {
        if (!enabled) {
            return;
        }
        aai.addAppender(newAppender);
    }

    @Override
    public Iterator<Appender<E>> iteratorForAppenders() {
        return aai.iteratorForAppenders();
    }

    @Override
    public Appender<E> getAppender(String name) {
        return aai.getAppender(name);
    }

    @Override
    public boolean isAttached(Appender<E> appender) {
        return aai.isAttached(appender);
    }

    @Override
    public void detachAndStopAllAppenders() {
        aai.detachAndStopAllAppenders();
    }

    @Override
    public boolean detachAppender(Appender<E> appender) {
        return aai.detachAppender(appender);
    }

    @Override
    public boolean detachAppender(String name) {
        return aai.detachAppender(name);
    }

    @Override
    protected void append(E e) {
        if (!enabled) {
            return;
        }
        final byte[] payload = encoder.encode(e);
        final byte[] key = keyingStrategy.createKey(e);

        final Long timestamp = isAppendTimestamp() ? getTimestamp(e) : null;

        final ProducerRecord<byte[], byte[]> producerRecord = new ProducerRecord<>(topic, partition, timestamp, key, payload);

        final Producer<byte[], byte[]> producer = lazyProducer.get();
        if (producer != null) {
            deliveryStrategy.send(lazyProducer.get(), producerRecord, e, failedDeliveryCallback);
        } else {
            failedDeliveryCallback.onFailedDelivery(e, null);
        }
    }

    protected Long getTimestamp(E e) {
        if (e instanceof ILoggingEvent ev) {
            return ev.getTimeStamp();
        } else {
            return System.currentTimeMillis();
        }
    }

    protected Producer<byte[], byte[]> createProducer() {
        return new KafkaProducer<>(new HashMap<>(producerConfig));
    }

    private void deferAppend(E event) {
        queue.add(event);
    }

    // drains queue events to super
    private void ensureDeferredAppends() {
        E event;

        while ((event = queue.poll()) != null) {
            super.doAppend(event);
        }
    }

    /**
     * Lazy initializer for producer, patterned after commons-lang.
     *
     * @see
     * <a href="https://commons.apache.org/proper/commons-lang/javadocs/api-3.4/org/apache/commons/lang3/concurrent/LazyInitializer.html">LazyInitializer</a>
     */
    private class LazyProducer {

        private final AtomicReference<Producer<byte[], byte[]>> producer = new AtomicReference<>();

        public Producer<byte[], byte[]> get() {
            Producer<byte[], byte[]> result = this.producer.get();
            if (result == null) {
                synchronized (this) {
                    result = this.producer.get();
                    if (result == null) {
                        result = this.initialize();
                        this.producer.set(result);
                    }
                }
            }
            return result;
        }

        protected Producer<byte[], byte[]> initialize() {
            try {
                return createProducer();
            } catch (Exception e) {
                addError("error creating producer", e);
            }
            return null;
        }

        public boolean isInitialized() {
            return producer.get() != null;
        }
    }

    private static boolean isEnabled() {
        String property = System.getProperty(KAFKA_APPENDER_ENABLED_KEY);
        property = System.getProperty(KAFKA_APPENDER_ENABLED_KEY, property);
        return property == null ? Boolean.TRUE : Boolean.TRUE.toString().equals(property);
    }
}
