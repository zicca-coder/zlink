package com.zicca.zlink.backend.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "创建短链接返回参数")
public class ZLinkCreateRespDTO {

    /**
     * 分组信息
     */
    @Schema(description = "分组信息", example = "DEGXES")
    private String gid;

    /**
     * 原始链接
     */
    @Schema(description = "原始链接", example = "https://articles.zsxq.com/id_i2rozw9b8wuw.html")
    private String originUrl;

    /**
     * 短链接
     */
    @Schema(description = "短链接", example = "https://articles.zsxq.com/degcxe")
    private String shortUrl;

}
