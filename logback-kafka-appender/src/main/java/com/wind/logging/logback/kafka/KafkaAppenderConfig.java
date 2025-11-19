package com.wind.logging.logback.kafka;

import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.spi.AppenderAttachable;
import com.wind.logging.logback.kafka.delivery.AsynchronousDeliveryStrategy;
import com.wind.logging.logback.kafka.delivery.DeliveryStrategy;
import com.wind.logging.logback.kafka.keying.KeyingStrategy;
import com.wind.logging.logback.kafka.keying.NoKeyKeyingStrategy;

import java.util.HashMap;
import java.util.Map;

import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;

/**
 * @since 0.0.1
 */
public abstract class KafkaAppenderConfig<E> extends UnsynchronizedAppenderBase<E> implements AppenderAttachable<E> {

    private static final String SERVERS_NAME = "spring.kafka.log.bootstrap-servers";
    private static final String TOPIC_NAME = "spring.kafka.log.topic";

    protected String topic = null;

    protected Encoder<E> encoder = null;

    protected KeyingStrategy<? super E> keyingStrategy = null;

    protected DeliveryStrategy deliveryStrategy;

    protected Integer partition = null;

    protected boolean appendTimestamp = true;

    protected Map<String, Object> producerConfig = new HashMap<String, Object>();

    protected boolean checkPrerequisites() {
        boolean errorFree = true;

        if (producerConfig.get(BOOTSTRAP_SERVERS_CONFIG) == null) {
            producerConfig.put(BOOTSTRAP_SERVERS_CONFIG, getSystemConfig(SERVERS_NAME));
            addError("No \"" + BOOTSTRAP_SERVERS_CONFIG + "\" set for the appender named [\""
                    + name + "\"].");
            errorFree = false;
        }

        if (topic == null || topic.trim().isEmpty()) {
            topic = getSystemConfig(TOPIC_NAME);
            addError("No topic set for the appender named [\"" + name + "\"].");
            errorFree = false;
        }

        if (encoder == null) {
            addError("No encoder set for the appender named [\"" + name + "\"].");
            errorFree = false;
        }

        if (keyingStrategy == null) {
            addInfo("No explicit keyingStrategy set for the appender named [\"" + name + "\"]. Using default NoKeyKeyingStrategy.");
            keyingStrategy = new NoKeyKeyingStrategy();
        }

        if (deliveryStrategy == null) {
            addInfo("No explicit deliveryStrategy set for the appender named [\"" + name + "\"]. Using default asynchronous strategy.");
            deliveryStrategy = new AsynchronousDeliveryStrategy();
        }

        return errorFree;
    }

    public void setEncoder(Encoder<E> encoder) {
        this.encoder = encoder;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setKeyingStrategy(KeyingStrategy<? super E> keyingStrategy) {
        this.keyingStrategy = keyingStrategy;
    }

    public void addProducerConfig(String keyValue) {
        String[] split = keyValue.split("=", 2);
        if (split.length == 2) {
            addProducerConfigValue(split[0], split[1]);
        }
    }

    public void addProducerConfigValue(String key, Object value) {
        this.producerConfig.put(key, value);
    }

    public Map<String, Object> getProducerConfig() {
        return producerConfig;
    }

    public void setDeliveryStrategy(DeliveryStrategy deliveryStrategy) {
        this.deliveryStrategy = deliveryStrategy;
    }

    public void setPartition(Integer partition) {
        this.partition = partition;
    }

    public boolean isAppendTimestamp() {
        return appendTimestamp;
    }

    public void setAppendTimestamp(boolean appendTimestamp) {
        this.appendTimestamp = appendTimestamp;
    }

    private String getSystemConfig(String name) {
        String property = System.getProperty(name);
        if (property == null) {
            return System.getenv(name);
        }
        return property;
    }
}
