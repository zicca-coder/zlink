package com.zicca.zlink.backend.cache;

import com.zicca.zlink.backend.common.constant.RedisKeyConstants;
import com.zicca.zlink.backend.dao.entity.ZLink;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j(topic = "CacheService")
@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final TieredBloomFilterService tieredBloomFilterService;


    /**
     * 统一缓存接口，从多级缓存获取短链接的原始链接
     *
     * @param shortUrl 短链接
     * @return 原始链接
     */
    public String getFromCache(String shortUrl) {
        // 先从本地缓存获取
        String originUrl = cacheFromLocal(shortUrl);
        if (originUrl != null) {
            log.debug(">>>本地缓存命中: shortUrl={}, originUrl={}", shortUrl, originUrl);
            return originUrl;
        }
        // 本地缓存未命中，从Redis获取
        originUrl = cacheFromRedis(shortUrl);
        if (originUrl != null) {
            log.debug(">>>Redis缓存命中: shortUrl={}, originUrl={}", shortUrl, originUrl);
            // 将Redis数据放入本地缓存
            cacheToLocal(shortUrl, originUrl);
        }
        return originUrl;
    }

    public void putToCache(String shortUrl, String originUrl, boolean isHot) {
        // 将短链接和原始链接放入本地缓存
        cacheToLocal(shortUrl, originUrl);
        // 将短链接和原始链接放入Redis缓存
        cacheToRedis(shortUrl, originUrl, isHot);
        log.debug(">>>添加数据到多级缓存: shortUrl={}, originUrl={}", shortUrl, originUrl);
    }

    public void putToCache(String shortUrl, String originUrl, Long validDate) {
        // 将短链接和原始链接放入本地缓存
        cacheToLocal(shortUrl, originUrl);
        // 将短链接和原始链接放入Redis缓存
        cacheToRedis(shortUrl, originUrl, validDate);
        log.debug(">>>添加数据到多级缓存: shortUrl={}, originUrl={}", shortUrl, originUrl);
    }


    /**
     * 添加短链接到本地缓存
     *
     * @param shortUrl  短链接
     * @param originUrl 原始链接
     * @return 原始链接
     */
    @CachePut(value = "shortUrls", key = "#shortUrl")
    public String cacheToLocal(String shortUrl, String originUrl) {
        log.debug(">>>添加数据到本地缓存: shortUrl={}, originUrl={}", shortUrl, originUrl);
        return originUrl;
    }

    /**
     * 根据短链从本地缓存获取原始链接
     *
     * @param shortUrl 短链接
     * @return 原始链接
     */
    @Cacheable(value = "shortUrls", key = "#shortUrl")
    public String cacheFromLocal(String shortUrl) {
        log.debug(">>>从本地缓存获取数据: shortUrl={}", shortUrl);
        return null; // 本地缓存未命中时返回 null
    }

    /**
     * 添加短链到Redis缓存
     *
     * @param shortUrl  短链接
     * @param originUrl 原始链接
     * @param isHot     是否是热门短链
     */
    public void cacheToRedis(String shortUrl, String originUrl, boolean isHot) {
        try {
            String key = RedisKeyConstants.LINK_CACHE_KEY + shortUrl;
            Duration baseDuration = isHot ? RedisKeyConstants.NORAML_EXPIRE_TIME : RedisKeyConstants.HOT_EXPIRE_TIME;
            int randomExtraHours = ThreadLocalRandom.current().nextInt(0, 3);
            Duration expireTime = baseDuration.plus(Duration.ofHours(randomExtraHours));
            redisTemplate.opsForValue().set(shortUrl, originUrl, expireTime);
            log.debug(">>>添加数据到Redis缓存: shortUrl={}, originUrl={}", shortUrl, originUrl);
        } catch (Exception e) {
            log.error(">>>添加数据到Redis缓存失败: shortUrl={}, originUrl={}", shortUrl, originUrl, e);
        }
    }


    /**
     * 添加短链到Redis缓存
     *
     * @param shortUrl  短链接
     * @param originUrl 原始链接
     * @param validDate 自定义的有效期（毫秒）
     */
    public void cacheToRedis(String shortUrl, String originUrl, Long validDate) {
        try {
            String key = RedisKeyConstants.LINK_CACHE_KEY + shortUrl;
            redisTemplate.opsForValue().set(shortUrl, originUrl, validDate, TimeUnit.MILLISECONDS);
            log.debug(">>>添加数据到Redis缓存: shortUrl={}, originUrl={}", shortUrl, originUrl);
        } catch (Exception e) {
            log.error(">>>添加数据到Redis缓存失败: shortUrl={}, originUrl={}", shortUrl, originUrl, e);
        }
    }

    public String cacheFromRedis(String shortUrl) {
        try {
            String key = RedisKeyConstants.LINK_CACHE_KEY + shortUrl;
            String originUrl = redisTemplate.opsForValue().get(key);
            if (originUrl != null) {
                log.debug(">>>从Redis缓存获取数据: shortUrl={}, originUrl={}", shortUrl, originUrl);
                return originUrl;
            }
        } catch (Exception e) {
            log.error(">>>从Redis缓存获取数据失败: shortUrl={}", shortUrl, e);
        }
        return null; // Redis缓存未命中时返回 null
    }


    /**
     * 缓存短链接访问次数至Redis缓存
     *
     * @param shortUrl 短链接
     * @param count    访问次数
     */
    public void cacheClickNumToRedis(String shortUrl, Long count) {
        try {
            String key = RedisKeyConstants.COUNT_CACHE_KEY + shortUrl;
            redisTemplate.opsForValue().set(key, count.toString(), RedisKeyConstants.NORAML_EXPIRE_TIME);
        } catch (Exception e) {
            log.error(">>>缓存访问次数失败: shortUrl={}, count={}", shortUrl, count, e);
        }
    }

    /**
     * 从Redis缓存获取短链接访问次数
     *
     * @param shortUrl 短链接
     * @return 访问次数
     */
    public Long cacheClickNumFromRedis(String shortUrl) {
        try {
            String key = RedisKeyConstants.COUNT_CACHE_KEY + shortUrl;
            String count = redisTemplate.opsForValue().get(key);
            return count != null ? Long.parseLong(count) : null;
        } catch (Exception e) {
            log.error(">>>获取访问次数失败: shortUrl={}", shortUrl, e);
            return null;
        }
    }

    /**
     * 删除缓存、本地缓存和Redis缓存
     *
     * @param shortUrl 短链接
     */
    @CacheEvict(value = "shortUrls", key = "#shortUrl")
    public void exictCache(String shortUrl) {
        try {
            String urlKey = RedisKeyConstants.LINK_CACHE_KEY + shortUrl;
            String countKey = RedisKeyConstants.COUNT_CACHE_KEY + shortUrl;
            redisTemplate.delete(urlKey);
            redisTemplate.delete(countKey);
            log.debug(">>>删除缓存: shortUrl={}", shortUrl);
        } catch (Exception e) {
            log.error(">>>删除缓存失败: shortUrl={}", shortUrl, e);
        }
    }

    /**
     * 预热缓存
     *
     * @param zLink 短链
     */
    public void warmUpCache(ZLink zLink) {
        cacheToRedis(zLink.getShortUrl(), zLink.getOriginUrl(), zLink.getClickNum() >= 1000);
        cacheClickNumToRedis(zLink.getShortUrl(), zLink.getClickNum());
    }

    public void addToBloomFilter(String shortUrl) {
        tieredBloomFilterService.add(shortUrl);
        log.debug(">>>添加短链接到布隆过滤器: shortUrl={}", shortUrl);
    }

    public boolean mightContainInBloomFilter(String shortUrl) {
        boolean result = tieredBloomFilterService.mightContain(shortUrl);
        log.debug(">>>布隆过滤器查询结果: shortUrl={}, result={}", shortUrl, result);
        return result;
    }

}
