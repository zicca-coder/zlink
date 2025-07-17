package com.zicca.zlink.backend.cache.service;

import cn.hutool.core.util.StrUtil;
import com.zicca.zlink.backend.common.constant.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j(topic = "RedisCacheService")
@RequiredArgsConstructor
public class RedisCacheService{

    private final RedisTemplate<String, String> redisTemplate;

    public String get(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(value)) {
                log.info(">>>从Redis缓存获取数据: key={}, value={}", key, value);
                return value;
            }
        } catch (Exception e) {
            log.error(">>>从Redis缓存获取数据失败: key={}", key, e);
        }
        return null;
    }

    public void put(String key, String value) {
        redisTemplate.opsForValue().set(key, value, RedisKeyConstants.NORAML_EXPIRE_TIME);
        log.info(">>>添加数据到Redis缓存: key={}, value={}", key, value);
    }

    public void put(String key, String value, boolean isHot) {
        try {
            Duration baseDuration = isHot ? RedisKeyConstants.NORAML_EXPIRE_TIME : RedisKeyConstants.HOT_EXPIRE_TIME;
            int randomExtraHours = ThreadLocalRandom.current().nextInt(0, 3);
            Duration expireTime = baseDuration.plus(Duration.ofHours(randomExtraHours));
            redisTemplate.opsForValue().set(key, value, expireTime);
            log.info(">>>添加数据到Redis缓存: key={}, value={}", key, value);
        } catch (Exception e) {
            log.error(">>>添加数据到Redis缓存失败: key={}, value={}", key, value, e);
        }
    }

    public void put(String key, String value, Long validDate) {
        try {
            redisTemplate.opsForValue().set(key, value, validDate, TimeUnit.MILLISECONDS);
            log.info(">>>添加数据到Redis缓存: key={}, value={}", key, value);
        } catch (Exception e) {
            log.error(">>>添加数据到Redis缓存失败: key={}, value={}", key, value, e);
        }
    }

    public void put(String key, String value, Long time, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, time, unit);
            log.info(">>>添加数据到Redis缓存: key={}, value={}", key, value);
        } catch (Exception e) {
            log.error(">>>添加数据到Redis缓存失败: key={}, value={}", key, value, e);
        }
    }

    public void putNull(String key, String value) {
        redisTemplate.opsForValue().set(key, value, 3, TimeUnit.MINUTES);
    }
}
