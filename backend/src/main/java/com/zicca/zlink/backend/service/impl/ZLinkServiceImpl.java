package com.zicca.zlink.backend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.text.StrBuilder;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zicca.zlink.backend.cache.CacheService;
import com.zicca.zlink.backend.common.enums.CreateTypeEnum;
import com.zicca.zlink.backend.common.enums.ValidDateTypeEnum;
import com.zicca.zlink.backend.dao.entity.ZLink;
import com.zicca.zlink.backend.dao.mapper.ZLinkMapper;
import com.zicca.zlink.backend.dto.biz.ZLinkStatsRecordDTO;
import com.zicca.zlink.backend.dto.req.ZLinkBatchCreateReqDTO;
import com.zicca.zlink.backend.dto.req.ZLinkCreateReqDTO;
import com.zicca.zlink.backend.dto.req.ZLinkPageReqDTO;
import com.zicca.zlink.backend.dto.req.ZLinkUpdateReqDTO;
import com.zicca.zlink.backend.dto.resp.ZLinkCreateRespDTO;
import com.zicca.zlink.backend.dto.resp.ZLinkGroupCountQueryRespDTO;
import com.zicca.zlink.backend.service.ZLinkService;
import com.zicca.zlink.backend.toolkit.HashUtil;
import com.zicca.zlink.backend.toolkit.LinkUtil;
import com.zicca.zlink.framework.execption.ServiceException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.UUID;

@Slf4j(topic = "ZLinkServiceImpl")
@Service
@RequiredArgsConstructor
public class ZLinkServiceImpl extends ServiceImpl<ZLinkMapper, ZLink> implements ZLinkService {

    private final CacheService cacheService;


    @Value("${zlink.domain.default}")
    private String defaultDomain;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ZLinkCreateRespDTO createZLink(ZLinkCreateReqDTO requestParam) {
        String suffix = generateSuffix();
        String shortUrl = StrBuilder.create(defaultDomain).append("/").append(suffix).toString();
        ZLink zLink = ZLink.builder()
                .domain(defaultDomain)
                .originUrl(requestParam.getOriginUrl())
                .shortUri(suffix)
                .shortUrl(shortUrl)
                .gid(requestParam.getGid())
                .createType(CreateTypeEnum.formCode(requestParam.getCreateType()))
                .validDateType(ValidDateTypeEnum.formCode(requestParam.getValidDateType()))
                .validDate(requestParam.getValidDate())
                .describe(requestParam.getDescribe())
                .favicon(getFavicon(requestParam.getOriginUrl()))
                .build();
        boolean saved = save(zLink);
        if (!saved) {
            throw new ServiceException("新增短链接失败");
        }
        // 加入缓存 默认刚创建的短链接是即将被访问的
        cacheService.putToCache(shortUrl, zLink.getOriginUrl(), LinkUtil.getLinkCacheValidTime(requestParam.getValidDate()));
        // 加入布隆过滤器
        cacheService.addToBloomFilter(shortUrl);
        return BeanUtil.copyProperties(zLink, ZLinkCreateRespDTO.class);
    }

    @Override
    public ZLinkCreateReqDTO createZLinkByLock(ZLinkCreateReqDTO requestParam) {
        return null;
    }

    @Override
    public ZLinkBatchCreateReqDTO batchCreateZLink(ZLinkBatchCreateReqDTO requestParam) {
        return null;
    }

    @Override
    public void updateZLink(ZLinkUpdateReqDTO requestParam) {

    }

    @Override
    public IPage<ZLinkPageReqDTO> pageZLink(ZLinkPageReqDTO requestParam) {
        return null;
    }

    @Override
    public List<ZLinkGroupCountQueryRespDTO> listGroupZLinkCount(List<String> requestParam) {
        return List.of();
    }

    @Override
    public void restoreUrl(String shortUri, ServletRequest request, ServletResponse response) {

    }

    @Override
    public void zLinkStats(ZLinkStatsRecordDTO requestParam) {

    }


    private String generateSuffix() {
        // todo: 改进
        String shortUri = HashUtil.hashToBase62(UUID.randomUUID().toString());
        return shortUri;
    }

    @SneakyThrows
    private String getFavicon(String url) {
        URL targetUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) targetUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        int responseCode = connection.getResponseCode();
        if (HttpURLConnection.HTTP_OK == responseCode) {
            Document document = Jsoup.connect(url).get();
            Element faviconLink = document.select("link[rel~=(?i)^(shortcut )?icon]").first();
            if (faviconLink != null) {
                return faviconLink.attr("abs:href");
            }
        }
        return null;
    }
}
