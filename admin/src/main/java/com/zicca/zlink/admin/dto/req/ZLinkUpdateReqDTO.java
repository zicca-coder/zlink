package com.zicca.zlink.admin.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 短链接更新请求参数
 */
@Data
@Schema(description = "短链接更新请求对象")
public class ZLinkUpdateReqDTO {

    /**
     * 原始链接
     */
    @Schema(description = "原始链接", example = "https://articles.zsxq.com/id_i2rozw9b8wuw.html")
    private String originUrl;

    /**
     * 完整短链接
     */
    @Schema(description = "完整短链接", example = "https://articles.zsxq.com/exbrfh")
    private String shortUrl;

    /**
     * 原始分组标识
     */
    @Schema(description = "原始分组标识", example = "1")
    private String originGid;

    /**
     * 分组标识
     */
    @Schema(description = "分组标识", example = "2")
    private String gid;

    /**
     * 有效期类型 0：永久有效 1：自定义
     */
    @Schema(description = "有效期类型 0：永久有效 1：自定义", example = "1")
    private Integer validDateType;

    /**
     * 有效期
     */
    @Schema(description = "有效期", example = "2021-09-01 00:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validDate;

    /**
     * 描述
     */
    @Schema(description = "描述", example = "描述")
    private String describe;

}
