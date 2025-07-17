package com.zicca.zlink.admin.api;

import com.zicca.zlink.admin.dto.req.ZLinkCreateReqDTO;
import com.zicca.zlink.admin.dto.resp.ZLinkCreateRespDTO;
import com.zicca.zlink.framework.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "backend")
public interface ZLinkService {

    /**
     * 创建短链接
     *
     * @param reqDTO 请求参数
     * @return 短链接创建结果
     */
    @PostMapping("/api/short-link/backend/v1/create")
    Result<ZLinkCreateRespDTO> createShortLink(@RequestBody ZLinkCreateReqDTO reqDTO);


}
