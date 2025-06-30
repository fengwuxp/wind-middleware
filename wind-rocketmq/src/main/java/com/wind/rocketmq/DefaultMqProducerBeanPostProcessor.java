package com.wind.rocketmq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.hook.SendMessageHook;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.lang.NonNull;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 给 {@link DefaultMQProducer} 增加 trace 的 {@link SendMessageHook}
 *
 * @author wuxp
 * @date 2024-06-17 15:37
 **/
@Slf4j
public class DefaultMqProducerBeanPostProcessor implements BeanPostProcessor {

    private final AtomicReference<Object> producerRef = new AtomicReference<>();

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        if (producerRef.get() != bean && bean instanceof DefaultMQProducer producer) {
            producerRef.set(bean);
            producer.getDefaultMQProducerImpl().registerSendMessageHook(new SendMessageTraceHook());
            log.info("register send message hook：SendMessageTraceHook");
        }
        return bean;
    }
}
