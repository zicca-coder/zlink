package com.zicca.zlink.backend.service;

/**
 * 短链接生成服务接口
 */
public interface ShortUrlGeneratorService {

    /**
     * 生成唯一的短链接后缀
     *
     * @param originalUrl 原始URL
     * @param gid 分组ID
     * @return 短链接后缀
     */
    String generateUniqueShortUrl(String originalUrl, String gid);

    /**
     * 检查短链接是否已存在
     *
     * @param shortUrl 短链接
     * @return 是否存在
     */
    boolean isShortUrlExists(String shortUrl);
}