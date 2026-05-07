package com.scorenow.scorenow_api.domain.stadium.service;

import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;
import com.scorenow.scorenow_api.domain.stadium.entity.StadiumCacheKey;
import com.scorenow.scorenow_api.external.common.ApiProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StadiumCacheService {

	private final Cache<StadiumCacheKey, Stadium> stadiumCache;
	private final StadiumService stadiumService;

	/**
	 * 경기장 생성 (캐시 + DB 활용)
	 */
	public Stadium getOrCreateStadium(final ApiProvider provider, final String externalSportId,
		final String externalStadiumId, final String name, final String city) {
		StadiumCacheKey cacheKey = new StadiumCacheKey(provider, externalSportId, externalStadiumId);

		return stadiumCache.get(cacheKey, key -> {
				log.info("[Cache Miss ❌] provider={}, externalSportId={}, externalStadiumId={}", provider, externalSportId,
					externalStadiumId);
				return stadiumService.getOrCreateStadium(provider, externalSportId, externalStadiumId, name, city);
			}
		);
	}
}
