package com.zicca.zlink.backend.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "短链接分组查询返回参数")
public class ZLinkGroupCountQueryRespDTO {

    @Schema(description = "分组标识", example = "1")
    private String gid;

    @Schema(description = "短链接数量", example = "1")
    private Integer shortLinkCount;

}
