package com.zicca.zlink.backend.pool;

import java.util.List;

/**
 * 短链接池接口
 */
public interface ShortUrlPool {

    /**
     * 从池中获取一个短链接
     *
     * @return 短链接，如果池为空返回null
     */
    String acquire();

    /**
     * 批量从池中获取短链接
     *
     * @param count 获取数量
     * @return 短链接列表
     */
    List<String> acquireBatch(int count);

    /**
     * 向池中添加短链接
     *
     * @param shortUrl 短链接
     * @return 是否添加成功
     */
    boolean offer(String shortUrl);

    /**
     * 批量向池中添加短链接
     *
     * @param shortUrls 短链接列表
     * @return 成功添加的数量
     */
    int offerBatch(List<String> shortUrls);

    /**
     * 获取池中当前数量
     *
     * @return 当前数量
     */
    int size();

    /**
     * 检查池是否为空
     *
     * @return 是否为空
     */
    boolean isEmpty();

    /**
     * 清空池
     */
    void clear();

    /**
     * 获取池的统计信息
     *
     * @return 统计信息
     */
    String getStats();
}