package com.zicca.zlink.admin.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 短链接基础信息响应参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "短链接基础信息响应参数")
public class ZLinkBaseInfoRespDTO {

    @Schema(description = "短链接描述", example = "这是一个短链接")
    private String describe;
    @Schema(description = "原始链接", example = "https://articles.zsxq.com/id_i2rozw9b8wuw.html")
    private String originUrl;
    @Schema(description = "短链接", example = "https://articles.zsxq.com/iozww")
    private String shortUrl;

}
