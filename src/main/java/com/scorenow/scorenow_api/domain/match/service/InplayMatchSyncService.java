package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.dto.MatchCandidate;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsEventResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.scorenow.scorenow_api.domain.match.constant.MatchConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InplayMatchSyncService {

    private final RedisTemplate<String, Object> redisTemplate;

    private final BetsApiClient betsApiClient;

    private final InplayMatchStatusUpdater inplayMatchStatusUpdater;

    public void syncInplayMatches() {
        ZonedDateTime now = LocalDateTime.now().atZone(ZoneId.of(SEOUL_TIME_ZONE));

        // 1. 현재 INPLAY 일 것으로 예상되는 경기들을 조회
        Set<Object> matchCandidates = redisTemplate.opsForZSet()
                .rangeByScore(INPLAY_CANDIDATES_KEY, 0, (double) now.plusSeconds(1).toInstant().toEpochMilli());

        if (matchCandidates == null || matchCandidates.isEmpty()) {
            log.info("{} 기준 시작 여부 검증 대상 경기가 없습니다. ❌", now);
            return;
        }

        List<MatchCandidate> candidates = matchCandidates.stream()
                .map(obj -> (MatchCandidate) obj)
                .toList();

        // 2. 외부 sportId 값만 추출 (종목이 여러 개면 각각 INPLAY API 를 호출해야 하기 때문)
        List<String> apiSportIds = candidates.stream()
                .map(MatchCandidate::getApiSportId)
                .distinct()
                .toList();

        // 2. 종목 별 INPLAY API 요청 후, 응답 받은 Match ID 만 취합
        Set<String> inplayMatchIds = apiSportIds.stream()
                .map(sportId -> betsApiClient.getInplayEvents(sportId, null))
                .flatMap(response -> response.getResults().stream())
                .map(BetsEventResponse.Event::getId)
                .collect(Collectors.toSet());

        /**
         * 3. INPLAY API 응답과 비교하며, 실제로 경기가 진행된 경기와 경기가 유예기간이 초과한 경기를 분류
         *    참고) 유예기간이 초과한 경기:
         *          현재 시간 기준 시작되어야 하는 경기이나 MAX_GRACE_PERIOD 만큼 대기 후에도 API 응답 상으로 여전히 시작하지 못한 경기
         */
        List<MatchCandidate> inplayMatches = new ArrayList<>();
        List<MatchCandidate> toBeFixedMatches = new ArrayList<>();

        for (MatchCandidate candidate : candidates) {
            if (inplayMatchIds.contains(candidate.getApiMatchId())) {
                inplayMatches.add(candidate);
                continue;
            }

            LocalDateTime startAt = candidate.getStartAt();
            if (startAt.plusMinutes(MAX_GRACE_PERIOD_MINUTES).isBefore(now.toLocalDateTime())) {
                log.warn("유예 기간 초과. matchId:{}", candidate.getMatchId());
                toBeFixedMatches.add(candidate);
            }
        }

        // 4. 분류 결과를 기반으로 Redis 와 DB 에 반영
        if (!inplayMatches.isEmpty() || !toBeFixedMatches.isEmpty()) {
            inplayMatchStatusUpdater.updateMatchStatuses(inplayMatches, toBeFixedMatches);
            removeFromZSet(inplayMatches);
            removeFromZSet(toBeFixedMatches);
        }
    }

    private void removeFromZSet(List<MatchCandidate> targets) {
        redisTemplate.opsForZSet().remove(INPLAY_CANDIDATES_KEY, targets.toArray());
    }
}
