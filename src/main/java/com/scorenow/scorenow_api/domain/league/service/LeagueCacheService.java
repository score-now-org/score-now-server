package com.scorenow.scorenow_api.domain.league.service;

import java.time.Duration;
import java.util.Optional;

import com.scorenow.scorenow_api.domain.league.entity.LeagueExternalMapping;
import com.scorenow.scorenow_api.domain.league.repository.LeagueExternalMappingRepository;
import com.scorenow.scorenow_api.external.common.ApiProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.repository.LeagueRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeagueCacheService {

    private static final String CACHE_PREFIX = "league:";
    private static final String DELIMITER = ":";

    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, Object> redisTemplate;
    private final LeagueRepository leagueRepository;
    private final LeagueExternalMappingRepository leagueExternalMappingRepository;

    /**
     * 리그 조회
     * 1. Redis에서 먼저 조회
     * 2. 없으면 DB 조회 후 Redis에 저장
     */
    public Optional<League> get(ApiProvider provider, String apiLeagueId) {
        String cacheKey = generateCacheKey(provider, apiLeagueId);

        // 1. 캐시 조회
        League cachedLeague = (League) redisTemplate.opsForValue().get(cacheKey);
        if (cachedLeague != null) {
            log.debug("CACHE HIT - League: {}", apiLeagueId);
            return Optional.of(cachedLeague);
        }

        log.debug("Cache MISS - League: {}", apiLeagueId);

        // 2. DB 조회 (Mapping Table)
        Optional<LeagueExternalMapping> leagueMappingInfo = leagueExternalMappingRepository.findByExternalInfo(provider, apiLeagueId);
        if (leagueMappingInfo.isPresent()) {
            Long internalLeagueId = leagueMappingInfo.get().getInternalLeagueId();

            // 3. DB 조회 (League Table)
            Optional<League> league = leagueRepository.findById(internalLeagueId);

            // 4. 캐시 저장
            if (league.isPresent()) {
                redisTemplate.opsForValue().set(cacheKey, league, CACHE_TTL);
                log.debug("Cache SET - League: {}", apiLeagueId);
            }

            return league;
        }

        return Optional.empty();
    }

    /**
     * 리그 캐시 삭제
     */
    public void delete(ApiProvider provider, String apiLeagueId) {
        String cacheKey = generateCacheKey(provider, apiLeagueId);
        redisTemplate.delete(cacheKey);
        log.info("Cache DELETE - League: {}", apiLeagueId);
    }

    /**
     * 전체 리그 캐시 초기화
     */
    public void invalidateAll() {
        String pattern = CACHE_PREFIX + "*";
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Cache INVALIDATE ALL - League count: {}", keys.size());
        }
    }

    /**
     * 리그 캐시 강제 갱신
     * DB 조회 후 캐시 업데이트
     */
    public Optional<League> refresh(ApiProvider provider, String apiLeagueId) {
        delete(provider, apiLeagueId);
        return get(provider, apiLeagueId);
    }

    /**
     * 리그 Cache Key 생성
     */
    private String generateCacheKey(ApiProvider provider, String apiLeagueId) {
        return CACHE_PREFIX + provider + DELIMITER + apiLeagueId;
    }
}
