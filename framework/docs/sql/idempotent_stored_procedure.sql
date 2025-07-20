-- 表结构及索引
-- 改进的幂等记录表结构
CREATE TABLE `mq_idempotent_record`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `msg_key`     varchar(255) NOT NULL COMMENT '消息唯一标识',
    `status`      tinyint      NOT NULL DEFAULT '0' COMMENT '消费状态：0-消费中，1-已消费',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_msg_key` (`msg_key`),
    KEY           `idx_status_update` (`status`, `update_time`),
    KEY           `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MQ消息幂等记录表';


-- 幂等性原子操作存储过程
-- 用于实现高性能的幂等检查和插入
-- 根据传入的msg_key，判断该消息是否为首次消费、正在消费中、还是已经消费完成，并返回对应的状态
-- 如果消息不存在：插入新纪录，并标记为“首次消费”
-- 如果消息存在且状态为“消费中”：返回“消费中”
-- 如果消息存在且状态为“已消费”：返回“已消费”

DELIMITER
$$

DROP PROCEDURE IF EXISTS sp_try_insert_idempotent_key$$

CREATE PROCEDURE sp_try_insert_idempotent_key(
    IN p_msg_key VARCHAR (255)
)
BEGIN
DECLARE v_status INT DEFAULT NULL; -- 变量声明：v_status，用于存储当前消息在数据库中的状态（0=消费中，1=已消费）
DECLARE v_result INT DEFAULT 0; -- 变量声明：v_result，用于返回结果（1=首次消费，0=消费中，2=已消费）

-- 定义异常处理，如果执行过程中发生异常（如死锁、唯一键冲突等），设置返回值为-1，标识出错
-- 注意：不在存储过程内部管理事务，让外部事务控制
DECLARE EXIT HANDLER FOR SQLEXCEPTION
BEGIN
    SET v_result = -1;
    SELECT v_result as result;
END;

-- 尝试查询现有记录
SELECT status INTO v_status
FROM mq_idempotent_record
WHERE msg_key = p_msg_key FOR UPDATE; -- 加锁，防止并发操作

-- status只有0、1、未知三种状态
-- result只有0、1、2三种状态
IF v_status IS NULL THEN
        -- 记录不存在，插入新记录，设置status=0
        INSERT INTO mq_idempotent_record (msg_key, status, create_time, update_time)
        VALUES (p_msg_key, 0, NOW(), NOW());
        SET v_result = 1; -- 首次消费
ELSEIF v_status = 0 THEN
        SET v_result = 0; -- 消费中
ELSEIF v_status = 1 THEN
        SET v_result = 2; -- 已消费
ELSE
        SET v_result = 0; -- 未知状态，当作消费中处理
END IF;

-- 返回结果（不在存储过程内提交事务，由外部事务管理）
-- 1: 首次消费（FIRST_CONSUME）
-- 0: 消费中（CONSUMING）
-- 2: 已消费（ALREADY_CONSUMED）
SELECT v_result as result;

END$$

DELIMITER ;
