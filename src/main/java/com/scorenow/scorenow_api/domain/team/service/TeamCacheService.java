package com.scorenow.scorenow_api.domain.team.service;

import java.time.Duration;
import java.util.Optional;

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
	private static final Duration CACHE_TTL = Duration.ofMinutes(30);
	private final RedisTemplate<String, Object> redisTemplate;
	private final TeamRepository teamRepository;

	/**
	 * 팀 조회 (캐시 우선)
	 */
	public Optional<Team> get(String teamId) {
		String cacheKey = CACHE_PREFIX + teamId;

		// 1. 캐시 조회
		Team cachedTeam = (Team) redisTemplate.opsForValue().get(cacheKey);
		if (cachedTeam != null) {
			log.debug("Cache HIT - Team: {}", teamId);
			return Optional.of(cachedTeam);
		}

		// 2. DB 조회
		log.debug("Cache MISS - Team: {}", teamId);
		Optional<Team> team = teamRepository.findById(teamId);

		// 3. 캐시 저장
		team.ifPresent(t -> {
			redisTemplate.opsForValue().set(cacheKey, t, CACHE_TTL);
			log.debug("Cache SET - Team: {}", teamId);
		});

		return team;
	}

	/**
	 * 팀 캐시 삭제
	 */
	public void delete(String teamId) {
		String cacheKey = CACHE_PREFIX + teamId;
		redisTemplate.delete(cacheKey);
		log.info("Cache DELETE - Team: {}", teamId);
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
	public Optional<Team> refresh(String teamId) {
		delete(teamId);
		return get(teamId);
	}
}
