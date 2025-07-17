package com.zicca.zlink.backend.cache.holder;

import com.zicca.zlink.backend.cache.service.LocalBloomFilterService;
import com.zicca.zlink.backend.cache.service.RedisBloomFilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j(topic = "BloomFilterHolder")
@RequiredArgsConstructor
public class BloomFilterHolder {

    private final RedisBloomFilterService redisBloomFilterService;
    private final LocalBloomFilterService localBloomFilterService;


    public boolean mightContains(String key) {
        if (!localBloomFilterService.mightContains(key)) {
            return false;
        }
        return redisBloomFilterService.mightContains(key);
    }

    public boolean mightContainsInLocal(String key) {
        return localBloomFilterService.mightContains(key);
    }

    public boolean mightContainsInRedis(String key) {
        return redisBloomFilterService.mightContains(key);
    }

    public void add(String key) {
        localBloomFilterService.add(key);
        redisBloomFilterService.add(key);
    }


}
