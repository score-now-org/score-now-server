package com.scorenow.scorenow_api.domain.match.repository.redis;

import com.scorenow.scorenow_api.domain.match.dto.MatchCandidate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.scorenow.scorenow_api.domain.match.constant.MatchConstants.INPLAY_CANDIDATES_KEY;

@Slf4j
@Repository
@RequiredArgsConstructor
public class InplayMatchRedisRepository {

    // { key:inplay_candidates / value:MatchCandidate / score:경기시작시간 }
    private final RedisTemplate<String, Object> redisTemplate;

    public void addCandidate(MatchCandidate candidate, ZonedDateTime startAt) {
        double score = (double) startAt.toInstant().toEpochMilli();
        redisTemplate.opsForZSet().addIfAbsent(INPLAY_CANDIDATES_KEY, candidate, score);
    }

    public List<MatchCandidate> findCandidatesToSync(ZonedDateTime now) {
        Set<Object> matchCandidates = redisTemplate.opsForZSet()
                .rangeByScore(INPLAY_CANDIDATES_KEY, 0, (double) now.plusSeconds(1).toInstant().toEpochMilli());

        if (matchCandidates == null || matchCandidates.isEmpty()) {
            log.info("{} 기준 시작 여부 검증 대상 경기가 없습니다. ❌", now);
            return Collections.emptyList();
        }

        return matchCandidates.stream()
                .map(obj -> (MatchCandidate) obj)
                .toList();
    }

    public void removeCandidates(List<MatchCandidate> targets) {
        redisTemplate.opsForZSet().remove(INPLAY_CANDIDATES_KEY, targets.toArray());
    }
}
