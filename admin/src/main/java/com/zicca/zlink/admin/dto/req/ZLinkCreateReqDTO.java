package com.zicca.zlink.admin.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 短链接创建请求参数
 */
@Data
@Schema(description = "短链接创建请求参数")
public class ZLinkCreateReqDTO {

    /**
     * 域名
     */
    @Schema(description = "域名", example = "https://articles.zsxq.com")
    private String domain;

    /**
     * 原始连接
     */
    @Schema(description = "原始连接", example = "https://articles.zsxq.com/id_i2rozw9b8wuw.html")
    private String originUrl;

    /**
     * 分组标识
     */
    @Schema(description = "分组标识", example = "1")
    private String gid;

    /**
     * 创建方式：0-接口创建，1-控制台创建
     */
    @Schema(description = "创建方式：0-接口创建，1-控制台创建", example = "0")
    private Integer createType;

    /**
     * 有效期类型：0-永久，1-指定时间
     */
    @Schema(description = "有效期类型：0-永久，1-指定时间", example = "0")
    private Integer validDateType;

    /**
     * 有效期
     */
    @Schema(description = "有效期", example = "2025-08-01 00:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validDate;

    /**
     * 描述信息
     */
    @Schema(description = "描述信息", example = "这是一个链接")
    private String describe;

}
