package com.zicca.zlink.framework.mapper;


import com.zicca.zlink.framework.entity.IdempotentRecord;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.mapping.StatementType;
import org.springframework.data.repository.query.Param;


@Mapper
public interface IdempotentRecordMapper {

    @Select("select status from mq_idempotent_record where msg_key = #{msgKey}")
    Integer selectStatus(@Param("msgKey") String msgKey);

    @Update("update mq_idempotent_record set status = #{status}, update_time = NOW() where msg_key = #{msgKey}")
    int updateStatus(@Param("msgKey") String msgKey, @Param("status") Integer status);

    @Delete("delete from mq_idempotent_record where msg_key = #{msgKey}")
    int deleteByKey(@Param("msgKey") String msgKey);

    /**
     * 使用存储过程实现原子操作
     * 返回值：1=首次消费, 0=消费中, 2=已消费
     */
    @Select("CALL sp_try_insert_idempotent_key(#{msgKey})")
    @Options(statementType = StatementType.CALLABLE)
    Integer tryInsertKeyAtomic(@Param("msgKey") String msgKey);

    /**
     * 降级方案：使用 INSERT IGNORE 实现原子插入
     */
    @Insert("INSERT IGNORE INTO mq_idempotent_record (msg_key, status, create_time, update_time) VALUES (#{msgKey}, 0, NOW(), NOW())")
    int insertIgnore(@Param("msgKey") String msgKey);

    /**
     * 分批清理过期记录，避免大面积锁表
     * 使用 LIMIT 限制每次删除的数量
     * 注意建立(status, uptade_time)联合索引，避免行锁升级为表锁
     */
    @Delete("DELETE FROM mq_idempotent_record WHERE status = 1 AND update_time < DATE_SUB(NOW(), INTERVAL #{hours} HOUR) LIMIT #{batchSize}")
    int deleteExpiredRecordsBatch(@Param("hours") int hours, @Param("batchSize") int batchSize);

}
