package com.zicca.zlink.backend.controller;

import com.zicca.zlink.backend.monitor.ShortUrlGenerationMetrics;
import com.zicca.zlink.framework.result.Result;
import com.zicca.zlink.framework.web.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 短链接生成指标监控接口
 */
@Tag(name = "短链接指标监控", description = "短链接生成性能监控相关接口")
@RestController
@RequestMapping("/backend/api/v1/metrics/shorturl")
@RequiredArgsConstructor
public class ShortUrlMetricsController {

    private final ShortUrlGenerationMetrics metrics;

    @Operation(summary = "获取短链接生成统计信息")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getGenerationStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalGenerations", metrics.getTotalGenerations().sum());
        stats.put("successfulGenerations", metrics.getSuccessfulGenerations().sum());
        stats.put("successRate", String.format("%.2f%%", metrics.getSuccessRate() * 100));
        stats.put("collisionCount", metrics.getCollisionCount().sum());
        stats.put("collisionRate", String.format("%.2f%%", metrics.getCollisionRate() * 100));
        stats.put("fallbackCount", metrics.getFallbackCount().sum());
        stats.put("averageGenerationTime", String.format("%.2fms", metrics.getAverageGenerationTime()));
        stats.put("averageRetryCount", String.format("%.2f", metrics.getAverageRetryCount()));
        stats.put("maxRetryCount", metrics.getMaxRetryCount().get());
        
        return Results.success(stats);
    }

    @Operation(summary = "获取详细统计报告")
    @GetMapping("/report")
    public Result<String> getDetailedReport() {
        return Results.success(metrics.getMetricsReport());
    }

    @Operation(summary = "重置统计指标")
    @PostMapping("/reset")
    public Result<Void> resetMetrics() {
        metrics.reset();
        return Results.success();
    }

    @Operation(summary = "获取系统健康状态")
    @GetMapping("/health")
    public Result<Map<String, Object>> getHealthStatus() {
        Map<String, Object> health = new HashMap<>();
        
        double successRate = metrics.getSuccessRate();
        double collisionRate = metrics.getCollisionRate();
        double avgRetryCount = metrics.getAverageRetryCount();
        
        // 健康状态评估
        String status = "HEALTHY";
        if (successRate < 0.95) {
            status = "UNHEALTHY";
        } else if (collisionRate > 0.1 || avgRetryCount > 3) {
            status = "WARNING";
        }
        
        health.put("status", status);
        health.put("successRate", successRate);
        health.put("collisionRate", collisionRate);
        health.put("averageRetryCount", avgRetryCount);
        health.put("recommendations", getRecommendations(successRate, collisionRate, avgRetryCount));
        
        return Results.success(health);
    }

    /**
     * 根据指标提供优化建议
     */
    private String getRecommendations(double successRate, double collisionRate, double avgRetryCount) {
        if (successRate < 0.95) {
            return "成功率过低，建议检查数据库连接和布隆过滤器配置";
        }
        if (collisionRate > 0.1) {
            return "冲突率过高，建议增加短链接长度或调整哈希算法";
        }
        if (avgRetryCount > 3) {
            return "平均重试次数过高，建议优化唯一性策略或增加初始长度";
        }
        return "系统运行正常";
    }
}