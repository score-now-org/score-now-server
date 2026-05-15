package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.dto.MatchCandidate;
import com.scorenow.scorenow_api.domain.match.repository.redis.InplayMatchRedisRepository;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsEventResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final BetsApiClient betsApiClient;
    private final InplayMatchStatusUpdater inplayMatchStatusUpdater;
    private final InplayMatchRedisRepository inplayMatchRedisRepository;

    public void syncInplayMatches() {
        ZonedDateTime now = LocalDateTime.now().atZone(ZoneId.of(SEOUL_TIME_ZONE));

        // 1. 동기화 대상 조회 (현재 시간 기준 INPLAY 일 것으로 예상되는 경기들)
        List<MatchCandidate> candidates = inplayMatchRedisRepository.findCandidatesToSync(now);

        // 2. 외부 API 중복 호출 방지를 위해 Sport ID만 추출 (종목별 INPLAY API 호출 필요)
        List<String> apiSportIds = candidates.stream()
                .map(MatchCandidate::getApiSportId)
                .distinct()
                .toList();

        // 3. 종목별 INPLAY API 호출 및 실제 진행 중인 경기 ID 취합
        Set<String> inplayMatchIds = apiSportIds.stream()
                .map(sportId -> betsApiClient.getInplayEvents(sportId, null))
                .flatMap(response -> response.getResults().stream())
                .map(BetsEventResponse.Event::getId)
                .collect(Collectors.toSet());


        // 4. INPLAY API 응답과 대조하여 대상 분류 (진행 중, 유예기간 초과)
        // 참고) 유예기간 초과한 경기 : 현재 시간 기준 시작돼야 하는 경기이나, 최대 유예 기간만큼 대기 후에도 API 응답 기준 여전히 시작하지 못한 경기
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

        // 5. 분류 결과를 기반으로 DB 반영 및 Redis 캐시 정리
        if (!inplayMatches.isEmpty() || !toBeFixedMatches.isEmpty()) {
            inplayMatchStatusUpdater.updateMatchStatuses(inplayMatches, toBeFixedMatches);

            inplayMatchRedisRepository.removeCandidates(inplayMatches);
            inplayMatchRedisRepository.removeCandidates(toBeFixedMatches);
        }
    }

}
