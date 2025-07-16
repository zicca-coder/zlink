package com.zicca.zlink.backend.cache;

import java.util.Collection;

public interface BloomFilterService {

    /**
     * 初始化布隆过滤器
     */
    public void init();

    /**
     * 判断布隆过滤器是否包含指定的key
     *
     * @param key 键
     * @return 是否包含
     */
    public boolean mightContains(String key);

    /**
     * 向布隆过滤器中添加一个键
     *
     * @param key 键
     */
    public void add(String key);

    /**
     * 获取布隆过滤器的统计信息
     *
     * @return 统计信息字符串
     */
    public String getStats();

}
