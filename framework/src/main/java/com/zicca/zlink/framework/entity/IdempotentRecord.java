package com.zicca.zlink.framework.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("mq_idempotent_record")
public class IdempotentRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "msg_key")
    private String msgKey;

    @TableField(value = "status")
    private Integer status; // 0:消费中, 1:已消费

    @TableField(value = "create_time")
    private Date createTime;

    @TableField(value = "update_time")
    private Date updateTime;
}
