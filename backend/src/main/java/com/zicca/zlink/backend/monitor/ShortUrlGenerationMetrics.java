package com.zicca.zlink.backend.monitor;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 短链接生成指标监控
 */
@Slf4j
@Data
@Component
public class ShortUrlGenerationMetrics {

    // 总生成次数
    private final LongAdder totalGenerations = new LongAdder();
    
    // 成功生成次数
    private final LongAdder successfulGenerations = new LongAdder();
    
    // 冲突次数
    private final LongAdder collisionCount = new LongAdder();
    
    // 重试次数统计
    private final LongAdder totalRetries = new LongAdder();
    
    // 使用兜底策略的次数
    private final LongAdder fallbackCount = new LongAdder();
    
    // 平均生成时间（毫秒）
    private final AtomicLong totalGenerationTime = new AtomicLong(0);
    
    // 最大重试次数记录
    private final AtomicLong maxRetryCount = new AtomicLong(0);

    /**
     * 记录生成开始
     */
    public void recordGenerationStart() {
        totalGenerations.increment();
    }

    /**
     * 记录生成成功
     */
    public void recordGenerationSuccess(long durationMs, int retryCount) {
        successfulGenerations.increment();
        totalGenerationTime.addAndGet(durationMs);
        totalRetries.add(retryCount);
        
        // 更新最大重试次数
        long currentMax = maxRetryCount.get();
        while (retryCount > currentMax && !maxRetryCount.compareAndSet(currentMax, retryCount)) {
            currentMax = maxRetryCount.get();
        }
    }

    /**
     * 记录冲突
     */
    public void recordCollision() {
        collisionCount.increment();
    }

    /**
     * 记录使用兜底策略
     */
    public void recordFallback() {
        fallbackCount.increment();
    }

    /**
     * 获取成功率
     */
    public double getSuccessRate() {
        long total = totalGenerations.sum();
        return total > 0 ? (double) successfulGenerations.sum() / total : 0.0;
    }

    /**
     * 获取冲突率
     */
    public double getCollisionRate() {
        long total = totalGenerations.sum();
        return total > 0 ? (double) collisionCount.sum() / total : 0.0;
    }

    /**
     * 获取平均生成时间
     */
    public double getAverageGenerationTime() {
        long successful = successfulGenerations.sum();
        return successful > 0 ? (double) totalGenerationTime.get() / successful : 0.0;
    }

    /**
     * 获取平均重试次数
     */
    public double getAverageRetryCount() {
        long successful = successfulGenerations.sum();
        return successful > 0 ? (double) totalRetries.sum() / successful : 0.0;
    }

    /**
     * 获取统计报告
     */
    public String getMetricsReport() {
        return String.format(
            "短链接生成统计报告:\n" +
            "总生成次数: %d\n" +
            "成功生成次数: %d\n" +
            "成功率: %.2f%%\n" +
            "冲突次数: %d\n" +
            "冲突率: %.2f%%\n" +
            "兜底策略使用次数: %d\n" +
            "平均生成时间: %.2fms\n" +
            "平均重试次数: %.2f\n" +
            "最大重试次数: %d",
            totalGenerations.sum(),
            successfulGenerations.sum(),
            getSuccessRate() * 100,
            collisionCount.sum(),
            getCollisionRate() * 100,
            fallbackCount.sum(),
            getAverageGenerationTime(),
            getAverageRetryCount(),
            maxRetryCount.get()
        );
    }

    /**
     * 重置所有指标
     */
    public void reset() {
        totalGenerations.reset();
        successfulGenerations.reset();
        collisionCount.reset();
        totalRetries.reset();
        fallbackCount.reset();
        totalGenerationTime.set(0);
        maxRetryCount.set(0);
        log.info("短链接生成指标已重置");
    }
}