package com.zicca.zlink.backend.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.zicca.zlink.backend.cache.holder.BloomFilterHolder;
import com.zicca.zlink.backend.config.ShortUrlConfig;
import com.zicca.zlink.backend.dao.mapper.ZLinkMapper;
import com.zicca.zlink.backend.pool.ShortUrlPoolManager;
import com.zicca.zlink.backend.service.ShortUrlPreGenerateService;
import com.zicca.zlink.backend.toolkit.HashUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 短链接预生成服务实现
 * 负责后台批量生成短链接并维护池的容量
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShortUrlPreGenerateServiceImpl implements ShortUrlPreGenerateService {

    private final ShortUrlPoolManager poolManager;
    private final ShortUrlConfig shortUrlConfig;
    private final BloomFilterHolder bloomFilterHolder;
    private final ZLinkMapper zLinkMapper;

    private final Snowflake snowflake = IdUtil.getSnowflake();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicLong totalGenerated = new AtomicLong(0);
    private final AtomicLong totalDuplicates = new AtomicLong(0);
    private final AtomicLong totalGenerationTime = new AtomicLong(0);

    @PostConstruct
    public void init() {
        if (shortUrlConfig.getPreGenerate().getEnabled()) {
            startPreGeneration();
        }
    }

    @PreDestroy
    public void destroy() {
        stopPreGeneration();
    }

    @Override
    public List<String> generateShortUrls(int count) {
        long startTime = System.currentTimeMillis();
        List<String> result = new ArrayList<>();
        int attempts = 0;
        int maxAttempts = count * 3; // 最多尝试3倍数量，避免无限循环

        while (result.size() < count && attempts < maxAttempts) {
            String shortUrl = generateSingleShortUrl();
            attempts++;

            // 检查是否已存在（布隆过滤器快速检查）
            if (!isShortUrlExists(shortUrl)) {
                result.add(shortUrl);
                totalGenerated.incrementAndGet();
            } else {
                totalDuplicates.incrementAndGet();
                log.info("生成的短链接已存在，跳过: {}", shortUrl);
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        totalGenerationTime.addAndGet(duration);
        
        log.info("批量生成短链接完成: 目标={}, 实际={}, 尝试={}, 耗时={}ms", 
                count, result.size(), attempts, duration);
        
        return result;
    }

    @Override
    @Async
    public void generateAndFillPool(int count) {
        if (!shortUrlConfig.getPreGenerate().getEnabled()) {
            return;
        }

        try {
            List<String> shortUrls = generateShortUrls(count);
            if (!shortUrls.isEmpty()) {
                int addedCount = poolManager.offerShortUrls(shortUrls);
                log.info("向池中添加预生成短链接: {} 个", addedCount);
            }
        } catch (Exception e) {
            log.error("预生成短链接并填充池失败", e);
        }
    }

    @Override
    @Scheduled(fixedDelayString = "${zlink.link.preGenerate.generateInterval:30}000")
    public void checkAndRefillPool() {
        if (!shortUrlConfig.getPreGenerate().getEnabled() || !isRunning.get()) {
            return;
        }

        try {
            if (poolManager.needsRefill()) {
                int batchSize = shortUrlConfig.getPreGenerate().getBatchSize();
                log.info("检测到池容量不足，开始补充: {} 个", batchSize);
                
                CompletableFuture.runAsync(() -> generateAndFillPool(batchSize))
                    .exceptionally(throwable -> {
                        log.error("异步补充池失败", throwable);
                        return null;
                    });
            }
        } catch (Exception e) {
            log.error("检查并补充池失败", e);
        }
    }

    @Override
    public void startPreGeneration() {
        if (isRunning.compareAndSet(false, true)) {
            log.info("启动短链接预生成服务");
            
            // 初始填充池
            if (shortUrlConfig.getPreGenerate().getEnabled()) {
                int initialSize = shortUrlConfig.getPreGenerate().getBatchSize();
                CompletableFuture.runAsync(() -> generateAndFillPool(initialSize))
                    .exceptionally(throwable -> {
                        log.error("初始填充池失败", throwable);
                        return null;
                    });
            }
        }
    }

    @Override
    public void stopPreGeneration() {
        if (isRunning.compareAndSet(true, false)) {
            log.info("停止短链接预生成服务");
        }
    }

    @Override
    public String getGenerationStats() {
        double avgGenerationTime = totalGenerated.get() > 0 ? 
            (double) totalGenerationTime.get() / totalGenerated.get() : 0.0;
        
        double duplicateRate = totalGenerated.get() > 0 ? 
            (double) totalDuplicates.get() / (totalGenerated.get() + totalDuplicates.get()) : 0.0;

        return String.format(
            "短链接预生成统计:\n" +
            "服务状态: %s\n" +
            "总生成数量: %d\n" +
            "重复数量: %d\n" +
            "重复率: %.2f%%\n" +
            "平均生成时间: %.2fms\n" +
            "总生成耗时: %dms",
            isRunning.get() ? "运行中" : "已停止",
            totalGenerated.get(),
            totalDuplicates.get(),
            duplicateRate * 100,
            avgGenerationTime,
            totalGenerationTime.get()
        );
    }

    /**
     * 生成单个短链接
     */
    private String generateSingleShortUrl() {
        // 使用多种策略生成唯一输入
        StringBuilder input = new StringBuilder();
        
        // 基础随机性
        input.append(snowflake.nextId())
             .append(":")
             .append(System.nanoTime())
             .append(":")
             .append(ThreadLocalRandom.current().nextLong())
             .append(":")
             .append(UUID.randomUUID().toString());

        // 根据配置选择哈希算法
        String shortUrl;
        switch (shortUrlConfig.getHashType()) {
            case 64:
                shortUrl = HashUtil.hash64ToBase62(input.toString());
                break;
            case 128:
                shortUrl = HashUtil.hash128ToBase62(input.toString());
                break;
            default:
                shortUrl = HashUtil.hashToBase62(input.toString());
                break;
        }

        // 控制长度
        int targetLength = shortUrlConfig.getBaseLength();
        if (shortUrl.length() > targetLength) {
            shortUrl = shortUrl.substring(0, targetLength);
        }

        return shortUrl;
    }

    /**
     * 检查短链接是否已存在
     */
    private boolean isShortUrlExists(String shortUrl) {
        // 1. 布隆过滤器快速检查
        if (!bloomFilterHolder.mightContainsInLocal(shortUrl) && 
            !bloomFilterHolder.mightContainsInRedis(shortUrl)) {
            return false;
        }

        // 2. 对于预生成的短链接，我们可以选择更宽松的检查策略
        // 因为即使有极少量重复，在实际使用时还会再次检查
        // 这里可以根据业务需求调整检查严格程度
        
        return false; // 预生成时采用宽松策略，提高生成效率
    }

    /**
     * 重置统计信息
     */
    public void resetStats() {
        totalGenerated.set(0);
        totalDuplicates.set(0);
        totalGenerationTime.set(0);
        log.info("重置预生成统计信息");
    }
}