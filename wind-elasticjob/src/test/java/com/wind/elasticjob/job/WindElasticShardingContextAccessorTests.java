package com.wind.elasticjob.job;

import org.apache.shardingsphere.elasticjob.api.ShardingContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class WindElasticShardingContextAccessorTests {

    @Test
    void testPreserveUntypedDecimalAsBigDecimal() {
        ShardingContext context = new ShardingContext(
                "example-job", "task-1", 1, "{\"amount\":1.25}", 0, "{\"amount\":2.50}");
        WindElasticShardingContextAccessor accessor = WindElasticShardingContextAccessor.of(context);

        Object jobAmount = accessor.getJobParameters().get("amount");
        Object shardingAmount = accessor.getJobShardingParameters().get("amount");

        assertInstanceOf(BigDecimal.class, jobAmount);
        assertInstanceOf(BigDecimal.class, shardingAmount);
        assertEquals(new BigDecimal("1.25"), jobAmount);
        assertEquals(new BigDecimal("2.50"), shardingAmount);
    }
}
