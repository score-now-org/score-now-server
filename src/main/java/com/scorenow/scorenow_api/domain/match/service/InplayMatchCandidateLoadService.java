package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.dto.MatchCandidate;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static com.scorenow.scorenow_api.domain.match.constant.MatchConstants.*;
import static com.scorenow.scorenow_api.domain.match.entity.MatchStatus.NOT_STARTED;

@Slf4j
@Service
@RequiredArgsConstructor
public class InplayMatchCandidateLoadService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final MatchRepository matchRepository;

    public void loadInplayCandidatesToCache() {
        ZonedDateTime now = LocalDateTime.now().atZone(ZoneId.of(SEOUL_TIME_ZONE));
        List<MatchCandidate> candidates = matchRepository.findMatchesStartingWithin(NOT_STARTED, now.toLocalDateTime(), now.toLocalDateTime().plusHours(INPLAY_CANDIDATES_SCAN_INTERVAL_HOURS));

        log.info("{}시간 이내 시작 예정 경기 건수 : {}", INPLAY_CANDIDATES_SCAN_INTERVAL_HOURS, candidates.size());

        for (MatchCandidate candidate : candidates) {
            // ZSet : { key:inplay_candidates / value:MatchCandidate / score:경기시작시간 }
            redisTemplate.opsForZSet().addIfAbsent(INPLAY_CANDIDATES_KEY, candidate, (double) now.toInstant().toEpochMilli());
        }
    }
}
