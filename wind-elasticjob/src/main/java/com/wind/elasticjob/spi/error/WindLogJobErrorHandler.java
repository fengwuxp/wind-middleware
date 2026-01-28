package com.wind.elasticjob.spi.error;

import com.wind.elasticjob.enums.ElasticJobErrorHandlerType;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.elasticjob.error.handler.JobErrorHandler;

import java.util.Properties;

/**
 * 自定义 log job error handler，用于覆盖默认的 {@link org.apache.shardingsphere.elasticjob.error.handler.general.LogJobErrorHandler}
 *
 * @author wuxp
 * @date 2025-10-14 13:57
 **/
@Slf4j
public class WindLogJobErrorHandler implements JobErrorHandler {

    @Override
    public void handleException(final String jobName, final Throwable cause) {
        log.error("{} job execute exception, message = {}", jobName, cause.getMessage(), cause);
    }

    @Override
    public void init(Properties props) {
        log.info("WindLogJobErrorHandler initialized");
    }

    @Override
    public String getType() {
        return ElasticJobErrorHandlerType.LOG.name();
    }
}
