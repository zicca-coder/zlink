package com.zicca.zlink.backend.pool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 本地短链接池实现
 * 使用内存队列存储预生成的短链接
 */
@Slf4j
@Component
public class LocalShortUrlPool implements ShortUrlPool {

    private final ConcurrentLinkedQueue<String> pool = new ConcurrentLinkedQueue<>();
    private final AtomicLong totalAcquired = new AtomicLong(0);
    private final AtomicLong totalOffered = new AtomicLong(0);

    @Override
    public String acquire() {
        String shortUrl = pool.poll();
        if (shortUrl != null) {
            totalAcquired.incrementAndGet();
            log.debug("从本地池获取短链接: {}, 剩余: {}", shortUrl, pool.size());
        }
        return shortUrl;
    }

    @Override
    public List<String> acquireBatch(int count) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String shortUrl = pool.poll();
            if (shortUrl == null) {
                break;
            }
            result.add(shortUrl);
        }
        
        if (!result.isEmpty()) {
            totalAcquired.addAndGet(result.size());
            log.debug("从本地池批量获取短链接: {} 个, 剩余: {}", result.size(), pool.size());
        }
        
        return result;
    }

    @Override
    public boolean offer(String shortUrl) {
        if (shortUrl == null || shortUrl.trim().isEmpty()) {
            return false;
        }
        
        boolean success = pool.offer(shortUrl);
        if (success) {
            totalOffered.incrementAndGet();
            log.debug("向本地池添加短链接: {}, 总数: {}", shortUrl, pool.size());
        }
        return success;
    }

    @Override
    public int offerBatch(List<String> shortUrls) {
        if (shortUrls == null || shortUrls.isEmpty()) {
            return 0;
        }
        
        int successCount = 0;
        for (String shortUrl : shortUrls) {
            if (offer(shortUrl)) {
                successCount++;
            }
        }
        
        log.debug("向本地池批量添加短链接: {} 个, 总数: {}", successCount, pool.size());
        return successCount;
    }

    @Override
    public int size() {
        return pool.size();
    }

    @Override
    public boolean isEmpty() {
        return pool.isEmpty();
    }

    @Override
    public void clear() {
        int oldSize = pool.size();
        pool.clear();
        log.info("清空本地短链接池, 清除数量: {}", oldSize);
    }

    @Override
    public String getStats() {
        return String.format("本地短链接池统计 - 当前数量: %d, 总获取: %d, 总添加: %d", 
                pool.size(), totalAcquired.get(), totalOffered.get());
    }

    /**
     * 重置统计信息
     */
    public void resetStats() {
        totalAcquired.set(0);
        totalOffered.set(0);
        log.info("重置本地池统计信息");
    }
}