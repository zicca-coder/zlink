package com.zicca.zlink.backend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zicca.zlink.backend.cache.holder.BloomFilterHolder;
import com.zicca.zlink.backend.cache.holder.CacheHolder;
import com.zicca.zlink.backend.common.constant.RedisKeyConstants;
import com.zicca.zlink.backend.common.enums.CreateTypeEnum;
import com.zicca.zlink.backend.common.enums.ValidDateTypeEnum;
import com.zicca.zlink.backend.config.ShortUrlConfig;
import com.zicca.zlink.backend.dao.entity.ZLink;
import com.zicca.zlink.backend.dao.mapper.ZLinkMapper;
import com.zicca.zlink.backend.dto.biz.ZLinkStatsRecordDTO;
import com.zicca.zlink.backend.dto.req.ZLinkBatchCreateReqDTO;
import com.zicca.zlink.backend.dto.req.ZLinkCreateReqDTO;
import com.zicca.zlink.backend.dto.req.ZLinkPageReqDTO;
import com.zicca.zlink.backend.dto.req.ZLinkUpdateReqDTO;
import com.zicca.zlink.backend.dto.resp.ZLinkCreateRespDTO;
import com.zicca.zlink.backend.dto.resp.ZLinkGroupCountQueryRespDTO;
import com.zicca.zlink.backend.pool.ShortUrlPoolManager;
import com.zicca.zlink.backend.service.ShortUrlGeneratorService;
import com.zicca.zlink.backend.service.ZLinkService;
import com.zicca.zlink.backend.toolkit.LinkUtil;
import com.zicca.zlink.framework.aop.TimeCost;
import com.zicca.zlink.framework.execption.ServiceException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

@Slf4j(topic = "ZLinkServiceImpl")
@Service
@RequiredArgsConstructor
public class ZLinkServiceImpl extends ServiceImpl<ZLinkMapper, ZLink> implements ZLinkService {

    private final CacheHolder cacheHolder;
    private final BloomFilterHolder bloomFilterHolder;
    private final RedissonClient redissonClient;
    private final ShortUrlGeneratorService shortUrlGeneratorService;
    private final ShortUrlPoolManager poolManager;
    private final ShortUrlConfig shortUrlConfig;

    @Value("${zlink.domain.default}")
    private String defaultDomain;

    @TimeCost
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ZLinkCreateRespDTO createZLink(ZLinkCreateReqDTO requestParam) {
        String shortUrl;
        
        // 策略选择：优先从预生成池获取，失败时使用实时生成
        if (shortUrlConfig.getPreGenerate().getEnabled()) {
            shortUrl = poolManager.acquireShortUrl();
            if (shortUrl == null) {
                log.warn("预生成池为空，使用实时生成策略");
                shortUrl = shortUrlGeneratorService.generateUniqueShortUrl(
                    requestParam.getOriginUrl(), 
                    requestParam.getGid()
                );
            } else {
                log.info("从预生成池获取短链接: {}", shortUrl);
            }
        } else {
            // 预生成池未启用，使用实时生成
            shortUrl = shortUrlGeneratorService.generateUniqueShortUrl(
                requestParam.getOriginUrl(), 
                requestParam.getGid()
            );
        }
        
        ZLink zLink = ZLink.builder()
                .domain(defaultDomain)
                .originUrl(requestParam.getOriginUrl())
                .shortUri(shortUrl)
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
        cacheHolder.putToCache(shortUrl, zLink.getOriginUrl(), 
            LinkUtil.getLinkCacheValidTime(requestParam.getValidDate()));
        
        // 加入布隆过滤器
        bloomFilterHolder.add(shortUrl);
        
        log.info("创建短链接成功: {} -> {}", shortUrl, requestParam.getOriginUrl());
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

    @SneakyThrows
    @Override
    public void restoreUrl(String shortUrl, ServletRequest request, ServletResponse response) {
        // 查询本地是否缓存空值（避免缓存击穿） 【短链：原始链接】
        String originUrl = null;
        // 如果本地缓存空值命中，直接返回404 \ 如果本地缓存命中非空，直接跳转
        if (StrUtil.isNotBlank(originUrl = cacheHolder.getFromCache(shortUrl))) {
            if (RedisKeyConstants.LINK_NOT_EXIST_VALUE.equals(originUrl)) {
                log.info(">>>本地缓存：短链不存在: shortUrl={}", shortUrl);
                ((HttpServletResponse) response).sendRedirect("/page/notfound");
                return;
            }
            // 消息队列异步统计短链访问信息
            log.info(">>>本地缓存命中短链接：shortUrl={}", shortUrl);
            ((HttpServletResponse) response).sendRedirect(originUrl);
            return;
        }
        // 如果本地缓存未命中，查询本地布隆过滤器是否存在
        // 如果不存在，直接返回404
        if (!bloomFilterHolder.mightContainsInLocal(shortUrl)) {
            // 如果不存在，直接返回404
            log.info(">>>本地布隆过滤器不存在: shortUrl={}", shortUrl);
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return;
        }
        // 如果布隆过滤器存在，查询Redis缓存
        // 查询Redis缓存是否命中 命中空值返回404 \ 命中非空值跳转
        if (StrUtil.isNotBlank(originUrl = cacheHolder.getFromRedis(shortUrl))) {
            if (RedisKeyConstants.LINK_NOT_EXIST_VALUE.equals(originUrl)) {
                log.info(">>>Redis缓存：短链不存在: shortUrl={}", shortUrl);
                ((HttpServletResponse) response).sendRedirect("/page/notfound");
                return;
            }
            // 如果缓存命中非空值，跳转
            log.info(">>>Redis缓存命中短链接：shortUrl={}", shortUrl);
            ((HttpServletResponse) response).sendRedirect(originUrl);
            return;
        }
        // 如果缓存未命中，查询Redis布隆过滤器是否存在
        // 如果不存在，直接返回404
        // 如果布隆过滤器存在，则查询数据库
        if (!bloomFilterHolder.mightContainsInRedis(shortUrl)) {
            // 如果不存在，直接返回404
            log.info(">>>Redis布隆过滤器不存在: shortUrl={}", shortUrl);
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return;
        }
        // 如果数据库命中，则加入缓存，跳转
        // 如果数据库未命中，则返回404，本地缓存空值，Redis缓存空值，缓存时间设置3-5分钟
        RLock lock = redissonClient.getLock(shortUrl);
        lock.lock();
        try {
            // 双重判断是否有其他线程已经重建缓存
            // 如果本地缓存空值命中，直接返回404 \ 如果本地缓存命中非空，直接跳转
            if (StrUtil.isNotBlank(originUrl = cacheHolder.getFromLocal(shortUrl))) {
                if (RedisKeyConstants.LINK_NOT_EXIST_VALUE.equals(originUrl)) {
                    ((HttpServletResponse) response).sendRedirect("/page/notfound");
                    return;
                }
                ((HttpServletResponse) response).sendRedirect(originUrl);
                return;
            }
            // 查询Redis缓存是否命中 命中空值返回404 \ 命中非空值跳转
            if (StrUtil.isNotBlank(originUrl = cacheHolder.getFromRedis(shortUrl))) {
                if (RedisKeyConstants.LINK_NOT_EXIST_VALUE.equals(originUrl)) {
                    ((HttpServletResponse) response).sendRedirect("/page/notfound");
                    return;
                }
                // 如果缓存命中非空值，跳转
                ((HttpServletResponse) response).sendRedirect(originUrl);
                return;
            }

            // 查询数据库
            ZLink link = lambdaQuery().eq(ZLink::getShortUrl, shortUrl).one();
            if (ObjectUtil.isNull(link)) {
                cacheHolder.putNullToCache(shortUrl); // 空值过期时间 3分钟
                ((HttpServletResponse) response).sendRedirect("/page/notfound");
                return;
            }
            // 重建缓存
            cacheHolder.putToCache(shortUrl, link.getOriginUrl(), true);
            ((HttpServletResponse) response).sendRedirect(originUrl);
        } finally {
            lock.unlock();
        }
    }


    @Override
    public void zLinkStats(ZLinkStatsRecordDTO requestParam) {

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
