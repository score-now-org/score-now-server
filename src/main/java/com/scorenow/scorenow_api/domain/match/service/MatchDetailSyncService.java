package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.redis.InplayRedisService;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.match.repository.mongo.MatchLineupRepository;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsViewResponse;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsViewResponse.ViewResult;
import com.scorenow.scorenow_api.external.common.ApiProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.scorenow.scorenow_api.domain.match.entity.MatchStatus.IN_PLAY;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchDetailSyncService {

    private static final int BATCH_SIZE = 10;

    private final BetsApiClient betsApiClient;

    private final MatchRepository matchRepository;
    private final MatchLineupRepository matchLineupRepository;

    private final MatchDetailService matchDetailService;
    private final InplayRedisService inplayRedisService;
    private final MatchLineupGoalsService matchLineupGoalsService;

    public void syncInplayMatchDetails() {
        // 1. Inplay 상태인 경기들을 조회한다. (단, 수동 경기의 경우 제외)
        List<Match> inplayMatches = matchRepository.findByStatusCodeAndIsManualFalse(IN_PLAY);

        if (inplayMatches.isEmpty()) {
            log.info("현재 INPLAY 상태의 경기가 없습니다.");
            return;
        }

        // 2. API 호출을 위한 apiMatchId 로 변환한다.
        List<String> apiMatchIds = inplayMatches.stream()
                .map(Match::getApiMatchId)
                .toList();

        // 3. Event View API 는 한번에 최대 10개의 경기까지 호출할 수 있다. 이를 위한 형식으로 변경한다.
        List<String> eventIdBatches = createEventIdBatches(apiMatchIds);

        // 4. Event View API 를 호출하고, 응답값을 Map<apiMatchId, MatchDetail> 형식으로 변경한다.
        List<BetsViewResponse> betsViewResponses = new ArrayList<>();
        for (String eventIdBatch : eventIdBatches) {
            try {
                log.info("📡[Event View API 호출] eventId : {}", eventIdBatch);
                betsViewResponses.add(betsApiClient.getEventView(eventIdBatch));
            } catch (Exception e) {
                log.warn("❌[Event View API 호출 실패] eventId : {}", eventIdBatch, e);
            }
        }

        Map<String, ViewResult> matchDetails = betsViewResponses.stream()
                .filter(viewResponse -> viewResponse != null && viewResponse.hasResults())
                .flatMap(viewResponse -> viewResponse.getResults().stream())
                .filter(viewResult -> viewResult != null && viewResult.getId() != null)
                .collect(Collectors.toMap(
                        ViewResult::getId,
                        viewResult -> viewResult,
                        (existing, replacement) -> existing));

        // 5. Inplay 인 경기 정보를 기반으로 MatchLineupDocument 를 조회한다. (라인업-골 정보 업데이트를 위하여 조회)
        List<Long> matchIds = inplayMatches.stream().map(Match::getId).toList();

        Map<Long, MatchLineupDocument> matchLineupDocuments = matchLineupRepository.findByIdIn(matchIds).stream()
                .collect(Collectors.toMap(
                        MatchLineupDocument::getId,
                        matchLineupDocument -> matchLineupDocument,
                        (existing, replacement) -> existing));

        // 6. 각 Inplay 경기의 세부 정보 및 라인업-골 정보를 업데이트 한다.
        for (Match inplayMatch : inplayMatches) {
            Long matchId = inplayMatch.getId();
            Long sportId = inplayMatch.getSportId();
            String apiMatchId = inplayMatch.getApiMatchId();

            ViewResult matchDetail = matchDetails.get(apiMatchId);
            if (matchDetail == null) {
                log.info("경기에 대한 세부 정보가 존재하지 않습니다 - matchId:{}, apiMatchId:{}", matchId, apiMatchId);
                continue;
            }

            try {
                matchDetailService.updateInplayMatchDetail(ApiProvider.BETS, matchDetail);  // 경기 세부 정보 업데이트

                if (matchDetail.hasLineup()) {
                    // Lineup 이 없는 경우, MatchLineupSyncScheduler 가 처리할 수 있게 레디스 큐에 삽입해준다.
                    if (!matchLineupDocuments.containsKey(matchId)) {
                        boolean isEnqueued = inplayRedisService.markSeenAndEnqueue(String.valueOf(matchId), String.valueOf(sportId));
                        log.info("LineupQueue 에 삽입 {} - matchId:{}", isEnqueued ? "성공" : "실패", matchId);
                        continue;
                    }

                    // 라인업-골 정보 업데이트
                    MatchLineupDocument matchLineupDocument = matchLineupDocuments.get(matchId);
                    matchLineupGoalsService.updateLineupGoals(matchLineupDocument, matchDetail);
                }
            } catch (Exception e) {
                log.error("❌ INPLAY 경기 세부 정보 업데이트 처리 중 실패 - matchId: {}", inplayMatch.getId(), e);
            }
        }
    }

    /**
     * 로직은 아래와 같다.
     * apiMatchIds : ["100","101","102","103","104","105","106","107","108","109","110"]
     * eventIdBatches :["100,101,102,103,104,105,106,107,108,109","110"]
     */
    private List<String> createEventIdBatches(List<String> ids) {
        List<String> batches = new ArrayList<>();

        for (int from = 0; from < ids.size(); from += BATCH_SIZE) {
            int to = Math.min(from + BATCH_SIZE, ids.size());

            List<String> batch = ids.subList(from, to);
            batches.add(String.join(",", batch));
        }

        return batches;
    }
}
