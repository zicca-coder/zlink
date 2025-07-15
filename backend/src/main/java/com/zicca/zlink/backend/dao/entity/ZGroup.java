package com.zicca.zlink.backend.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zicca.zlink.backend.common.domain.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分组实体
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("z_group")
public class ZGroup extends BaseDO {

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 分组标识
     */
    @TableField(value = "gid")
    private String gid;

    /**
     * 分组名称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 分组创建用户（用户名唯一）
     */
    @TableField(value = "username")
    private String username;

    /**
     * 分组排序位次：用户创建多个分组，该分组展示在列表的位次
     */
    @TableField(value = "sort_order")
    private Integer sortOrder;

}
