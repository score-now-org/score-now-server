package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.dto.MatchCandidate;
import com.scorenow.scorenow_api.domain.match.repository.redis.InplayMatchRedisRepository;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsEventResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.scorenow.scorenow_api.domain.match.constant.MatchConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InplayMatchSyncService {

    private final BetsApiClient betsApiClient;

    private final InplayCandidateStatusUpdateService inplayCandidateStatusUpdateService;

    private final InplayMatchRedisRepository inplayMatchRedisRepository;

    public void syncInplayMatches(ZonedDateTime standardTime) {
        ZonedDateTime now = standardTime.withZoneSameInstant(SEOUL_TIME_ZONE_ID);

        // 1. 동기화 대상 조회 (현재 시간 기준 INPLAY 일 것으로 예상되는 경기들)
        List<MatchCandidate> candidates = inplayMatchRedisRepository.findCandidatesToSync(now);

        // 2. 외부 API 중복 호출 방지를 위해 Sport ID만 추출 (종목별 INPLAY API 호출 필요)
        List<String> apiSportIds = candidates.stream()
                .map(MatchCandidate::getApiSportId)
                .distinct()
                .toList();

        // 3. 종목별 INPLAY API 호출 및 실제 진행 중인 경기 ID 취합
        Map<String, BetsEventResponse.Event> inplayEvents = apiSportIds.stream()
                .map(sportId -> {
                    try {
                        return betsApiClient.getInplayEvents(sportId, null);
                    } catch (Exception e) {
                        log.error("[종목ID:{}] 종목별 INPLAY API 호출 실패.", sportId, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(response -> response.getResults() != null)
                .flatMap(response -> response.getResults().stream())
                .filter(event -> event.getId() != null)
                .collect(Collectors.toMap(
                        BetsEventResponse.Event::getId,
                        event -> event,
                        (first, second) -> first
                ));


        // 4. INPLAY API 응답과 대조하여 대상 분류 (진행 중, 유예기간 초과)
        // 참고) 유예기간 초과한 경기 : 현재 시간 기준 시작돼야 하는 경기이나, 최대 유예 기간만큼 대기 후에도 API 응답 기준 여전히 시작하지 못한 경기
        List<MatchCandidate> inplayMatches = new ArrayList<>();
        List<MatchCandidate> toBeFixedMatches = new ArrayList<>();

        for (MatchCandidate candidate : candidates) {
            if (inplayEvents.containsKey(candidate.getApiMatchId())) {
                inplayMatches.add(candidate);
                continue;
            }

            LocalDateTime startAt = candidate.getStartAt();
            if (startAt.plusMinutes(MAX_GRACE_PERIOD_MINUTES).isBefore(now.toLocalDateTime())) {
                log.warn("🟠유예 기간 초과. matchId:{}", candidate.getMatchId());
                toBeFixedMatches.add(candidate);
            }
        }

        // 5. 분류 결과를 기반으로 DB 반영 및 Redis 캐시 정리
        if (!inplayMatches.isEmpty()) {
            int updatedCount = inplayCandidateStatusUpdateService.updateInplayStatuses(inplayMatches);
            log.info("🟢IN_PLAY 상태 업데이트 완료. requested={}, updated={}", inplayMatches.size(), updatedCount);

            inplayMatchRedisRepository.removeCandidates(inplayMatches);
        }

        if (!toBeFixedMatches.isEmpty()) {
            int updatedCount = inplayCandidateStatusUpdateService.updateToBeFixedStatuses(toBeFixedMatches);
            log.info("🟢TO_BE_FIXED 상태 업데이트 완료. requested={}, updated={}", toBeFixedMatches.size(), updatedCount);

            inplayMatchRedisRepository.removeCandidates(toBeFixedMatches);
        }

    }

}
