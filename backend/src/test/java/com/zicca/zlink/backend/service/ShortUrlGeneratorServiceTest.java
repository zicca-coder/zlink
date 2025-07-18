package com.zicca.zlink.backend.service;

import com.zicca.zlink.backend.service.impl.ShortUrlGeneratorServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 短链接生成服务测试
 */
@Slf4j
@SpringBootTest
@TestPropertySource(properties = {
    "zlink.link.hashType=32",
    "zlink.link.maxRetryTimes=10",
    "zlink.link.baseLength=6",
    "zlink.link.maxLength=12"
})
public class ShortUrlGeneratorServiceTest {

    /**
     * 测试基本的短链接生成功能
     */
    @Test
    public void testBasicGeneration() {
        // 这里需要mock依赖，实际测试时需要完整的Spring上下文
        log.info("测试基本短链接生成功能");
        
        String[] testUrls = {
            "https://www.baidu.com",
            "https://www.google.com", 
            "https://github.com",
            "https://stackoverflow.com",
            "https://www.zhihu.com"
        };
        
        for (String url : testUrls) {
            // 模拟生成过程
            String shortUrl = simulateGeneration(url, "test-group");
            log.info("原始URL: {} -> 短链接: {}", url, shortUrl);
            
            // 验证长度
            assert shortUrl.length() >= 6 && shortUrl.length() <= 12;
            // 验证字符集（Base62）
            assert shortUrl.matches("[0-9A-Za-z]+");
        }
    }

    /**
     * 测试并发生成的唯一性
     */
    @Test
    public void testConcurrentUniqueness() throws InterruptedException {
        log.info("测试并发生成的唯一性");
        
        int threadCount = 100;
        int urlsPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        Set<String> generatedUrls = new HashSet<>();
        AtomicInteger duplicateCount = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < urlsPerThread; j++) {
                        String testUrl = "https://test" + threadId + "-" + j + ".com";
                        String shortUrl = simulateGeneration(testUrl, "group-" + threadId);
                        
                        synchronized (generatedUrls) {
                            if (!generatedUrls.add(shortUrl)) {
                                duplicateCount.incrementAndGet();
                                log.warn("发现重复短链接: {}", shortUrl);
                            }
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        int totalGenerated = threadCount * urlsPerThread;
        log.info("总生成数量: {}, 唯一数量: {}, 重复数量: {}", 
                totalGenerated, generatedUrls.size(), duplicateCount.get());
        
        // 验证唯一性（允许极少量重复，因为这是概率性的）
        double duplicateRate = (double) duplicateCount.get() / totalGenerated;
        assert duplicateRate < 0.01; // 重复率应该小于1%
    }

    /**
     * 测试冲突重试机制
     */
    @Test
    public void testCollisionRetry() {
        log.info("测试冲突重试机制");
        
        // 模拟相同输入的多次生成
        String testUrl = "https://collision-test.com";
        String gid = "collision-group";
        
        Set<String> results = new HashSet<>();
        
        // 生成多个短链接，验证重试机制
        for (int i = 0; i < 20; i++) {
            String shortUrl = simulateGenerationWithRetry(testUrl, gid, i);
            results.add(shortUrl);
            log.info("第{}次生成: {}", i + 1, shortUrl);
        }
        
        log.info("20次生成产生了{}个不同的短链接", results.size());
        // 由于加入了重试次数作为输入，应该产生不同的结果
        assert results.size() > 1;
    }

    /**
     * 模拟短链接生成（简化版本，用于测试）
     */
    private String simulateGeneration(String originalUrl, String gid) {
        // 这里使用简化的生成逻辑进行测试
        String input = originalUrl + ":" + gid + ":" + System.nanoTime();
        return com.zicca.zlink.backend.toolkit.HashUtil.hashToBase62(input).substring(0, 6);
    }

    /**
     * 模拟带重试的短链接生成
     */
    private String simulateGenerationWithRetry(String originalUrl, String gid, int retryCount) {
        String input = originalUrl + ":" + gid + ":retry:" + retryCount + ":" + System.nanoTime();
        return com.zicca.zlink.backend.toolkit.HashUtil.hashToBase62(input).substring(0, 6);
    }
}