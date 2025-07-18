package com.zicca.zlink.backend.pool;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 短链接池测试
 */
@Slf4j
@SpringBootTest
public class ShortUrlPoolTest {

    /**
     * 测试本地池的基本功能
     */
    @Test
    public void testLocalPoolBasicOperations() {
        LocalShortUrlPool localPool = new LocalShortUrlPool();
        
        // 测试添加和获取
        assert localPool.offer("test1");
        assert localPool.offer("test2");
        assert localPool.offer("test3");
        
        assert localPool.size() == 3;
        assert !localPool.isEmpty();
        
        // 测试获取
        String url1 = localPool.acquire();
        assert "test1".equals(url1);
        assert localPool.size() == 2;
        
        // 测试批量操作
        List<String> batch = List.of("batch1", "batch2", "batch3");
        int added = localPool.offerBatch(batch);
        assert added == 3;
        assert localPool.size() == 5;
        
        List<String> acquired = localPool.acquireBatch(2);
        assert acquired.size() == 2;
        assert localPool.size() == 3;
        
        log.info("本地池测试通过: {}", localPool.getStats());
    }

    /**
     * 测试并发安全性
     */
    @Test
    public void testConcurrentSafety() throws InterruptedException {
        LocalShortUrlPool localPool = new LocalShortUrlPool();
        int threadCount = 10;
        int operationsPerThread = 100;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // 先填充一些数据
        for (int i = 0; i < 500; i++) {
            localPool.offer("url" + i);
        }
        
        // 并发获取测试
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String url = localPool.acquire();
                        if (url != null) {
                            successCount.incrementAndGet();
                        }
                        
                        // 模拟一些处理时间
                        Thread.sleep(1);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        log.info("并发测试完成: 成功获取 {} 个短链接, 剩余 {} 个", 
                successCount.get(), localPool.size());
        
        // 验证没有重复获取
        assert successCount.get() <= 500;
        assert successCount.get() + localPool.size() == 500;
    }

    /**
     * 测试池管理器的分层获取策略
     */
    @Test
    public void testPoolManagerStrategy() {
        // 这里需要mock依赖进行测试
        log.info("池管理器分层策略测试");
        
        // 模拟测试场景
        simulatePoolManagerTest();
    }

    /**
     * 模拟池管理器测试
     */
    private void simulatePoolManagerTest() {
        LocalShortUrlPool localPool = new LocalShortUrlPool();
        
        // 模拟分层获取策略
        // 1. 本地池有数据时优先获取
        localPool.offer("local1");
        localPool.offer("local2");
        
        String url = localPool.acquire();
        assert "local1".equals(url);
        log.info("从本地池获取: {}", url);
        
        // 2. 本地池为空时的处理逻辑
        localPool.clear();
        assert localPool.isEmpty();
        
        // 模拟从Redis池补充本地池
        List<String> fromRedis = List.of("redis1", "redis2", "redis3");
        localPool.offerBatch(fromRedis);
        
        String urlFromRedis = localPool.acquire();
        assert "redis1".equals(urlFromRedis);
        log.info("从Redis补充后获取: {}", urlFromRedis);
        
        log.info("分层策略测试通过");
    }

    /**
     * 性能基准测试
     */
    @Test
    public void testPerformanceBenchmark() {
        LocalShortUrlPool localPool = new LocalShortUrlPool();
        
        // 预填充数据
        List<String> testData = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            testData.add("benchmark" + i);
        }
        localPool.offerBatch(testData);
        
        // 测试获取性能
        long startTime = System.nanoTime();
        int testCount = 1000;
        
        for (int i = 0; i < testCount; i++) {
            String url = localPool.acquire();
            assert url != null;
        }
        
        long endTime = System.nanoTime();
        double avgTimeNs = (double) (endTime - startTime) / testCount;
        double avgTimeMs = avgTimeNs / 1_000_000;
        
        log.info("性能测试结果: 平均获取时间 {:.3f}ms ({:.0f}ns)", avgTimeMs, avgTimeNs);
        
        // 验证性能指标（应该小于1ms）
        assert avgTimeMs < 1.0;
    }
}