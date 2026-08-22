package com.scorenow.scorenow_api.domain.stadium.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.scorenow.scorenow_api.domain.stadium.entity.StadiumCacheKey;
import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StadiumCacheService {

    private final Cache<StadiumCacheKey, Long> stadiumIdCache;
    private final StadiumService stadiumService;

    /**
     * 경기장 생성 (캐시 + DB 활용)
     */
    public Long getOrCreateStadiumId(final DataOrigin dataOrigin, final String apiSportId, final String apiStadiumId, final String name, final String city) {
        StadiumCacheKey cacheKey = new StadiumCacheKey(dataOrigin, apiSportId, apiStadiumId);

        return stadiumIdCache.get(cacheKey, key -> {
                    log.info("[Cache Miss ❌] dataOrigin={}, apiSportId={}, apiStadiumId={}", dataOrigin, apiSportId, apiStadiumId);
                    return stadiumService.getOrCreateStadium(dataOrigin, apiSportId, apiStadiumId, name, city).getId();
                }
        );
    }
}
