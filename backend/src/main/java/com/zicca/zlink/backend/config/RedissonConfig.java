package com.zicca.zlink.backend.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private String redisPort;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + redisHost + ":" + redisPort;
        config.useSingleServer() // 单机模式
                .setAddress(address) // Redis地址
                .setConnectionMinimumIdleSize(10) // 最小空闲连接数，表示始终保留10个空闲连接以提升性能
                .setConnectionPoolSize(20) // 连接池大小，连接池最大连接数
                .setIdleConnectionTimeout(10000) // 空闲连接超时时间，单位毫秒。空闲连接最大先知时间，超过这个时间后，空闲连接关闭
                .setConnectTimeout(10000) // 连接超时时间，建立新连接的最大等待时间
                .setTimeout(3000) // 命令等待超时时间，单个redis操作的最大等待时间
                .setRetryAttempts(3) // 失败重试次数
                .setRetryInterval(1500); // 重试间隔时间

        return Redisson.create(config);
    }

}
