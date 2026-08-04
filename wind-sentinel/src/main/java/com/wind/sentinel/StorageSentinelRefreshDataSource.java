package com.wind.sentinel;

import com.alibaba.csp.sentinel.datasource.AutoRefreshDataSource;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.system.SystemRule;
import com.wind.jackson.WindJson;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Supplier;

/**
 * 基于存储的数据源，例如：数据库
 * 参见：https://github.com/alibaba/Sentinel/wiki/%E5%8A%A8%E6%80%81%E8%A7%84%E5%88%99%E6%89%A9%E5%B1%95
 *
 * @author wuxp
 * @date 2024-03-07 16:20
 **/
@Slf4j
public class StorageSentinelRefreshDataSource<T> extends AutoRefreshDataSource<String, List<T>> {

    private final Supplier<String> configRepository;

    public StorageSentinelRefreshDataSource(Supplier<String> configRepository, Class<T> configType) {
        this(configRepository, configType, 5 * 60 * 1000L);
    }

    public StorageSentinelRefreshDataSource(Supplier<String> configRepository, Class<T> configType, long recommendRefreshMs) {
        super(source -> WindJson.parseArray(source, configType), recommendRefreshMs);
        this.configRepository = configRepository;
        SentinelRuleListenRegister.registerListen(configType, this);
        initConfig();
    }

    public static StorageSentinelRefreshDataSource<FlowRule> flow(Supplier<String> configRepository) {
        return new StorageSentinelRefreshDataSource<>(configRepository, FlowRule.class);
    }

    public static StorageSentinelRefreshDataSource<DegradeRule> degrade(Supplier<String> configRepository) {
        return new StorageSentinelRefreshDataSource<>(configRepository, DegradeRule.class);
    }

    public static StorageSentinelRefreshDataSource<ParamFlowRule> param(Supplier<String> configRepository) {
        return new StorageSentinelRefreshDataSource<>(configRepository, ParamFlowRule.class);
    }

    public static StorageSentinelRefreshDataSource<SystemRule> system(Supplier<String> configRepository) {
        return new StorageSentinelRefreshDataSource<>(configRepository, SystemRule.class);
    }

    public static StorageSentinelRefreshDataSource<AuthorityRule> authority(Supplier<String> configRepository) {
        return new StorageSentinelRefreshDataSource<>(configRepository, AuthorityRule.class);
    }

    @Override
    public String readSource() throws Exception {
        return configRepository.get();
    }

    private void initConfig() {
        try {
            List<T> newValue = loadConfig();
            if (newValue == null) {
                log.warn("load sentinel config is null");
                return;
            }
            getProperty().updateValue(newValue);
        } catch (Throwable throwable) {
            log.error("load sentinel config exception", throwable);
        }
    }
}
