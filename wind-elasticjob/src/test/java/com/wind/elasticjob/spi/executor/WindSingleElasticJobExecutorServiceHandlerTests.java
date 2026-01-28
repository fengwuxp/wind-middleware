package com.wind.elasticjob.spi.executor;

import com.wind.elasticjob.enums.ElasticJobExecutorServiceHandlerType;
import org.apache.shardingsphere.elasticjob.infra.handler.threadpool.JobExecutorServiceHandler;
import org.apache.shardingsphere.elasticjob.infra.handler.threadpool.JobExecutorServiceHandlerFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author wuxp
 * @date 2026-01-28 18:13
 **/
class WindSingleElasticJobExecutorServiceHandlerTests {

    @Test
    void testGetHandler() {
        JobExecutorServiceHandler handler = JobExecutorServiceHandlerFactory.getHandler(ElasticJobExecutorServiceHandlerType.SINGLE.name());
        Assertions.assertNotNull(handler);
        Assertions.assertInstanceOf(WindSingleElasticJobExecutorServiceHandler.class, handler);
    }
}
