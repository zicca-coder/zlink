package com.zicca.zlink.framework.service.impl;

import com.zicca.zlink.framework.idempotent.IdempotentStatus;
import com.zicca.zlink.framework.mapper.IdempotentRecordMapper;
import com.zicca.zlink.framework.service.IdempotentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 改进版幂等服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotentServiceImpl implements IdempotentService {

    private final IdempotentRecordMapper mapper;

    /**
     * 尝试插入幂等记录
     *
     * @param key 消息标识
     * @return
     */
    @Override
    public IdempotentStatus tryInsertKey(String key) {
        try {
            // 优先使用存储过程实现原子操作
            // result: 1=首次消费, 0=消费中, 2=已消费
            Integer result = mapper.tryInsertKeyAtomic(key);
            if (result == null || result == -1) {
                log.warn("存储过程执行异常或返回错误码，降级到INSERT IGNORE方案，key: {}", key);
                return tryInsertKeyFallback(key);
            }
            return convertResult(result);
            
        } catch (Exception e) {
            // 存储过程不可用时，降级到简单方案
            log.warn("存储过程调用失败，降级到INSERT IGNORE方案，key: {}, error: {}", key, e.getMessage());
            return tryInsertKeyFallback(key);
        }
    }
    
    /**
     * 降级方案：使用 INSERT IGNORE + 查询状态
     */
    private IdempotentStatus tryInsertKeyFallback(String key) {
        try {
            // 尝试插入，如果已存在则忽略
            int inserted = mapper.insertIgnore(key);
            
            if (inserted > 0) {
                // 插入成功，说明是首次消费
                return IdempotentStatus.FIRST_CONSUME;
            } else {
                // 插入失败，记录已存在，查询当前状态
                Integer status = mapper.selectStatus(key);
                if (status == null) {
                    log.warn("INSERT IGNORE失败但查询不到记录，可能存在并发问题: {}", key);
                    return IdempotentStatus.CONSUMING;
                }
                return status == 1 ? IdempotentStatus.ALREADY_CONSUMED : IdempotentStatus.CONSUMING;
            }
            
        } catch (Exception e) {
            log.error("降级方案执行失败，key: {}", key, e);
            // 最后的兜底：直接查询状态
            Integer status = mapper.selectStatus(key);
            return status != null && status == 1 ? 
                IdempotentStatus.ALREADY_CONSUMED : IdempotentStatus.CONSUMING;
        }
    }
    
    /**
     * 转换数据库返回结果为业务状态
     * @param result 1=首次消费, 0=消费中, 2=已消费
     */
    private IdempotentStatus convertResult(int result) {
        switch (result) {
            case 1:
                return IdempotentStatus.FIRST_CONSUME;
            case 0:
                return IdempotentStatus.CONSUMING;
            case 2:
                return IdempotentStatus.ALREADY_CONSUMED;
            default:
                log.warn("未知的数据库返回结果: {}", result);
                return IdempotentStatus.CONSUMING;
        }
    }

    @Override
    public void markConsumed(String key) {
        int updated = mapper.updateStatus(key, 1);
        if (updated == 0) {
            log.warn("标记消费完成失败，记录可能不存在: {}", key);
        }
    }

    @Override
    public void deleteKey(String key) {
        int deleted = mapper.deleteByKey(key);
        if (deleted == 0) {
            log.warn("删除幂等记录失败，记录可能不存在: {}", key);
        }
    }

    /**
     * 定时清理过期记录
     * 每小时执行一次，清理24小时前的已消费记录
     * 采用分批删除策略，避免大面积锁表
     */
    @Scheduled(fixedRate = 3600000) // 1小时
    public void cleanupExpiredRecords() {
        int totalCleaned = 0;
        int batchSize = 1000; // 每批删除1000条
        int maxBatches = 100; // 最多执行100批，防止无限循环
        
        try {
            for (int batch = 0; batch < maxBatches; batch++) {
                // 分批删除，每次删除少量数据
                int cleaned = mapper.deleteExpiredRecordsBatch(24, batchSize);
                totalCleaned += cleaned;
                
                if (cleaned == 0) {
                    // 没有更多数据需要删除
                    break;
                }
                
                // 批次间短暂休眠，释放数据库资源
                if (cleaned == batchSize) {
                    Thread.sleep(100); // 休眠100ms
                }
            }
            
            if (totalCleaned > 0) {
                log.info("清理过期幂等记录: {} 条", totalCleaned);
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("清理过期记录被中断");
        } catch (Exception e) {
            log.error("清理过期幂等记录失败，已清理: {} 条", totalCleaned, e);
        }
    }
    
    /**
     * 异步清理过期记录（可选方案）
     * 在业务低峰期执行，减少对正常业务的影响
     */
    @Scheduled(cron = "0 30 2 * * ?") // 每天凌晨2:30执行
    public void asyncCleanupExpiredRecords() {
        log.info("开始异步清理过期幂等记录");
        cleanupExpiredRecords();
    }
}