package com.scorenow.scorenow_api.domain.league.service;

import java.time.Duration;
import java.util.Optional;

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
	private static final Duration CACHE_TTL = Duration.ofMinutes(30);
	private final RedisTemplate<String, Object> redisTemplate;
	private final LeagueRepository leaguerepository;

	/**
	 * 리그 조회
	 * 1. Redis에서 먼저 조회
	 * 2. 없으면 DB 조회 후 Redis에 저장
	 */
	public Optional<League> get(String leagueId){
		String cacheKey = CACHE_PREFIX + leagueId;

		// 1. 캐시 조회
		League cachedLeague = (League) redisTemplate.opsForValue().get(cacheKey);
		if(cachedLeague != null){
			log.debug("CACHE HIT - League: {}", leagueId);
			return Optional.of(cachedLeague);
		}

		// 2. DB 조회
		log.debug("Cache MISS - League: {}", leagueId);
		Optional<League> league = leaguerepository.findById(leagueId);

		// 3. 캐시 저장
		league.ifPresent(l -> {
			redisTemplate.opsForValue().set(cacheKey, l, CACHE_TTL);
			log.debug("Cache SET - League: {}", leagueId);
		});

		return league;
	}

	/**
	 * 리그 캐시 삭제
	 */
	public void delete(String leagueId){
		String cacheKey = CACHE_PREFIX + leagueId;
		redisTemplate.delete(cacheKey);
		log.info("Cache DELETE - League: {}", leagueId);
	}

	/**
	 * 전체 리그 캐시 초기화
	 */
	public void invalidateAll(){
		String pattern = CACHE_PREFIX + "*";
		var keys = redisTemplate.keys(pattern);
		if(keys != null && !keys.isEmpty()){
			redisTemplate.delete(keys);
			log.info("Cache INVALIDATE ALL - League count: {}", keys.size());
		}
	}

	/**
	 * 리그 캐시 강제 갱신
	 * DB 조회 후 캐시 업데이트
	 */
	public Optional<League> refresh(String leagueId){
		delete(leagueId);
		return get(leagueId);
	}
}
