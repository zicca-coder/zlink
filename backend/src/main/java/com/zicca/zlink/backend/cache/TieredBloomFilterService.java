package com.zicca.zlink.backend.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j(topic = "TieredBloomFilterService")
@Service
@RequiredArgsConstructor
public class TieredBloomFilterService {

    private final LocalBloomFilterService localBloomFilterService;
    private final RedisBloomFilterService redisBloomFilterService;


    public boolean mightContain(String key) {
        if (!localBloomFilterService.mightContains(key)) {
            return false;
        }
        return redisBloomFilterService.mightContains(key);
    }

    public void add(String key) {
        localBloomFilterService.add(key);
        redisBloomFilterService.add(key);
    }

}
