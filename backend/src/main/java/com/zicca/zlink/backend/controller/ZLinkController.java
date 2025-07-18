package com.zicca.zlink.backend.controller;

import com.zicca.zlink.backend.dto.req.ZLinkCreateReqDTO;
import com.zicca.zlink.backend.dto.resp.ZLinkCreateRespDTO;
import com.zicca.zlink.backend.service.ZLinkService;
import com.zicca.zlink.framework.result.Result;
import com.zicca.zlink.framework.web.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(description = "短链接管理", name = "短链接接口管理")
@RestController
@RequestMapping("backend/api/v1/shorturl/")
@RequiredArgsConstructor
public class ZLinkController {

    private final ZLinkService zLinkService;


    @PostMapping("/create")
    @Operation(summary = "创建短链接", description = "创建短链接")
    @ApiResponse(
            responseCode = "200",
            description = "创建成功",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ZLinkCreateRespDTO.class)
            )
    )
    public Result<ZLinkCreateRespDTO> create(@RequestBody ZLinkCreateReqDTO reqDTO) {
        return Results.success(zLinkService.createZLink(reqDTO));
    }


    @GetMapping("/{short-url}")
    @Operation(summary = "访问短链接", description = "访问短链接")
    @ApiResponse(
            responseCode = "200",
            description = "访问成功"
    )
    public void restoreUrl(@PathVariable("short-url") String shortUrl, ServletRequest request, ServletResponse  response) {
        zLinkService.restoreUrl(shortUrl, request, response);
    }


}
