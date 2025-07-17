package com.zicca.zlink.backend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zicca.zlink.backend.dao.entity.ZLink;
import com.zicca.zlink.backend.dto.biz.ZLinkStatsRecordDTO;
import com.zicca.zlink.backend.dto.req.ZLinkBatchCreateReqDTO;
import com.zicca.zlink.backend.dto.req.ZLinkCreateReqDTO;
import com.zicca.zlink.backend.dto.req.ZLinkPageReqDTO;
import com.zicca.zlink.backend.dto.req.ZLinkUpdateReqDTO;
import com.zicca.zlink.backend.dto.resp.ZLinkCreateRespDTO;
import com.zicca.zlink.backend.dto.resp.ZLinkGroupCountQueryRespDTO;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.util.List;

public interface ZLinkService extends IService<ZLink> {

    /**
     * 创建短链接
     *
     * @param requestParam 创建短链接请求参数
     * @return 短链接创建信息
     */
    ZLinkCreateRespDTO createZLink(ZLinkCreateReqDTO requestParam);

    /**
     * 根据分布式锁创建短链接
     *
     * @param requestParam 创建短链接请求参数
     * @return 短链接创建信息
     */
    ZLinkCreateReqDTO createZLinkByLock(ZLinkCreateReqDTO requestParam);

    /**
     * 批量创建短链接
     *
     * @param requestParam 批量创建短链接请求参数
     * @return 短链接创建信息
     */
    ZLinkBatchCreateReqDTO batchCreateZLink(ZLinkBatchCreateReqDTO requestParam);


    /**
     * 修改短链接
     *
     * @param requestParam 修改短链接请求参数
     */
    void updateZLink(ZLinkUpdateReqDTO requestParam);

    /**
     * 分页查询短链接
     *
     * @param requestParam 分页查询短链接请求参数
     * @return 分页结果
     */
    IPage<ZLinkPageReqDTO> pageZLink(ZLinkPageReqDTO requestParam);

    /**
     * 查询短链接分组内数量
     *
     * @param requestParam 查询短链接分组内数量请求参数
     * @return 分组内数量
     */
    List<ZLinkGroupCountQueryRespDTO> listGroupZLinkCount(List<String> requestParam);

    /**
     * 短链接跳转
     *
     * @param shortUrl 短链接后缀
     * @param request  请求
     * @param response 响应
     */
    void restoreUrl(String shortUrl, ServletRequest request, ServletResponse response);


    void zLinkStats(ZLinkStatsRecordDTO requestParam);

}
