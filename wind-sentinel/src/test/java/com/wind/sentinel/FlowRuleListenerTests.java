package com.wind.sentinel;

import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.fastjson2.JSON;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author wuxp
 * @date 2024-03-11 15:41
 **/
class FlowRuleListenerTests {

    private final List<FlowRule> rules = new ArrayList<>();

    private final AtomicBoolean ready = new AtomicBoolean(false);

    private final StorageSentinelRefreshDataSource<FlowRule> dataSource = new StorageSentinelRefreshDataSource<>(() -> {
        if (ready.get()) {
            FlowRule second = new FlowRule();
            second.setResource("/a/b/c");
            second.setCount(200);
            rules.add(second);
        }
        if (rules.isEmpty()) {
            FlowRule first = new FlowRule();
            first.setResource("/a/b/c");
            first.setCount(100);
            rules.add(first);
        }
        return JSON.toJSONString(rules);

    }, FlowRule.class, 100);


    @Test
    void testReadSource() throws Exception {
        String text = dataSource.readSource();
        Assertions.assertEquals("[{\"clusterMode\":false,\"controlBehavior\":0,\"count\":100.0,\"grade\":1,\"limitApp\":\"default\"," +
                "\"maxQueueingTimeMs\":500,\"regex\":false,\"resource\":\"/a/b/c\",\"strategy\":0,\"warmUpPeriodSec\":10}]", text);
    }

    @Test
    void testLoadAndGetRules() {
        Assertions.assertEquals(1, FlowRuleManager.getRules().size());
        ready.set(true);
        await().atMost(200, TimeUnit.MILLISECONDS).until(() -> FlowRuleManager.getRules().size(), equalTo(2));
    }
}
