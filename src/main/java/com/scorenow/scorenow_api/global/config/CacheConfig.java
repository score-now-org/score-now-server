package com.scorenow.scorenow_api.global.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;
import com.scorenow.scorenow_api.domain.stadium.entity.StadiumCacheKey;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public Cache<StadiumCacheKey, Stadium> stadiumCache() {
        return Caffeine.newBuilder()
                .maximumSize(5_000)
                .build();
    }
}
