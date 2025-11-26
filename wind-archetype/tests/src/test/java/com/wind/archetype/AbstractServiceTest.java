package com.wind.archetype;


import com.wind.common.spring.SpringApplicationContextUtils;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

/**
 * 参考文档说明：
 * https://www.yuque.com/suiyuerufeng-akjad/ekxk67/ohnwkqez799ows8b
 * https://juejin.cn/post/7039606720343048206
 *
 * @author wind
 */
@SpringJUnitConfig()
@Import({AbstractServiceTest.TestConfig.class})
@ImportAutoConfiguration(value = {AbstractServiceTest.H2InitializationAutoConfiguration.class})
@Transactional(rollbackFor = Exception.class)
@TestPropertySource(locations = {"classpath:application-h2.properties", "classpath:application-test.properties"})
@EnableAspectJAutoProxy(proxyTargetClass = true)
public abstract class AbstractServiceTest {

    @Configuration
    @Import({SpringApplicationContextUtils.class})
    static class TestConfig {


    }

    /**
     * 初始化 h2 不支持的 MYSQL 函数
     */
    @AllArgsConstructor
    @AutoConfiguration
    public static class H2InitializationAutoConfiguration {


    }
}
