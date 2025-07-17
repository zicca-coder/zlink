package com.zicca.zlink.admin.controller;

import com.zicca.zlink.admin.api.ZLinkService;
import com.zicca.zlink.admin.dto.req.ZLinkCreateReqDTO;
import com.zicca.zlink.admin.dto.resp.ZLinkCreateRespDTO;
import com.zicca.zlink.framework.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(description = "短链接管理", name = "短链接接口管理")
@RestController
@RequestMapping("/api/short-link/admin/v1")
@RequiredArgsConstructor
public class ZLinkController {

    private final ZLinkService zLinkService;


    @PostMapping("/create")
    public Result<ZLinkCreateRespDTO> create(@RequestBody ZLinkCreateReqDTO reqDTO) {
        return zLinkService.createShortLink(reqDTO);
    }




}
