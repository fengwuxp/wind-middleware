package com.wind.elasticjob.error.handler;

import com.wind.elasticjob.enums.ElasticJobErrorHandlerType;
import org.apache.shardingsphere.elasticjob.error.handler.JobErrorHandler;
import org.apache.shardingsphere.elasticjob.error.handler.JobErrorHandlerFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Properties;

/**
 * @author wuxp
 * @date 2025-10-16 16:50
 **/
class WindLogJobErrorHandlerTests {


    @Test
    void testCreateHandler() {
        Optional<JobErrorHandler> optional = JobErrorHandlerFactory.createHandler(ElasticJobErrorHandlerType.WIND_LOG.name(), new Properties());
        Assertions.assertTrue(optional.isPresent());
    }
}
