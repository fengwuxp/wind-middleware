/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.spring.support;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.wind.common.WindConstants;
import com.wind.sentinel.SentinelResource;
import com.wind.sentinel.SentinelResourceBuilder;
import com.wind.sentinel.util.SentinelFlowLimitUtils;
import com.wind.trace.WindTracer;
import lombok.Data;
import org.apache.rocketmq.client.AccessChannel;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.MessageSelector;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.consumer.rebalance.AllocateMessageQueueAveragely;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.client.utils.MessageUtil;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQPushConsumerLifecycleListener;
import org.apache.rocketmq.spring.core.RocketMQReplyListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SmartMessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Data
public class DefaultRocketMQListenerContainer implements InitializingBean,
        RocketMQListenerContainer, SmartLifecycle, ApplicationContextAware {
    private final static Logger log = LoggerFactory.getLogger(DefaultRocketMQListenerContainer.class);

    private ApplicationContext applicationContext;

    /**
     * The name of the DefaultRocketMQListenerContainer instance
     */
    private String name;

    /**
     * Suspending pulling time in orderly mode.
     * <p>
     * The minimum value is 10 and the maximum is 30000.
     */
    private long suspendCurrentQueueTimeMillis = 1000;

    /**
     * Message consume retry strategy in concurrently mode.
     * <p>
     * -1,no retry,put into DLQ directly
     * 0,broker control retry frequency
     * >0,client control retry frequency
     */
    private int delayLevelWhenNextConsume = 0;

    private String nameServer;

    private AccessChannel accessChannel = AccessChannel.LOCAL;

    private String consumerGroup;

    private String topic;

    private int consumeThreadMax = 64;

    private int consumeThreadNumber = 20;

    private String charset = "UTF-8";

    private MessageConverter messageConverter;

    @SuppressWarnings("rawtypes")
    private RocketMQListener rocketMQListener;

    @SuppressWarnings("rawtypes")
    private RocketMQReplyListener rocketMQReplyListener;

    private RocketMQMessageListener rocketMQMessageListener;

    private DefaultMQPushConsumer consumer;

    private Type messageType;

    private MethodParameter methodParameter;

    private boolean running;

    // The following properties came from @RocketMQMessageListener.
    private ConsumeMode consumeMode;
    private SelectorType selectorType;
    private String selectorExpression;
    private MessageModel messageModel;
    private long consumeTimeout;
    private int maxReconsumeTimes;
    private int replyTimeout;
    private String tlsEnable;
    private String namespace;
    private long awaitTerminationMillisWhenShutdown;

    private String instanceName;

    private boolean enableFlowControl = false;


    public void setRocketMQMessageListener(RocketMQMessageListener annotation) {
        this.rocketMQMessageListener = annotation;
        this.consumeMode = annotation.consumeMode();
        this.consumeThreadMax = annotation.consumeThreadMax();
        this.consumeThreadNumber = annotation.consumeThreadNumber();
        this.messageModel = annotation.messageModel();
        this.selectorType = annotation.selectorType();
        this.selectorExpression = annotation.selectorExpression();
        this.consumeTimeout = annotation.consumeTimeout();
        this.maxReconsumeTimes = annotation.maxReconsumeTimes();
        this.replyTimeout = annotation.replyTimeout();
        this.tlsEnable = annotation.tlsEnable();
        this.namespace = annotation.namespace();
        this.delayLevelWhenNextConsume = annotation.delayLevelWhenNextConsume();
        this.suspendCurrentQueueTimeMillis = annotation.suspendCurrentQueueTimeMillis();
        this.awaitTerminationMillisWhenShutdown = Math.max(0, annotation.awaitTerminationMillisWhenShutdown());
        this.instanceName = annotation.instanceName();
    }


    public DefaultRocketMQListenerContainer setAwaitTerminationMillisWhenShutdown(long awaitTerminationMillisWhenShutdown) {
        this.awaitTerminationMillisWhenShutdown = awaitTerminationMillisWhenShutdown;
        return this;
    }

    @Override
    public void destroy() {
        this.setRunning(false);
        if (Objects.nonNull(consumer)) {
            consumer.shutdown();
        }
        log.info("container destroyed, {}", this.toString());
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public void start() {
        if (this.isRunning()) {
            throw new IllegalStateException("container already running. " + this.toString());
        }

        try {
            consumer.start();
        } catch (MQClientException e) {
            throw new IllegalStateException("Failed to start RocketMQ push consumer", e);
        }
        this.setRunning(true);

        log.info("running container: {}", this.toString());
    }

    @Override
    public void stop() {
        if (this.isRunning()) {
            if (Objects.nonNull(consumer)) {
                consumer.shutdown();
            }
            setRunning(false);
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void setRunning(boolean running) {
        this.running = running;
    }

    @Override
    public int getPhase() {
        // Returning Integer.MAX_VALUE only suggests that
        // we will be the first bean to shutdown and last bean to start
        return Integer.MAX_VALUE;
    }

    public DefaultRocketMQListenerContainer setMessageConverter(MessageConverter messageConverter) {
        this.messageConverter = messageConverter;
        return this;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.enableFlowControl = applicationContext.getBean(RocketMQProperties.class).isEnabledFlowControl();
        initRocketMQPushConsumer();

        this.messageType = getMessageType();
        this.methodParameter = getMethodParameter();
        log.debug("RocketMQ messageType: {}", messageType);
    }

    public class DefaultMessageListenerConcurrently implements MessageListenerConcurrently {
        @Override
        public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
            for (MessageExt message : msgs) {
                try {
                    handleMessage(message);
                } catch (Exception e) {
                    log.warn("consume message failed. messageId:{}, topic:{}, reconsumeTimes:{}", message.getMsgId(), message.getTopic(),
                            message.getReconsumeTimes(), e);
                    context.setDelayLevelWhenNextConsume(delayLevelWhenNextConsume);
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        }
    }

    public class DefaultMessageListenerOrderly implements MessageListenerOrderly {

        @Override
        public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs, ConsumeOrderlyContext context) {
            for (MessageExt messageExt : msgs) {
                try {
                    handleMessage(messageExt);
                } catch (Exception e) {
                    log.warn("consume message failed. messageId:{}, topic:{}, reconsumeTimes:{}", messageExt.getMsgId(), messageExt.getTopic(),
                            messageExt.getReconsumeTimes(), e);
                    context.setSuspendCurrentQueueTimeMillis(suspendCurrentQueueTimeMillis);
                    return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
                }
            }
            return ConsumeOrderlyStatus.SUCCESS;
        }
    }

    public void handleMessage(MessageExt messageExt) throws MQClientException, RemotingException, InterruptedException, BlockException {
        log.debug("received msg: {}", messageExt);
        boolean debugEnabled = log.isDebugEnabled();
        long now = debugEnabled ? System.currentTimeMillis() : -1;

        String traceId = messageExt.getUserProperty(WindConstants.TRACE_ID_NAME);
        boolean traceMessage = StringUtils.hasText(traceId);
        if (traceMessage) {
            // 设置 traceId
            WindTracer.TRACER.trace(traceId);
        }

        try {
            tryFlowControl(messageExt);
        } finally {
            if (traceMessage) {
                WindTracer.TRACER.clear();
            }
        }

        if (debugEnabled) {
            long costTime = System.currentTimeMillis() - now;
            log.debug("consume {} cost: {} ms", messageExt.getMsgId(), costTime);
        }
    }

    private void tryFlowControl(MessageExt messageExt) throws BlockException, MQClientException, RemotingException, InterruptedException {
        if (enableFlowControl) {
            SentinelResource resource = SentinelResourceBuilder.rocketConsumer()
                    .groupName(consumerGroup)
                    .topic(topic)
                    .tag(messageExt.getTags())
                    .build();
            Consumer<Throwable> throwableConsumer = SentinelFlowLimitUtils.limit(resource);
            Exception exception = null;
            try {
                dispatchMessage(messageExt);
            } catch (MQClientException | RemotingException | InterruptedException e) {
                exception = e;
                throw e;
            } finally {
                throwableConsumer.accept(exception);
            }
        } else {
            dispatchMessage(messageExt);
        }
    }

    @SuppressWarnings("unchecked")
    private void dispatchMessage(MessageExt messageExt) throws MQClientException, RemotingException, InterruptedException {
        if (rocketMQListener != null) {
            rocketMQListener.onMessage(doConvertMessage(messageExt));
        } else if (rocketMQReplyListener != null) {
            Object replyContent = rocketMQReplyListener.onMessage(doConvertMessage(messageExt));
            Message<?> message = MessageBuilder.withPayload(replyContent).build();

            org.apache.rocketmq.common.message.Message replyMessage = MessageUtil.createReplyMessage(messageExt, convertToBytes(message));
            DefaultMQProducer producer = consumer.getDefaultMQPushConsumerImpl().getmQClientFactory().getDefaultMQProducer();
            producer.setSendMsgTimeout(replyTimeout);
            producer.send(replyMessage, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    if (sendResult.getSendStatus() != SendStatus.SEND_OK) {
                        log.error("Consumer replies message failed. SendStatus: {}", sendResult.getSendStatus());
                    } else {
                        log.debug("Consumer replies message success.");
                    }
                }

                @Override
                public void onException(Throwable e) {
                    log.error("Consumer replies message failed. error: {}", e.getLocalizedMessage());
                }
            });
        }
    }

    private byte[] convertToBytes(Message<?> message) {
        Message<?> messageWithSerializedPayload = doConvert(message.getPayload(), message.getHeaders());
        Object payloadObj = messageWithSerializedPayload.getPayload();
        byte[] payloads;
        try {
            if (null == payloadObj) {
                throw new RuntimeException("the message cannot be empty");
            }
            if (payloadObj instanceof String) {
                payloads = ((String) payloadObj).getBytes(Charset.forName(charset));
            } else if (payloadObj instanceof byte[]) {
                payloads = (byte[]) messageWithSerializedPayload.getPayload();
            } else {
                String jsonObj = (String) this.messageConverter.fromMessage(messageWithSerializedPayload, payloadObj.getClass());
                if (null == jsonObj) {
                    throw new RuntimeException(String.format(
                            "empty after conversion [messageConverter:%s,payloadClass:%s,payloadObj:%s]",
                            this.messageConverter.getClass(), payloadObj.getClass(), payloadObj));
                }
                payloads = jsonObj.getBytes(Charset.forName(charset));
            }
        } catch (Exception e) {
            throw new RuntimeException("convert to bytes failed.", e);
        }
        return payloads;
    }

    private Message<?> doConvert(Object payload, MessageHeaders headers) {
        Message<?> message = this.messageConverter instanceof SmartMessageConverter ?
                ((SmartMessageConverter) this.messageConverter).toMessage(payload, headers, null) :
                this.messageConverter.toMessage(payload, headers);
        if (message == null) {
            String payloadType = payload.getClass().getName();
            Object contentType = headers != null ? headers.get(MessageHeaders.CONTENT_TYPE) : null;
            throw new MessageConversionException("Unable to convert payload with type='" + payloadType +
                    "', contentType='" + contentType + "', converter=[" + this.messageConverter + "]");
        }
        MessageBuilder<?> builder = MessageBuilder.fromMessage(message);
        builder.setHeaderIfAbsent(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN);
        return builder.build();
    }

    private Object doConvertMessage(MessageExt messageExt) {
        if (Objects.equals(messageType, MessageExt.class) || Objects.equals(messageType, org.apache.rocketmq.common.message.Message.class)) {
            return messageExt;
        } else {
            String str = new String(messageExt.getBody(), Charset.forName(charset));
            if (Objects.equals(messageType, String.class)) {
                return str;
            } else {
                // If msgType not string, use objectMapper change it.
                try {
                    if (messageType instanceof Class) {
                        //if the messageType has not Generic Parameter
                        return this.getMessageConverter().fromMessage(MessageBuilder.withPayload(str).build(), (Class<?>) messageType);
                    } else {
                        //if the messageType has Generic Parameter, then use SmartMessageConverter#fromMessage with third parameter "conversionHint".
                        //we have validate the MessageConverter is SmartMessageConverter in this#getMethodParameter.
                        return ((SmartMessageConverter) this.getMessageConverter()).fromMessage(MessageBuilder.withPayload(str).build(),
                                (Class<?>) ((ParameterizedType) messageType).getRawType(), methodParameter);
                    }
                } catch (Exception e) {
                    log.info("convert failed. str:{}, msgType:{}", str, messageType);
                    throw new RuntimeException("cannot convert message to " + messageType, e);
                }
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private MethodParameter getMethodParameter() {
        Class<?> targetClass;
        if (rocketMQListener != null) {
            targetClass = AopProxyUtils.ultimateTargetClass(rocketMQListener);
        } else {
            targetClass = AopProxyUtils.ultimateTargetClass(rocketMQReplyListener);
        }
        Type messageType = this.getMessageType();
        Class clazz = null;
        if (messageType instanceof ParameterizedType && messageConverter instanceof SmartMessageConverter) {
            clazz = (Class) ((ParameterizedType) messageType).getRawType();
        } else if (messageType instanceof Class) {
            clazz = (Class) messageType;
        } else {
            throw new RuntimeException("parameterType:" + messageType + " of onMessage method is not supported");
        }
        try {
            final Method method = targetClass.getMethod("onMessage", clazz);
            return new MethodParameter(method, 0);
        } catch (NoSuchMethodException e) {
            log.error("get mq listener MethodParameter error", e);
            throw new RuntimeException("parameterType:" + messageType + " of onMessage method is not supported");
        }
    }

    private Type getMessageType() {
        Class<?> targetClass;
        if (rocketMQListener != null) {
            targetClass = AopProxyUtils.ultimateTargetClass(rocketMQListener);
        } else {
            targetClass = AopProxyUtils.ultimateTargetClass(rocketMQReplyListener);
        }
        Type matchedGenericInterface = null;
        while (Objects.nonNull(targetClass)) {
            Type[] interfaces = targetClass.getGenericInterfaces();
            if (Objects.nonNull(interfaces)) {
                for (Type type : interfaces) {
                    if (type instanceof ParameterizedType &&
                            (Objects.equals(((ParameterizedType) type).getRawType(), RocketMQListener.class) || Objects.equals(((ParameterizedType) type).getRawType(), RocketMQReplyListener.class))) {
                        matchedGenericInterface = type;
                        break;
                    }
                }
            }
            targetClass = targetClass.getSuperclass();
        }
        if (Objects.isNull(matchedGenericInterface)) {
            return Object.class;
        }

        Type[] actualTypeArguments = ((ParameterizedType) matchedGenericInterface).getActualTypeArguments();
        if (Objects.nonNull(actualTypeArguments) && actualTypeArguments.length > 0) {
            return actualTypeArguments[0];
        }
        return Object.class;
    }

    private void initRocketMQPushConsumer() throws MQClientException {
        if (rocketMQListener == null && rocketMQReplyListener == null) {
            throw new IllegalArgumentException("Property 'rocketMQListener' or 'rocketMQReplyListener' is required");
        }
        Assert.notNull(consumerGroup, "Property 'consumerGroup' is required");
        Assert.notNull(nameServer, "Property 'nameServer' is required");
        Assert.notNull(topic, "Property 'topic' is required");

        RPCHook rpcHook = RocketMQUtil.getRPCHookByAkSk(applicationContext.getEnvironment(),
                this.rocketMQMessageListener.accessKey(), this.rocketMQMessageListener.secretKey());
        boolean enableMsgTrace = rocketMQMessageListener.enableMsgTrace();
        if (Objects.nonNull(rpcHook)) {
            consumer = new DefaultMQPushConsumer(consumerGroup, rpcHook, new AllocateMessageQueueAveragely(),
                    enableMsgTrace, this.applicationContext.getEnvironment().
                    resolveRequiredPlaceholders(this.rocketMQMessageListener.customizedTraceTopic()));
            consumer.setVipChannelEnabled(false);
        } else {
            log.debug("Access-key or secret-key not configure in " + this + ".");
            consumer = new DefaultMQPushConsumer(consumerGroup, enableMsgTrace,
                    this.applicationContext.getEnvironment().
                            resolveRequiredPlaceholders(this.rocketMQMessageListener.customizedTraceTopic()));
        }
        consumer.setNamespace(namespace);

        String customizedNameServer = this.applicationContext.getEnvironment().resolveRequiredPlaceholders(this.rocketMQMessageListener.nameServer());
        if (StringUtils.hasText(customizedNameServer)) {
            consumer.setNamesrvAddr(customizedNameServer);
        } else {
            consumer.setNamesrvAddr(nameServer);
        }
        if (accessChannel != null) {
            consumer.setAccessChannel(accessChannel);
        }

        consumer.setConsumeThreadMax(consumeThreadMax);
        consumer.setConsumeThreadMin(consumeThreadNumber);
        consumer.setConsumeTimeout(consumeTimeout);
        consumer.setMaxReconsumeTimes(maxReconsumeTimes);
        consumer.setAwaitTerminationMillisWhenShutdown(awaitTerminationMillisWhenShutdown);
        consumer.setInstanceName(instanceName);
        switch (messageModel) {
            case BROADCASTING:
                consumer.setMessageModel(org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel.BROADCASTING);
                break;
            case CLUSTERING:
                consumer.setMessageModel(org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel.CLUSTERING);
                break;
            default:
                throw new IllegalArgumentException("Property 'messageModel' was wrong.");
        }

        switch (selectorType) {
            case TAG:
                consumer.subscribe(topic, selectorExpression);
                break;
            case SQL92:
                consumer.subscribe(topic, MessageSelector.bySql(selectorExpression));
                break;
            default:
                throw new IllegalArgumentException("Property 'selectorType' was wrong.");
        }

        switch (consumeMode) {
            case ORDERLY:
                consumer.setMessageListener(new DefaultMessageListenerOrderly());
                break;
            case CONCURRENTLY:
                consumer.setMessageListener(new DefaultMessageListenerConcurrently());
                break;
            default:
                throw new IllegalArgumentException("Property 'consumeMode' was wrong.");
        }

        //if String is not is equal "true" TLS mode will represent the as default value false
        consumer.setUseTLS(Boolean.parseBoolean(tlsEnable));

        if (rocketMQListener instanceof RocketMQPushConsumerLifecycleListener) {
            ((RocketMQPushConsumerLifecycleListener) rocketMQListener).prepareStart(consumer);
        } else if (rocketMQReplyListener instanceof RocketMQPushConsumerLifecycleListener) {
            ((RocketMQPushConsumerLifecycleListener) rocketMQReplyListener).prepareStart(consumer);
        }

    }

}
