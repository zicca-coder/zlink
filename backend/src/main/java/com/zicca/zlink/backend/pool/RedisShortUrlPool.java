package com.zicca.zlink.backend.pool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis短链接池实现
 * 使用Redis List存储预生成的短链接，支持分布式环境
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisShortUrlPool implements ShortUrlPool {

    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String POOL_KEY = "zlink:shorturl:pool";
    private static final String STATS_KEY = "zlink:shorturl:pool:stats";
    
    private final AtomicLong localAcquired = new AtomicLong(0);
    private final AtomicLong localOffered = new AtomicLong(0);

    @Override
    public String acquire() {
        try {
            String shortUrl = redisTemplate.opsForList().rightPop(POOL_KEY);
            if (shortUrl != null) {
                localAcquired.incrementAndGet();
                redisTemplate.opsForHash().increment(STATS_KEY, "acquired", 1);
                log.debug("从Redis池获取短链接: {}, 剩余: {}", shortUrl, size());
            }
            return shortUrl;
        } catch (Exception e) {
            log.error("从Redis池获取短链接失败", e);
            return null;
        }
    }

    @Override
    public List<String> acquireBatch(int count) {
        List<String> result = new ArrayList<>();
        try {
            // 使用pipeline批量获取，提高性能
            List<String> shortUrls = redisTemplate.opsForList().rightPop(POOL_KEY, count);
            if (shortUrls != null && !shortUrls.isEmpty()) {
                result.addAll(shortUrls);
                localAcquired.addAndGet(result.size());
                redisTemplate.opsForHash().increment(STATS_KEY, "acquired", result.size());
                log.debug("从Redis池批量获取短链接: {} 个, 剩余: {}", result.size(), size());
            }
        } catch (Exception e) {
            log.error("从Redis池批量获取短链接失败", e);
        }
        return result;
    }

    @Override
    public boolean offer(String shortUrl) {
        if (shortUrl == null || shortUrl.trim().isEmpty()) {
            return false;
        }
        
        try {
            Long result = redisTemplate.opsForList().leftPush(POOL_KEY, shortUrl);
            if (result != null && result > 0) {
                localOffered.incrementAndGet();
                redisTemplate.opsForHash().increment(STATS_KEY, "offered", 1);
                log.debug("向Redis池添加短链接: {}, 总数: {}", shortUrl, result);
                return true;
            }
        } catch (Exception e) {
            log.error("向Redis池添加短链接失败: {}", shortUrl, e);
        }
        return false;
    }

    @Override
    public int offerBatch(List<String> shortUrls) {
        if (shortUrls == null || shortUrls.isEmpty()) {
            return 0;
        }
        
        try {
            String[] urlArray = shortUrls.toArray(new String[0]);
            Long result = redisTemplate.opsForList().leftPushAll(POOL_KEY, urlArray);
            
            if (result != null && result > 0) {
                int addedCount = shortUrls.size();
                localOffered.addAndGet(addedCount);
                redisTemplate.opsForHash().increment(STATS_KEY, "offered", addedCount);
                log.debug("向Redis池批量添加短链接: {} 个, 总数: {}", addedCount, result);
                return addedCount;
            }
        } catch (Exception e) {
            log.error("向Redis池批量添加短链接失败", e);
        }
        return 0;
    }

    @Override
    public int size() {
        try {
            Long size = redisTemplate.opsForList().size(POOL_KEY);
            return size != null ? size.intValue() : 0;
        } catch (Exception e) {
            log.error("获取Redis池大小失败", e);
            return 0;
        }
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public void clear() {
        try {
            int oldSize = size();
            redisTemplate.delete(POOL_KEY);
            log.info("清空Redis短链接池, 清除数量: {}", oldSize);
        } catch (Exception e) {
            log.error("清空Redis池失败", e);
        }
    }

    @Override
    public String getStats() {
        try {
            Object acquired = redisTemplate.opsForHash().get(STATS_KEY, "acquired");
            Object offered = redisTemplate.opsForHash().get(STATS_KEY, "offered");
            
            long totalAcquired = acquired != null ? Long.parseLong(acquired.toString()) : 0;
            long totalOffered = offered != null ? Long.parseLong(offered.toString()) : 0;
            
            return String.format("Redis短链接池统计 - 当前数量: %d, 总获取: %d, 总添加: %d, 本地获取: %d, 本地添加: %d", 
                    size(), totalAcquired, totalOffered, localAcquired.get(), localOffered.get());
        } catch (Exception e) {
            log.error("获取Redis池统计信息失败", e);
            return "Redis短链接池统计信息获取失败";
        }
    }

    /**
     * 设置池的过期时间（防止内存泄漏）
     */
    public void setExpire(long timeout, TimeUnit unit) {
        try {
            redisTemplate.expire(POOL_KEY, timeout, unit);
            redisTemplate.expire(STATS_KEY, timeout, unit);
        } catch (Exception e) {
            log.error("设置Redis池过期时间失败", e);
        }
    }

    /**
     * 重置统计信息
     */
    public void resetStats() {
        try {
            redisTemplate.delete(STATS_KEY);
            localAcquired.set(0);
            localOffered.set(0);
            log.info("重置Redis池统计信息");
        } catch (Exception e) {
            log.error("重置Redis池统计信息失败", e);
        }
    }
}