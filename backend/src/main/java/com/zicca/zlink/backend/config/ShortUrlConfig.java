package com.zicca.zlink.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 短链接生成配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "zlink.link")
public class ShortUrlConfig {

    /**
     * 哈希类型 (32/64/128)
     */
    private Integer hashType = 32;

    /**
     * 最大重试次数
     */
    private Integer maxRetryTimes = 10;

    /**
     * 基础短链长度
     */
    private Integer baseLength = 6;

    /**
     * 最大短链长度
     */
    private Integer maxLength = 12;

    /**
     * 是否启用分布式锁
     */
    private Boolean enableDistributedLock = true;

    /**
     * 锁超时时间（秒）
     */
    private Integer lockTimeoutSeconds = 10;

    /**
     * 是否启用性能监控
     */
    private Boolean enableMetrics = true;

    /**
     * 预生成池配置
     */
    private PreGenerate preGenerate = new PreGenerate();

    @Data
    public static class PreGenerate {
        /**
         * 是否启用预生成池
         */
        private Boolean enabled = true;

        /**
         * 本地池大小
         */
        private Integer localPoolSize = 1000;

        /**
         * Redis池大小
         */
        private Integer redisPoolSize = 10000;

        /**
         * 最小阈值，低于此值触发补充
         */
        private Integer minThreshold = 100;

        /**
         * 每次批量生成数量
         */
        private Integer batchSize = 500;

        /**
         * 生成间隔（秒）
         */
        private Integer generateInterval = 30;

        /**
         * 单次生成最大耗时（毫秒）
         */
        private Integer maxGenerateTime = 5000;
    }
}