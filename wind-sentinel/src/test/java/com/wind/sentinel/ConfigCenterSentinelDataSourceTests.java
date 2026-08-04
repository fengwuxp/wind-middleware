package com.wind.sentinel;

import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.wind.configcenter.core.ConfigRepository;
import com.wind.jackson.WindJson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wuxp
 * @date 2024-06-19 11:08
 **/
class ConfigCenterSentinelDataSourceTests {

    @Test
    void testLoadRuleConfig() {
        MockConfigRepository configRepository = new MockConfigRepository();
        ConfigRepository.ConfigDescriptor descriptor1 = ConfigRepository.ConfigDescriptor.immutable("t1", "t1");
        ConfigRepository.ConfigDescriptor descriptor2 = ConfigRepository.ConfigDescriptor.immutable("t2", "t2");
        ConfigRepository.ConfigDescriptor descriptor3 = ConfigRepository.ConfigDescriptor.immutable("t3", "t3");
        new ConfigCenterSentinelDataSource<FlowRule>(configRepository, Arrays.asList(descriptor1, descriptor2, descriptor3), FlowRule.class);

        List<FlowRule> rules = FlowRuleManager.getRules();
        Assertions.assertEquals(9, rules.size());
        configRepository.mockPushConfig(descriptor1, SentinelFlowTestUtils.mockFlowRules("t11", "t12"));
        List<FlowRule> rules2 = FlowRuleManager.getRules();
        Assertions.assertEquals(8, rules2.size());
        configRepository.mockPushConfig(descriptor2, SentinelFlowTestUtils.mockFlowRules("t21", "t22"));
        List<FlowRule> rules3 = FlowRuleManager.getRules();
        Assertions.assertEquals(7, rules3.size());
    }

    static class MockConfigRepository implements ConfigRepository {

        private final Map<ConfigDescriptor, TextConfigListener> listeners = new HashMap<>();

        private final Map<ConfigDescriptor, Object> mockConfigs = new HashMap<>();

        @Override
        public void saveTextConfig(ConfigDescriptor descriptor, String content) {

        }

        @Override
        public String getConfigSourceName() {
            return "MOCK";
        }

        @Override
        public ConfigSubscription onChange(ConfigDescriptor descriptor, TextConfigListener listener) {
            listeners.put(descriptor, listener);
            return new ConfigSubscription() {
                @Override
                public ConfigDescriptor getConfigDescriptor() {
                    return descriptor;
                }

                @Override
                public void unsubscribe() {
                    listeners.remove(descriptor);
                }
            };
        }

        @Override
        public String getTextConfig(ConfigDescriptor descriptor) {
            Object result = mockConfigs.get(descriptor);
            if (result == null) {
                String configId = descriptor.getConfigId();
                result = SentinelFlowTestUtils.mockFlowRules(configId, configId, configId);
            }
            return WindJson.toJsonString(result);
        }

        public void mockPushConfig(ConfigDescriptor descriptor, Object value) {
            mockConfigs.put(descriptor, value);
            listeners.get(descriptor).change(WindJson.toJsonString(value));
        }
    }
}
