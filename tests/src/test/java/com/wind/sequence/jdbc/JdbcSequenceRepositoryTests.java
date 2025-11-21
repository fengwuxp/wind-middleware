package com.wind.sequence.jdbc;

import com.wind.AbstractJdbcTest;
import com.wind.sequence.SequenceGenerator;
import com.wind.sequence.SequenceRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author wuxp
 * @date 2024-03-31 10:41
 **/
@ContextConfiguration(classes = JdbcSequenceRepositoryTests.TestConfig.class)
@Slf4j
class JdbcSequenceRepositoryTests extends AbstractJdbcTest {

    @Autowired
    private SequenceRepository jdbcSequenceRepository;

    @Test
    void testSequence() {
        SequenceGenerator sequenceGenerator = jdbcSequenceRepository.getSequence("test");
        for (int i = 1; i < RandomUtils.secure().randomInt(1, 500); i++) {
            Assertions.assertEquals(i + "", sequenceGenerator.next());
        }
    }

    @Configuration
    static class TestConfig {

        @Bean
        public SequenceRepository jdbcSequenceRepository(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
            return new JdbcSequenceRepository(jdbcTemplate, transactionManager);
        }
    }
}
