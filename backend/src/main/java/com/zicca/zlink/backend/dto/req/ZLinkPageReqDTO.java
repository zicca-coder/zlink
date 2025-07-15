package com.zicca.zlink.backend.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zicca.zlink.backend.dao.entity.ZLink;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "短链接分页查询参数")
public class ZLinkPageReqDTO extends Page<ZLink> {

    /**
     * 分组标识
     */
    @Schema(description = "分组标识", example = "1")
    private String gid;

    /**
     * 排序标识
     */
    @Schema(description = "排序标识", example = "ASC")
    private String orderTag;

}
