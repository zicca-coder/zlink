package com.zicca.zlink.backend.pool;

import com.zicca.zlink.backend.config.ShortUrlConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 短链接池管理器
 * 统一管理本地池和Redis池，实现分层获取策略
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShortUrlPoolManager {

    private final LocalShortUrlPool localPool;
    private final RedisShortUrlPool redisPool;
    private final ShortUrlConfig shortUrlConfig;

    /**
     * 获取短链接（分层策略）
     * 1. 优先从本地池获取
     * 2. 本地池为空时从Redis池获取并补充本地池
     * 3. 都为空时返回null
     */
    public String acquireShortUrl() {
        // 1. 优先从本地池获取
        String shortUrl = localPool.acquire();
        if (shortUrl != null) {
            return shortUrl;
        }

        // 2. 本地池为空，从Redis池批量获取并补充本地池
        if (!redisPool.isEmpty()) {
            int batchSize = Math.min(shortUrlConfig.getPreGenerate().getBatchSize() / 2, 100);
            List<String> batchUrls = redisPool.acquireBatch(batchSize);
            
            if (!batchUrls.isEmpty()) {
                // 补充本地池
                localPool.offerBatch(batchUrls);
                log.info("从Redis池补充本地池: {} 个短链接", batchUrls.size());
                
                // 返回第一个
                return localPool.acquire();
            }
        }

        log.warn("短链接池已空，无法获取短链接");
        return null;
    }

    /**
     * 批量获取短链接
     */
    public List<String> acquireShortUrls(int count) {
        // 先从本地池获取
        List<String> result = localPool.acquireBatch(count);
        
        // 如果本地池不够，从Redis池补充
        int remaining = count - result.size();
        if (remaining > 0 && !redisPool.isEmpty()) {
            List<String> fromRedis = redisPool.acquireBatch(remaining);
            result.addAll(fromRedis);
        }
        
        return result;
    }

    /**
     * 向池中添加短链接（优先添加到Redis池）
     */
    public boolean offerShortUrl(String shortUrl) {
        // 优先添加到Redis池（分布式共享）
        boolean redisSuccess = redisPool.offer(shortUrl);
        
        // 如果本地池未满，也添加到本地池
        if (localPool.size() < shortUrlConfig.getPreGenerate().getLocalPoolSize()) {
            localPool.offer(shortUrl);
        }
        
        return redisSuccess;
    }

    /**
     * 批量添加短链接
     */
    public int offerShortUrls(List<String> shortUrls) {
        if (shortUrls == null || shortUrls.isEmpty()) {
            return 0;
        }

        // 添加到Redis池
        int redisCount = redisPool.offerBatch(shortUrls);
        
        // 部分添加到本地池（避免本地池过大）
        int localCapacity = shortUrlConfig.getPreGenerate().getLocalPoolSize() - localPool.size();
        if (localCapacity > 0) {
            List<String> forLocal = shortUrls.subList(0, Math.min(localCapacity, shortUrls.size()));
            localPool.offerBatch(forLocal);
        }
        
        return redisCount;
    }

    /**
     * 检查是否需要补充池
     */
    public boolean needsRefill() {
        int localSize = localPool.size();
        int redisSize = redisPool.size();
        int minThreshold = shortUrlConfig.getPreGenerate().getMinThreshold();
        
        return localSize < minThreshold || redisSize < minThreshold * 2;
    }

    /**
     * 获取池状态信息
     */
    public PoolStatus getPoolStatus() {
        return PoolStatus.builder()
                .localPoolSize(localPool.size())
                .redisPoolSize(redisPool.size())
                .localPoolStats(localPool.getStats())
                .redisPoolStats(redisPool.getStats())
                .needsRefill(needsRefill())
                .build();
    }

    /**
     * 清空所有池
     */
    public void clearAllPools() {
        localPool.clear();
        redisPool.clear();
        log.info("已清空所有短链接池");
    }

    /**
     * 池状态信息
     */
    @lombok.Data
    @lombok.Builder
    public static class PoolStatus {
        private int localPoolSize;
        private int redisPoolSize;
        private String localPoolStats;
        private String redisPoolStats;
        private boolean needsRefill;
    }
}