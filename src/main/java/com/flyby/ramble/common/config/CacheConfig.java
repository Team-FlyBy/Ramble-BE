package com.flyby.ramble.common.config;

import com.flyby.ramble.common.model.CacheType;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        List<CaffeineCache> caches = Arrays.stream(CacheType.values())
                .map(this::buildCaffeineCache)
                .toList();

        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(caches);
        return cacheManager;
    }

    private CaffeineCache buildCaffeineCache(CacheType type) {
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .recordStats()
                .expireAfterWrite(Duration.ofMinutes(type.getExpireAfterWrite()))
                .maximumSize(type.getMaximumSize());

        return new CaffeineCache(type.getCacheName(), caffeine.build());
    }

}
