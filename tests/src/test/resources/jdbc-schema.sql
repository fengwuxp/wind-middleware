-- ------------------------------------
-- 全局序列号
-- ------------------------------------
drop table if exists `t_wind_sequence`;
create table `t_wind_sequence`
(
    `id`           bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `gmt_create`   datetime                     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` datetime                     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `name`         varchar(50)         NOT NULL COMMENT 'seq 名称',
    `group_name`   varchar(30)         NOT NULL DEFAULT 'default' COMMENT '分组名称',
    `seq_value`    bigint(20) unsigned NOT NULL DEFAULT 0 COMMENT 'seq值',
    `step`         tinyint(5) unsigned NOT NULL DEFAULT 1 COMMENT '步长',
    `version`      int(11)             NOT NULL DEFAULT 0 COMMENT '数据版本控制',
    primary key (`id`),
    unique `uk_wind_sequence_name` (`name`)
) COMMENT = '全局序列号表' ENGINE = InnoDB default charset = utf8mb4;