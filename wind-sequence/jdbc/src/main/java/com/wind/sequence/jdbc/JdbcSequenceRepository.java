package com.wind.sequence.jdbc;

import com.wind.common.exception.AssertUtils;
import com.wind.sequence.SequenceGenerator;
import com.wind.sequence.SequenceRepository;
import lombok.Data;
import lombok.Getter;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * 基于数据库的序列号存储
 *
 * @author wuxp
 * @date 2024-03-31 09:41
 **/
public class JdbcSequenceRepository implements SequenceRepository {

    private static final SequenceSql DEFAULT_SQL = new SequenceSql(
            "insert into `%s`(`name`,`group_name`,`seq_value`,`step`) values (?, ?, 0, 1)",
            "select id from `%s` where name = ?",
            "select seq_value as seqValue, step as stepValue from `%s` where id = ?",
            "update `%s` set seq_value = seq_value + step where id = ? and seq_value = ?"
    );

    private final SequenceSql sequenceSql;

    private final JdbcTemplate jdbcTemplate;

    private final TransactionTemplate transactionTemplate;

    public JdbcSequenceRepository(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
        this(DEFAULT_SQL, jdbcTemplate, transactionManager);
    }

    public JdbcSequenceRepository(SequenceSql sequenceSql, JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
        this(sequenceSql, jdbcTemplate, transactionManager, TransactionDefinition.PROPAGATION_REQUIRED);
    }

    public JdbcSequenceRepository(SequenceSql sequenceSql, JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager,
                                  int propagationBehavior) {
        this.sequenceSql = sequenceSql;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(propagationBehavior));
    }


    @Override
    public SequenceGenerator getSequence(String sequenceName, String groupName) {
        return new JdbcSequenceGenerator(sequenceName, groupName);
    }

    @Getter
    public static class SequenceSql {

        /**
         * 创建序列sql
         */
        private final String create;

        /**
         * 获取序列的主键
         */
        private final String findId;

        /**
         * 查询序列sql
         */
        private final String querySeqValue;

        /**
         * 获取下一个序列sql
         */
        private final String next;

        public SequenceSql(String create, String findId, String querySeqValue, String next) {
            this("t_wind_sequence", create, findId, querySeqValue, next);
        }

        public SequenceSql(String tableName, String create, String findId, String querySeqValue, String next) {
            this.create = String.format(create, tableName);
            this.findId = String.format(findId, tableName);
            this.querySeqValue = String.format(querySeqValue, tableName);
            this.next = String.format(next, tableName);
        }

    }

    class JdbcSequenceGenerator implements SequenceGenerator {

        private final String sequenceName;

        private final Long sequenceId;

        public JdbcSequenceGenerator(String sequenceName, String groupName) {
            this.sequenceName = sequenceName;
            this.sequenceId = transactionTemplate.execute(status -> {
                List<Long> ids = jdbcTemplate.queryForList(sequenceSql.findId, Long.class, sequenceName);
                if (ids.isEmpty()) {
                    // 创建序列
                    jdbcTemplate.update(sequenceSql.create, sequenceName, groupName);
                    Long result = jdbcTemplate.queryForObject(sequenceSql.findId, Long.class, sequenceName);
                    AssertUtils.notNull(result, String.format("find sequence name = %s id failure", sequenceName));
                    return result;
                }
                return ids.getFirst();
            });
        }

        @Override
        public String next() {
            Integer next = transactionTemplate.execute(transactionStatus -> {
                SequenceValue current = jdbcTemplate.queryForObject(sequenceSql.querySeqValue,
                        new BeanPropertyRowMapper<>(SequenceValue.class), sequenceId);
                AssertUtils.notNull(current, () -> "not found current sequence value");
                AssertUtils.isTrue(jdbcTemplate.update(sequenceSql.next, sequenceId, current.seqValue) > 0,
                        () -> String.format("update sequence name = %s failure", sequenceName));
                return current.seqValue + current.stepValue;
            });
            AssertUtils.notNull(next, () -> String.format("get sequence name = %s failure", sequenceName));
            return String.valueOf(next);
        }
    }

    @Data
    static class SequenceValue {

        private Integer seqValue;

        private Integer stepValue;
    }
}
