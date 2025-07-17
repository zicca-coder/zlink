package com.zicca.zlink.backend.cache.service;

import cn.hutool.core.collection.CollectionUtil;
import com.zicca.zlink.backend.common.constant.RedisKeyConstants;
import com.zicca.zlink.framework.execption.ServiceException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j(topic = "RedisBloomFilterService")
@Service
@RequiredArgsConstructor
public class RedisBloomFilterService implements BloomFilterService {

    private final RedissonClient redissonClient;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${zlink.bloom.expectedInsertions}")
    private long expectedInsertions;
    @Value("${zlink.bloom.falseProbability}")
    private double falseProbability;

    private final Executor bloomFilterExecutor;

    private RBloomFilter<String> bloomFilter;

    @Override
    @PostConstruct
    public void init() {
        // 使用统一的布隆过滤器名称
        bloomFilter = redissonClient.getBloomFilter(RedisKeyConstants.BLOOM_FILTER_NAME);
        try {
            // 如果布隆过滤器不存在，则初始化
            if (!bloomFilter.isExists()) {
                bloomFilter.tryInit(expectedInsertions, falseProbability);
                log.info(">>>Redis布隆过滤器初始化完成: expectedInsertions={}, falseProbability={}", expectedInsertions, falseProbability);
            }
        } catch (Exception e) {
            log.error(">>>Redis布隆过滤器初始化失败: {}", e.getMessage());
            throw new ServiceException("Redis布隆过滤器初始化失败");
        }
    }

    @Override
    public boolean mightContains(String key) {
        try {
            return bloomFilter.contains(key);
        } catch (Exception e) {
            log.error(">>>Redis布隆过滤器查询失败: key={}", key, e);
            // 若查询失败，则返回true，交由后续缓存判断是否存在
            return true;
        }
    }

    @Override
    public void add(String key) {
        try {
            bloomFilter.add(key);
            // 同步添加到Redis set中，用于与本地布隆过滤器同步
            // todo: 大key问题
            redisTemplate.opsForSet().add(RedisKeyConstants.BLOOM_FILTER_KEY, key);
            log.info(">>>Redis布隆过滤器添加成功: key={}", key);
        } catch (Exception e) {
            log.error(">>>Redis布隆过滤器添加失败: key={}, error={}", key, e.getMessage());
        }
    }

    public void addAsync(String key) {
        CompletableFuture.runAsync(() -> add(key), bloomFilterExecutor)
                .exceptionally(throwable -> {
                    log.error(">>>Redis布隆过滤器异步添加失败: key={}, error={}", key, throwable.getMessage());
                    return null;
                });
    }

    public void addBatch(Collection<String> keys) {
        CompletableFuture.runAsync(() -> {
            try {
                keys.forEach(key -> bloomFilter.add(key));
                if (CollectionUtil.isNotEmpty(keys)) {
                    redisTemplate.opsForSet().add(RedisKeyConstants.BLOOM_FILTER_KEY, keys.toArray(new String[0]));
                }
                log.info(">>>Redis布隆过滤器批量添加成功: counts={}", keys.size());
            } catch (Exception e) {
                log.error(">>>Redis布隆过滤器批量添加失败: keys={}, error={}", keys, e.getMessage());
            }
        }, bloomFilterExecutor);
    }

    @Override
    public String getStats() {
        try {
            long count = bloomFilter.count();
            return String.format(">>>Redis布隆过滤器统计信息: count=%d, expectedInsertions=%d, falseProbability=%f",
                    count, expectedInsertions, falseProbability);
        } catch (Exception e) {
            log.error(">>>Redis布隆过滤器统计信息获取失败: {}", e.getMessage());
            return String.format(">>>Redis布隆过滤器统计信息获取失败: %s", e.getMessage());
        }
    }
}
