package com.zicca.zlink.backend.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zicca.zlink.backend.cache.holder.BloomFilterHolder;
import com.zicca.zlink.backend.cache.holder.CacheHolder;
import com.zicca.zlink.backend.config.ShortUrlConfig;
import com.zicca.zlink.backend.dao.entity.ZLink;
import com.zicca.zlink.backend.dao.mapper.ZLinkMapper;
import com.zicca.zlink.backend.monitor.ShortUrlGenerationMetrics;
import com.zicca.zlink.backend.service.ShortUrlGeneratorService;
import com.zicca.zlink.backend.toolkit.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 短链接生成服务实现
 * 多层次冲突解决策略：
 * 1. 基础策略：UUID/雪花ID + MurmurHash + Base62
 * 2. 冲突检测：布隆过滤器 + 数据库查询
 * 3. 冲突解决：盐值重试 -> 时间戳后缀 -> 随机后缀 -> 长度递增
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShortUrlGeneratorServiceImpl implements ShortUrlGeneratorService {

    private final ZLinkMapper zLinkMapper;
    private final CacheHolder cacheHolder;
    private final BloomFilterHolder bloomFilterHolder;
    private final ShortUrlConfig shortUrlConfig;
    private final ShortUrlGenerationMetrics metrics;

    // 雪花算法实例
    private final Snowflake snowflake = IdUtil.getSnowflake();

    @Override
    public String generateUniqueShortUrl(String originalUrl, String gid) {
        long startTime = System.currentTimeMillis();
        metrics.recordGenerationStart();

        String shortUrl = null;
        int retryCount = 0;
        int currentLength = shortUrlConfig.getBaseLength();

        while (retryCount < shortUrlConfig.getMaxRetryTimes() && currentLength <= shortUrlConfig.getMaxLength()) {
            try {
                // 策略1: 基础生成策略
                shortUrl = generateBaseShortUrl(originalUrl, gid, retryCount);

                // 长度控制
                if (shortUrl.length() > currentLength) {
                    shortUrl = shortUrl.substring(0, currentLength);
                }

                // 冲突检测
                if (!isShortUrlExists(shortUrl)) {
                    long duration = System.currentTimeMillis() - startTime;
                    metrics.recordGenerationSuccess(duration, retryCount);
                    log.info("生成短链接成功: {} (重试次数: {}, 长度: {}, 耗时: {}ms)",
                            shortUrl, retryCount, shortUrl.length(), duration);
                    return shortUrl;
                }

                log.info("短链接冲突，进行重试: {} (第{}次重试)", shortUrl, retryCount + 1);
                retryCount++;

                // 记录冲突
                metrics.recordCollision();

                // 策略升级：每3次重试增加一位长度
                if (retryCount % 3 == 0 && currentLength < shortUrlConfig.getMaxLength()) {
                    currentLength++;
                    log.info("增加短链接长度至: {}", currentLength);
                }

            } catch (Exception e) {
                log.info("生成短链接异常，重试中: {}", e.getMessage());
                retryCount++;
            }
        }

        // 最后兜底：使用时间戳 + 随机数
        metrics.recordFallback();
        shortUrl = generateFallbackShortUrl();
        long duration = System.currentTimeMillis() - startTime;
        metrics.recordGenerationSuccess(duration, retryCount);
        log.warn("使用兜底策略生成短链接: {} (耗时: {}ms)", shortUrl, duration);
        return shortUrl;
    }

    @Override
    public boolean isShortUrlExists(String shortUrl) {
        // 1. 先检查布隆过滤器（快速排除不存在的情况）
        if (!bloomFilterHolder.mightContainsInLocal(shortUrl) &&
                !bloomFilterHolder.mightContainsInRedis(shortUrl)) {
            return false;
        }
        // 2. 再查缓存（低延迟）
        String cache = cacheHolder.getFromCache(shortUrl);
        if (StrUtil.isNotBlank(cache)) {
            return true;
        }
        // 3. 数据库精确查询
        LambdaQueryWrapper<ZLink> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ZLink::getShortUrl, shortUrl)
                .or()
                .eq(ZLink::getShortUri, shortUrl);

        return zLinkMapper.selectCount(queryWrapper) > 0;
    }

    /**
     * 基础短链接生成策略
     */
    private String generateBaseShortUrl(int retryCount) {
        String baseInput = buildBaseInput(retryCount);

        switch (shortUrlConfig.getHashType()) {
            case 64:
                return HashUtil.hash64ToBase62(baseInput);
            case 128:
                return HashUtil.hash128ToBase62(baseInput);
            default:
                return HashUtil.hashToBase62(baseInput);
        }
    }

    /**
     * 基础短链接生成策略
     */
    private String generateBaseShortUrl(String originalUrl, String gid, int retryCount) {
        String baseInput = buildBaseInput(originalUrl, gid, retryCount);

        switch (shortUrlConfig.getHashType()) {
            case 64:
                return HashUtil.hash64ToBase62(baseInput);
            case 128:
                return HashUtil.hash128ToBase62(baseInput);
            default:
                return HashUtil.hashToBase62(baseInput);
        }
    }

    /**
     * 构建基础输入字符串
     */
    private String buildBaseInput(int retryCount) {
        StringBuilder inputBuilder = new StringBuilder();

        // 根据重试次数选择不同的唯一性策略
        if (retryCount == 0) {
            // 第1次：使用雪花ID
            inputBuilder.append(snowflake.nextId());
        } else if (retryCount <= 3) {
            // 第2-4次：使用UUID + 盐值
            inputBuilder.append(UUID.randomUUID().toString())
                    .append(":salt").append(retryCount);
        } else if (retryCount <= 6) {
            // 第5-7次：使用时间戳 + 随机数
            inputBuilder.append(System.nanoTime())
                    .append(":").append(ThreadLocalRandom.current().nextLong());
        } else {
            // 第8次以后：使用MD5增强随机性 \ 直接原始ID 编码，保证唯一，但是较长
            String randomStr = RandomUtil.randomString(16);
            inputBuilder.append(getMD5Hash(randomStr + retryCount));
        }

        return inputBuilder.toString();
    }

    /**
     * 构建基础输入字符串
     */
    private String buildBaseInput(String originalUrl, String gid, int retryCount) {
        StringBuilder inputBuilder = new StringBuilder();

        // 基础信息
        inputBuilder.append(originalUrl);
        if (StrUtil.isNotBlank(gid)) {
            inputBuilder.append(":").append(gid);
        }

        // 根据重试次数选择不同的唯一性策略
        if (retryCount == 0) {
            // 第1次：使用雪花ID
            inputBuilder.append(":").append(snowflake.nextId());
        } else if (retryCount <= 3) {
            // 第2-4次：使用UUID + 盐值
            inputBuilder.append(":").append(UUID.randomUUID().toString())
                    .append(":salt").append(retryCount);
        } else if (retryCount <= 6) {
            // 第5-7次：使用时间戳 + 随机数
            inputBuilder.append(":").append(System.nanoTime())
                    .append(":").append(ThreadLocalRandom.current().nextLong());
        } else {
            // 第8次以后：使用MD5增强随机性 \ 直接原始ID 编码，保证唯一，但是较长
            String randomStr = RandomUtil.randomString(16);
            inputBuilder.append(":").append(getMD5Hash(randomStr + retryCount));
        }

        return inputBuilder.toString();
    }

    /**
     * 兜底策略：时间戳 + 随机数
     */
    private String generateFallbackShortUrl() {
        long timestamp = System.currentTimeMillis();
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        String fallbackInput = timestamp + ":" + random + ":" + UUID.randomUUID().toString();

        return HashUtil.hashToBase62(fallbackInput).substring(0, Math.min(8, shortUrlConfig.getMaxLength()));
    }

    /**
     * MD5哈希
     */
    private String getMD5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("MD5算法不可用", e);
            return String.valueOf(input.hashCode());
        }
    }
}