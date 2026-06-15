package com.scorenow.scorenow_api.domain.team.service;

import java.time.Duration;
import java.util.Optional;

import com.scorenow.scorenow_api.domain.team.entity.TeamExternalMapping;
import com.scorenow.scorenow_api.domain.team.repository.TeamExternalMappingRepository;
import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.domain.team.repository.TeamRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamCacheService {

    private static final String CACHE_PREFIX = "team:";
    private static final String DELIMITER = ":";

    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, Object> redisTemplate;
    private final TeamRepository teamRepository;
    private final TeamExternalMappingRepository teamExternalMappingRepository;

    /**
     * 팀 조회 (캐시 우선)
     */
    public Optional<Team> get(DataOrigin provider, String apiTeamId) {
        String cacheKey = generateCacheKey(provider, apiTeamId);

        // 1. 캐시 조회
        Team cachedTeam = (Team) redisTemplate.opsForValue().get(cacheKey);
        if (cachedTeam != null) {
            log.debug("Cache HIT - Team: {}", apiTeamId);
            return Optional.of(cachedTeam);
        }

        log.debug("Cache MISS - Team: {}", apiTeamId);

        // 2. DB 조회 (Mapping Table)
        Optional<TeamExternalMapping> teamMappingInfo = teamExternalMappingRepository.findByProviderAndApiTeamId(provider, apiTeamId);
        if (teamMappingInfo.isPresent()) {
            Long internalTeamId = teamMappingInfo.get().getInternalTeamId();

            // 3. DB 조회 (Team Table)
            Optional<Team> team = teamRepository.findById(internalTeamId);

            // 4. 캐시 저장
            if (team.isPresent()) {
                redisTemplate.opsForValue().set(cacheKey, team, CACHE_TTL);
                log.debug("Cache SET - Team: {}", apiTeamId);
            }

            return team;
        }

        return Optional.empty();
    }

    /**
     * 팀 캐시 삭제
     */
    public void delete(String apiTeamId) {
        String cacheKey = CACHE_PREFIX + apiTeamId;
        redisTemplate.delete(cacheKey);
        log.info("Cache DELETE - Team: {}", apiTeamId);
    }

    /**
     * 전체 팀 캐시 초기화
     */
    public void invalidateAll() {
        String pattern = CACHE_PREFIX + "*";
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Cache INVALIDATE ALL - Team count: {}", keys.size());
        }
    }

    /**
     * 팀 캐시 강제 갱신
     */
    public Optional<Team> refresh(DataOrigin provider, String apiTeamId) {
        delete(apiTeamId);
        return get(provider, apiTeamId);
    }

    /**
     * 팀 Cache Key 생성
     */
    private String generateCacheKey(DataOrigin provider, String apiTeamId) {
        return CACHE_PREFIX + provider + DELIMITER + apiTeamId;
    }
}
