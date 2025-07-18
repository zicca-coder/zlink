package com.zicca.zlink.backend.service;

import java.util.List;

/**
 * 短链接预生成服务接口
 */
public interface ShortUrlPreGenerateService {

    /**
     * 生成指定数量的短链接
     *
     * @param count 生成数量
     * @return 生成的短链接列表
     */
    List<String> generateShortUrls(int count);

    /**
     * 异步生成短链接并添加到池中
     *
     * @param count 生成数量
     */
    void generateAndFillPool(int count);

    /**
     * 检查并补充池
     */
    void checkAndRefillPool();

    /**
     * 启动预生成任务
     */
    void startPreGeneration();

    /**
     * 停止预生成任务
     */
    void stopPreGeneration();

    /**
     * 获取预生成统计信息
     *
     * @return 统计信息
     */
    String getGenerationStats();
}