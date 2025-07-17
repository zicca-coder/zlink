package com.zicca.zlink.backend.cache.holder;

import cn.hutool.core.util.StrUtil;
import com.zicca.zlink.backend.cache.service.LocalCacheService;
import com.zicca.zlink.backend.cache.service.RedisCacheService;
import com.zicca.zlink.backend.common.constant.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j(topic = "CacheHolder")
@Service
@RequiredArgsConstructor
public class CacheHolder {

    private final RedisCacheService redisCacheService;
    private final LocalCacheService localCacheService;


    public void putToLocal(String key, String value) {
        localCacheService.put(key, value);
    }

    public void putToRedis(String key, String value) {
        redisCacheService.put(key, value);
    }

    public void putToRedis(String key, String value, boolean isHot) {
        redisCacheService.put(key, value, isHot);
    }

    public void putToRedis(String key, String value, Long validDate) {
        redisCacheService.put(key, value, validDate);
    }

    public void putToRedis(String key, String value, Long time, TimeUnit unit) {
        redisCacheService.put(key, value, time, unit);
    }

    public void putNullToLocal(String key) {
        localCacheService.putNull(key, RedisKeyConstants.LINK_NOT_EXIST_VALUE);
    }

    public void putNullToRedis(String key) {
        redisCacheService.putNull(key, RedisKeyConstants.LINK_NOT_EXIST_VALUE);
    }

    public void putNullToCache(String key) {
        localCacheService.putNull(key, RedisKeyConstants.LINK_NOT_EXIST_VALUE);
        redisCacheService.putNull(key, RedisKeyConstants.LINK_NOT_EXIST_VALUE);
    }

    public void putToCache(String key, String value) {
        localCacheService.put(key, value);
        redisCacheService.put(key, value);
    }

    public void putToCache(String key, String value, boolean isHot) {
        localCacheService.put(key, value);
        redisCacheService.put(key, value, isHot);
    }

    public void putToCache(String key, String value, Long validDate) {
        localCacheService.put(key, value);
        redisCacheService.put(key, value, validDate);
    }

    public void putToCache(String key, String value, Long time, TimeUnit unit) {
        localCacheService.put(key, value);
        redisCacheService.put(key, value, time, unit);
    }

    public String getFromCache(String key) {
        String value = localCacheService.get(key);
        if (StrUtil.isNotBlank(value)) {
            log.info(">>>本地缓存命中");
            return value;
        }
        value = redisCacheService.get(key);
        if (StrUtil.isNotBlank(value)) {
            log.info(">>>Redis缓存命中");
            // 同步到本地缓存
            localCacheService.put(key, value);
        }
        return value;
    }

    public String getFromLocal(String key) {
        String value = localCacheService.get(key);
        if (StrUtil.isNotBlank(value)) {
            log.info(">>>本地缓存命中");
        }
        return value;
    }

    public String getFromRedis(String key) {
        String value = redisCacheService.get(key);
        if (StrUtil.isNotBlank(value)) {
            log.info(">>>Redis缓存命中");
        }
        return value;
    }


}
