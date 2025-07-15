package com.zicca.zlink.backend.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zicca.zlink.backend.common.domain.BaseDO;
import com.zicca.zlink.backend.common.enums.CreateTypeEnum;
import com.zicca.zlink.backend.common.enums.EnableStatusEnum;
import com.zicca.zlink.backend.common.enums.ValidDateTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * ZLink实体类
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("z_link")
public class ZLink extends BaseDO {

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 域名
     */
    @TableField(value = "domain")
    private String domain;

    /**
     * 短链接 uri
     */
    @TableField(value = "short_uri")
    private String shortUri;

    /**
     * 短链接 url
     */
    @TableField(value = "short_url")
    private String shortUrl;

    /**
     * 原始 url
     */
    @TableField(value = "origin_url")
    private String originUrl;

    /**
     * 分组标识
     */
    @TableField(value = "gid")
    private String gid;

    /**
     * 点击数
     */
    @TableField(value = "click_num", fill = FieldFill.INSERT)
    private Integer clickNum;

    /**
     * 启用状态：0-启用，1-禁用
     */
    @TableField(value = "enable_status", fill = FieldFill.INSERT)
    private EnableStatusEnum enableStatus;

    /**
     * 创建类型：0-接口创建，1-控制台创建
     */
    @TableField(value = "create_type")
    private CreateTypeEnum createType;

    /**
     * 有效类型：0-永久，1-指定时间
     */
    @TableField(value = "valid_data_type")
    private ValidDateTypeEnum validDateType;

    /**
     * 有效日期
     */
    @TableField(value = "valid_date")
    private Date validDate;

    /**
     * 描述信息
     */
    @TableField(value = "description")
    private String describe;

    /**
     * 网站图标
     */
    @TableField(value = "favicon")
    private String favicon;

    /**
     * 总页面浏览量（Page View）
     */
    @TableField(value = "total_pv", fill = FieldFill.INSERT)
    private Integer totalPv;

    /**
     * 总独立访客数（Unique Visitor）
     */
    @TableField(value = "total_uv", fill = FieldFill.INSERT)
    private Integer totalUv;

    /**
     * 总独立IP数（Unique IP）
     */
    @TableField(value = "total_uip", fill = FieldFill.INSERT)
    private Integer totalUip;

    /**
     * 删除时间戳
     */
    @TableField(value = "delete_time", fill = FieldFill.INSERT)
    private Long deleteTime;

}
