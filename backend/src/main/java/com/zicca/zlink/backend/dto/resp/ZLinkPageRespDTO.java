package com.zicca.zlink.backend.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zicca.zlink.backend.common.enums.EnableStatusEnum;
import com.zicca.zlink.backend.common.enums.ValidDateTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 短链接分页返回参数
 */
@Data
@Schema(description = "短链接分页返回参数")
public class ZLinkPageRespDTO {

    @Schema(description = "短链接主键ID", example = "1")
    private Long id;

    @Schema(description = "短链接域名", example = "https://zicca.com")
    private String domain;

    @Schema(description = "短链接uri", example = "abxfe")
    private String shortUri;

    @Schema(description = "短链接url", example = "https://www.zicca.com/abxfe")
    private String shortUrl;

    @Schema(description = "原始链接", example = "https://www.example.com")
    private String originUrl;

    @Schema(description = "分组标识", example = "1")
    private String gid;

    @Schema(description = "有效期类型：0-永久有效，1-自定义", example = "1")
    private ValidDateTypeEnum validDateType;

    @Schema(description = "启用状态：0-禁用，1-启用", example = "1")
    private EnableStatusEnum enableStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "有效期", example = "2023-08-08 00:00:00")
    private Date validDate;

    @Schema(description = "图标", example = "https://www.iocoder.cn/favicon.ico")
    private String favicon;

    @Schema(description = "总页面浏览量", example = "1024")
    private Integer totalPv;

    @Schema(description = "总独立访客数", example = "1024")
    private Integer totalUv;

    @Schema(description = "总独立ip数", example = "1024")
    private Integer totalUip;

    @Schema(description = "今日页面浏览量", example = "1024")
    private Integer todayPv;

    @Schema(description = "今日独立访客数", example = "1024")
    private Integer todayUv;

    @Schema(description = "今日独立ip数", example = "1024")
    private Integer todayUip;

}
