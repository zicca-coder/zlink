package com.zicca.zlink.backend.controller;

import com.zicca.zlink.backend.pool.ShortUrlPoolManager;
import com.zicca.zlink.backend.service.ShortUrlPreGenerateService;
import com.zicca.zlink.framework.execption.ServiceException;
import com.zicca.zlink.framework.result.Result;
import com.zicca.zlink.framework.web.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 短链接预生成池管理接口
 */
@Tag(name = "短链接预生成池", description = "短链接预生成池管理相关接口")
@RestController
@RequestMapping("backend/api/v1/pool/shorturl")
@RequiredArgsConstructor
public class ShortUrlPoolController {

    private final ShortUrlPoolManager poolManager;
    private final ShortUrlPreGenerateService preGenerateService;

    @Operation(summary = "获取池状态信息")
    @GetMapping("/status")
    public Result<ShortUrlPoolManager.PoolStatus> getPoolStatus() {
        return Results.success(poolManager.getPoolStatus());
    }

    @Operation(summary = "手动触发池补充")
    @PostMapping("/refill")
    public Result<String> refillPool(
            @Parameter(description = "补充数量，默认使用配置的批量大小")
            @RequestParam(required = false) Integer count) {
        
        int generateCount = count != null ? count : 500;
        preGenerateService.generateAndFillPool(generateCount);
        
        return Results.success("已触发池补充任务，生成数量: " + generateCount);
    }

    @Operation(summary = "批量获取短链接")
    @GetMapping("/acquire")
    public Result<List<String>> acquireShortUrls(
            @Parameter(description = "获取数量")
            @RequestParam(defaultValue = "10") Integer count) {
        
        if (count > 100) {
            throw new ServiceException("单次获取数量不能超过100");
        }
        
        List<String> shortUrls = poolManager.acquireShortUrls(count);
        return Results.success(shortUrls);
    }

    @Operation(summary = "向池中添加短链接")
    @PostMapping("/offer")
    public Result<String> offerShortUrls(
            @Parameter(description = "短链接列表")
            @RequestBody List<String> shortUrls) {
        
        if (shortUrls == null || shortUrls.isEmpty()) {
            throw new ServiceException("短链接列表不能为空");
        }
        
        if (shortUrls.size() > 1000) {
            throw new ServiceException("单次添加数量不能超过1000");
        }
        
        int addedCount = poolManager.offerShortUrls(shortUrls);
        return Results.success("成功添加 " + addedCount + " 个短链接到池中");
    }

    @Operation(summary = "清空所有池")
    @DeleteMapping("/clear")
    public Result<String> clearAllPools() {
        poolManager.clearAllPools();
        return Results.success("已清空所有短链接池");
    }

    @Operation(summary = "获取预生成统计信息")
    @GetMapping("/generation-stats")
    public Result<String> getGenerationStats() {
        return Results.success(preGenerateService.getGenerationStats());
    }

    @Operation(summary = "启动预生成服务")
    @PostMapping("/start")
    public Result<String> startPreGeneration() {
        preGenerateService.startPreGeneration();
        return Results.success("预生成服务已启动");
    }

    @Operation(summary = "停止预生成服务")
    @PostMapping("/stop")
    public Result<String> stopPreGeneration() {
        preGenerateService.stopPreGeneration();
        return Results.success("预生成服务已停止");
    }

    @Operation(summary = "获取池健康状态")
    @GetMapping("/health")
    public Result<Map<String, Object>> getPoolHealth() {
        ShortUrlPoolManager.PoolStatus status = poolManager.getPoolStatus();
        
        Map<String, Object> health = new HashMap<>();
        health.put("localPoolSize", status.getLocalPoolSize());
        health.put("redisPoolSize", status.getRedisPoolSize());
        health.put("needsRefill", status.isNeedsRefill());
        
        // 健康状态评估
        String healthStatus = "HEALTHY";
        if (status.getLocalPoolSize() == 0 && status.getRedisPoolSize() == 0) {
            healthStatus = "CRITICAL";
        } else if (status.isNeedsRefill()) {
            healthStatus = "WARNING";
        }
        
        health.put("status", healthStatus);
        health.put("recommendations", getHealthRecommendations(status));
        
        return Results.success(health);
    }

    @Operation(summary = "获取池配置信息")
    @GetMapping("/config")
    public Result<Map<String, Object>> getPoolConfig() {
        // 这里可以返回当前的池配置信息
        Map<String, Object> config = new HashMap<>();
        config.put("message", "池配置信息需要从ShortUrlConfig中获取");
        return Results.success(config);
    }

    /**
     * 根据池状态提供健康建议
     */
    private String getHealthRecommendations(ShortUrlPoolManager.PoolStatus status) {
        if (status.getLocalPoolSize() == 0 && status.getRedisPoolSize() == 0) {
            return "所有池都为空，建议立即触发补充或检查预生成服务状态";
        }
        if (status.isNeedsRefill()) {
            return "池容量不足，建议增加预生成频率或批量大小";
        }
        if (status.getLocalPoolSize() > 5000) {
            return "本地池容量过大，建议调整本地池大小配置";
        }
        return "池状态正常";
    }
}