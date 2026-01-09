package com.wind.elasticjob.job;

import org.apache.shardingsphere.elasticjob.dataflow.job.DataflowJob;

/**
 * wind elastic job 数据流任务
 *
 * @author wuxp
 * @date 2024-12-15 18:33
 **/
public interface WindElasticDataFlowJob<T> extends WindElasticJob, DataflowJob<T> {

}
