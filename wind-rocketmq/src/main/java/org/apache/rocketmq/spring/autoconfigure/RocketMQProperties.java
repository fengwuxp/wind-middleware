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

package org.apache.rocketmq.spring.autoconfigure;

import lombok.Data;
import org.apache.rocketmq.common.topic.TopicValidator;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;


@ConfigurationProperties(prefix = RocketMQProperties.PREFIX)
@Data
public class RocketMQProperties {

    public static final String PREFIX = "spring.rocketmq";

    /**
     * The name server for rocketMQ, formats: `host:port;host:port`.
     */
    private String nameServer;

    /**
     * Enum type for accessChannel, values: LOCAL, CLOUD
     */
    private String accessChannel;

    private Producer producer;

    /**
     * for pull consumer only
     *
     * @see org.apache.rocketmq.spring.annotation.RocketMQMessageListener for a push consumer
     */
    private PullConsumer pullConsumer = new PullConsumer();
    /**
     * Configure enable listener or not.
     * In some particular cases, if you don't want the the listener is enabled when container startup,
     * the configuration pattern is like this :
     * rocketmq.push-consumer.listeners.<group-name>.<topic-name>.enabled=<boolean value, true or false>
     * <p>
     * the listener is enabled by default.
     */
    private PushConsumer consumer = new PushConsumer();

    /**
     * enable flow control
     */
    private boolean enabledFlowControl;

    @Data
    public static class Producer {

        /**
         * Group name of producer.
         */
        private String group;

        /**
         * Namespace for this MQ Producer instance.
         */
        private String namespace;

        /**
         * Millis of send message timeout.
         */
        private int sendMessageTimeout = 3000;

        /**
         * Compress message body threshold, namely, message body larger than 4k will be compressed on default.
         */
        private int compressMessageBodyThreshold = 1024 * 4;

        /**
         * Maximum number of retry to perform internally before claiming sending failure in synchronous mode.
         * This may potentially cause message duplication which is up to application developers to resolve.
         */
        private int retryTimesWhenSendFailed = 2;

        /**
         * <p> Maximum number of retry to perform internally before claiming sending failure in asynchronous mode. </p>
         * This may potentially cause message duplication which is up to application developers to resolve.
         */
        private int retryTimesWhenSendAsyncFailed = 2;

        /**
         * Indicate whether to retry another broker on sending failure internally.
         */
        private boolean retryNextServer = false;

        /**
         * Maximum allowed message size in bytes.
         */
        private int maxMessageSize = 1024 * 1024 * 4;

        /**
         * The property of "access-key".
         */
        private String accessKey;

        /**
         * The property of "secret-key".
         */
        private String secretKey;

        /**
         * Switch flag instance for message trace.
         */
        private boolean enableMsgTrace = false;

        /**
         * The name value of message trace topic.If you don't config,you can use the default trace topic name.
         */
        private String customizedTraceTopic = TopicValidator.RMQ_SYS_TRACE_TOPIC;

        /**
         * The property of "tlsEnable".
         */
        private boolean tlsEnable = false;

        /**
         * The property of "instanceName".
         */
        private String instanceName = "DEFAULT";
    }

    @Data
    public static class PullConsumer {
        /**
         * Group name of consumer.
         */
        private String group;

        /**
         * Namespace for this MQ Consumer instance.
         */
        private String namespace;

        /**
         * Topic name of consumer.
         */
        private String topic;

        /**
         * Control message mode, if you want all subscribers receive message all message, broadcasting is a good choice.
         */
        private String messageModel = "CLUSTERING";

        /**
         * Control how to selector message.
         */
        private String selectorType = "TAG";

        /**
         * Control which message can be select.
         */
        private String selectorExpression = "*";

        /**
         * The property of "access-key".
         */
        private String accessKey;

        /**
         * The property of "secret-key".
         */
        private String secretKey;

        /**
         * Maximum number of messages pulled each time.
         */
        private int pullBatchSize = 10;

        /**
         * Switch flag instance for message trace.
         */
        private boolean enableMsgTrace = false;

        /**
         * The name value of message trace topic.If you don't config,you can use the default trace topic name.
         */
        private String customizedTraceTopic = TopicValidator.RMQ_SYS_TRACE_TOPIC;

        /**
         * The property of "tlsEnable".
         */
        private boolean tlsEnable = false;

        /**
         * The property of "instanceName".
         */
        private String instanceName = "DEFAULT";

    }

    public static class PushConsumer extends PullConsumer {
        /**
         * listener configuration container
         * the pattern is like this:
         * group1.topic1 = false
         * group2.topic2 = true
         * group3.topic3 = false
         */
        private Map<String, Map<String, Boolean>> listeners = new HashMap<>();

        public Map<String, Map<String, Boolean>> getListeners() {
            return listeners;
        }

        public void setListeners(Map<String, Map<String, Boolean>> listeners) {
            this.listeners = listeners;
        }
    }

}