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
package org.apache.rocketmq.spring.annotation;

import lombok.Getter;
import lombok.Setter;
import org.apache.rocketmq.spring.support.RocketMQMessageListenerContainerRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.OrderComparator;
import org.springframework.core.annotation.AnnotationUtils;

import jakarta.validation.constraints.NotNull;
import java.lang.reflect.AnnotatedElement;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Getter
@Setter
public class RocketMQMessageListenerBeanPostProcessor implements ApplicationContextAware, BeanPostProcessor, InitializingBean, SmartLifecycle,
        ApplicationListener<ApplicationStartedEvent> {

    private static final Logger log = LoggerFactory.getLogger(RocketMQMessageListenerBeanPostProcessor.class);
    private ApplicationContext applicationContext;

    private AnnotationEnhancer enhancer;

    private RocketMQMessageListenerContainerRegistrar listenerContainerRegistrar;

    private boolean running = false;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        RocketMQMessageListener ann = targetClass.getAnnotation(RocketMQMessageListener.class);
        if (ann != null) {
            RocketMQMessageListener enhance = enhance(targetClass, ann);
            if (listenerContainerRegistrar != null) {
                listenerContainerRegistrar.registerContainer(beanName, bean, enhance);
            }
        }
        return bean;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 2000;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void onApplicationEvent(@NotNull ApplicationStartedEvent event) {
        // 应用启动后才开始监听
        if (!isRunning()) {
            log.info("start rocketmq message listen");
            this.setRunning(true);
            listenerContainerRegistrar.startContainer();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        buildEnhancer();
        this.listenerContainerRegistrar = this.applicationContext.getBean(RocketMQMessageListenerContainerRegistrar.class);
    }

    private void buildEnhancer() {
        if (this.applicationContext != null) {
            Map<String, AnnotationEnhancer> enhancersMap =
                    this.applicationContext.getBeansOfType(AnnotationEnhancer.class, false, false);
            if (!enhancersMap.isEmpty()) {
                List<AnnotationEnhancer> enhancers = enhancersMap.values()
                        .stream()
                        .sorted(new OrderComparator())
                        .collect(Collectors.toList());
                this.enhancer = (attrs, element) -> {
                    Map<String, Object> newAttrs = attrs;
                    for (AnnotationEnhancer enh : enhancers) {
                        newAttrs = enh.apply(newAttrs, element);
                    }
                    return attrs;
                };
            }
        }
    }

    private RocketMQMessageListener enhance(AnnotatedElement element, RocketMQMessageListener ann) {
        if (this.enhancer == null) {
            return ann;
        } else {
            return AnnotationUtils.synthesizeAnnotation(
                    this.enhancer.apply(AnnotationUtils.getAnnotationAttributes(ann), element), RocketMQMessageListener.class, null);
        }
    }

    public interface AnnotationEnhancer extends BiFunction<Map<String, Object>, AnnotatedElement, Map<String, Object>> {
    }
}
