package com.zicca.zlink.backend.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 短链接批量创建响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "短链接批量创建响应对象")
public class ZLinkBatchCreateRespDTO {

    @Schema(description = "创建成功的短链接数量", example = "10")
    private Integer total;
    @Schema(description = "批量创建的短链接信息列表", example = "[...]")
    private List<ZLinkBaseInfoRespDTO> baseLinkInfos;
}
