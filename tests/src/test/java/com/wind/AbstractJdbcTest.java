package com.wind;

/**
 * @author wuxp
 * @date 2024-03-31 10:42
 **/

import com.wind.tools.h2.H2FunctionInitializer;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

/**
 * 参考文档说明：
 * https://www.yuque.com/suiyuerufeng-akjad/ekxk67/ohnwkqez799ows8b
 * https://juejin.cn/post/7039606720343048206
 *
 * @author wuxp
 */
@SpringJUnitConfig()
@Import({AbstractJdbcTest.TestConfig.class})
@ImportAutoConfiguration(value = {
        AbstractJdbcTest.H2InitializationAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        SqlInitializationAutoConfiguration.class,
})
@Transactional(rollbackFor = Exception.class)
@TestPropertySource(locations = {"classpath:application-h2.properties", "classpath:application-test.properties"})
@EnableAspectJAutoProxy(proxyTargetClass = true)
public abstract class AbstractJdbcTest {

    @Configuration
    static class TestConfig {

    }

    /**
     * 初始化 h2 不支持的 MYSQL 函数
     */
    @AllArgsConstructor
    @AutoConfiguration
    @AutoConfigureBefore(SqlInitializationAutoConfiguration.class)
    @EnableConfigurationProperties(DataSourceProperties.class)
    public static class H2InitializationAutoConfiguration {

        @Bean
        public DataSource dataSource(DataSourceProperties properties) {
            properties.setType(HikariDataSource.class);
            DataSource result = properties.initializeDataSourceBuilder().build();
            H2FunctionInitializer.initialize(result);
            return result;
        }

    }

}

