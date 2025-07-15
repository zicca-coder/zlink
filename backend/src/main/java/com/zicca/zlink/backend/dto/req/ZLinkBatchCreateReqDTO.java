package com.zicca.zlink.backend.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Schema(description = "批量创建短链接请求参数")
public class ZLinkBatchCreateReqDTO {

    /**
     * 原始链接集合
     */
    @Schema(description = "原始链接集合", example = "[https://example.com, https://example.org]")
    private List<String> originUrls;

    /**
     * 描述集合
     */
    @Schema(description = "描述集合", example = "[示例1, 示例2]")
    private List<String> describes;

    /**
     * 分组标识
     */
    @Schema(description = "分组标识", example = "1")
    private String gid;

    /**
     * 创建类型：0-接口创建 1-控制台创建
     */
    @Schema(description = "创建类型：0-接口创建 1-控制台创建", example = "0")
    private Integer createType;

    /**
     * 链接有效期类型：0-永久 1-指定时间
     */
    @Schema(description = "链接有效期类型：0-永久 1-指定时间", example = "0")
    private Integer validDateType;

    /**
     * 链接有效期
     */
    @Schema(description = "链接有效期", example = "2021-01-01 00:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validDate;


}
