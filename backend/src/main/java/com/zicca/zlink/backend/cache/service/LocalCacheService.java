package com.zicca.zlink.backend.cache.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;


@Service
@Slf4j(topic = "LocalCacheService")
public class LocalCacheService{

    @Cacheable(value = "shortUrls", key = "#key")
    public String get(String key) {
        log.info(">>>从本地缓存获取数据: key={}", key);
        return null;
    }
    @CachePut(value = "shortUrls", key = "#key")
    public void put(String key, String value) {
        log.info(">>>添加数据到本地缓存: key={}, value={}", key, value);
    }

    @CachePut(value = "shortUrls", key = "#key")
    public void put(String key, Object value) {
        log.info(">>>添加数据到本地缓存: key={}, value={}", key, value);
    }

    @Cacheable(value = "shortUrls", key = "#key")
    public void putNull(String key, String value) {
        log.info(">>>添加空数据到本地缓存: key={}, value={}", key, value);
    }


}
