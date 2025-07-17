package com.zicca.zlink.backend.cache.service;

import cn.hutool.core.collection.CollectionUtil;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.zicca.zlink.backend.common.constant.RedisKeyConstants;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j(topic = "LocalBloomFilterService")
@Service
@RequiredArgsConstructor
public class LocalBloomFilterService implements BloomFilterService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${zlink.bloom.expectedInsertions}")
    private long expectedInsertions;
    @Value("${zlink.bloom.falseProbability}")
    private double falseProbability;
    @Value("${zlink.redis.fetch-size}")
    private int fetchSize;

    private BloomFilter<String> localBloomFilter;
    private final ConcurrentLinkedQueue<String> pendingSync = new ConcurrentLinkedQueue<>();
    private final AtomicLong syncCounter = new AtomicLong(0);


    @Override
    @PostConstruct
    public void init() {
        localBloomFilter = BloomFilter.create(
                Funnels.stringFunnel(Charset.defaultCharset()),
                expectedInsertions,
                falseProbability
        );
        loadFromRedis();
        log.info(">>>初始化本地布隆过滤器完成");
    }

    @Override
    public boolean mightContains(String key) {
        try {
            return localBloomFilter.mightContain(key);
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    public void add(String key) {
        try {
            localBloomFilter.put(key);
            pendingSync.offer(key);
            log.info(">>>添加数据到本地布隆过滤器: {}", key);
        } catch (Exception e) {
            log.error(">>>添加数据到本地布隆过滤器失败: {}", key, e);
        }
    }

    @Override
    public String getStats() {
        return String.format(">>>本地布隆过滤器 - 预期容量: %d, 误判率: %.4f, 已同步: %d, 待同步: %d",
                expectedInsertions, falseProbability, syncCounter.get(), pendingSync.size());
    }

    @Scheduled(fixedRate = 5000)
    public void syncToRedis() {
        if (CollectionUtil.isEmpty(pendingSync)) {
            return;
        }

        try {
            int batchSize = Math.min(fetchSize, pendingSync.size());
            String[] batch = new String[batchSize];
            for (int i = 0; i < batchSize && CollectionUtil.isNotEmpty(pendingSync); i++) {
                batch[i] = pendingSync.poll();
            }

            if (batch.length > 0) {
                redisTemplate.opsForSet().add(RedisKeyConstants.BLOOM_FILTER_KEY, batch);
                syncCounter.addAndGet(batch.length);
                log.info(">>>同步数据到Redis完成: count={}", batch.length);
            }
        } catch (Exception e) {
            log.error(">>>同步数据到Redis失败", e);
        }

    }

    private void loadFromRedis() {
        try {
            long count = loadFromRedisWithScan(RedisKeyConstants.BLOOM_FILTER_KEY);
            if (count > 0) {
                log.info(">>>从Redis加载本地布隆过滤器数据完成: count={}", count);
            } else {
                log.warn(">>>从Redis加载本地布隆过滤器数据为空");
            }
        } catch (Exception e) {
            log.error(">>>从Redis加载本地布隆过滤器数据失败", e);
        }
    }


    private long loadFromRedisWithScan(String key) {
        long totalCount = 0;
        try {
            ScanOptions scanOptions = ScanOptions.scanOptions().match("*").count(fetchSize).build();
            Cursor<String> cursor = redisTemplate.opsForSet().scan(key, scanOptions);
            List<String> batch = new ArrayList<>();
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                totalCount++;
                if (batch.size() >= fetchSize) {
                    processBatch(batch);
                    totalCount += batch.size();
                    batch.clear();
                    Thread.yield();
                }
            }
            if (CollectionUtil.isNotEmpty(batch)) {
                processBatch(batch);
            }
            cursor.close();
        } catch (Exception e) {
            log.error(">>>分批加载Redis数据失败: key={}", key, e);
        }
        return totalCount;
    }



    /**
     * 处理一批数据
     *
     * @param batch 一批数据
     */
    private void processBatch(List<String> batch) {
        batch.forEach(key -> localBloomFilter.put(key));
        log.info(">>>处理一批数据：{}条", batch.size());
    }

    /**
     * 异步同步数据到本地key
     *
     * @param sourceKey 源key
     * @param targetKey 目标key
     */
    private void asyncSyncToLocalKey(String sourceKey, String targetKey) {
        CompletableFuture.runAsync(() -> {
            try {
                ScanOptions scanOptions = ScanOptions.scanOptions().match("*").count(1000).build();
                Cursor<String> cursor = redisTemplate.opsForSet().scan(sourceKey, scanOptions);
                ArrayList<String> batch = new ArrayList<>();

                while (cursor.hasNext()) {
                    batch.add(cursor.next());

                    if (batch.size() >= 1000) {
                        redisTemplate.opsForSet().add(targetKey, batch.toArray(new String[0]));
                        batch.clear();
                    }
                }
                if (CollectionUtil.isNotEmpty(batch)) {
                    redisTemplate.opsForSet().add(targetKey, batch.toArray(new String[0]));
                }
                cursor.close();
                log.info(">>>异步同步数据到本地key完成: sourceKey={}, targetKey={}", sourceKey, targetKey);
            } catch (Exception e) {
                log.error(">>>异步同步数据到本地key异常", e);
            }
        });
    }



}
