package com.scorenow.scorenow_api.domain.match.service.sportdetail.synchronizer;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballDetail;
import com.scorenow.scorenow_api.domain.match.dto.sync.MatchDetailSyncSnapshot;
import com.scorenow.scorenow_api.domain.match.dto.sync.MatchDetailSyncStadium;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.redis.InplayRedisService;
import com.scorenow.scorenow_api.domain.match.repository.mongo.MatchLineupRepository;
import com.scorenow.scorenow_api.domain.match.service.MatchDetailService;
import com.scorenow.scorenow_api.domain.match.service.MatchLineupGoalsService;
import com.scorenow.scorenow_api.domain.match.service.sportdetail.normalizer.FootballDetailNormalizer;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsFootballViewResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FootballInplayMatchDetailSynchronizer implements InplayMatchDetailSynchronizer {
    private static final int BATCH_SIZE = 10;

    private final BetsApiClient betsApiClient;

    private final MatchLineupRepository matchLineupRepository;

    private final MatchDetailService matchDetailService;
    private final InplayRedisService inplayRedisService;
    private final MatchLineupGoalsService matchLineupGoalsService;

    private final FootballDetailNormalizer normalizer;

    @Override
    public SportCode type() {
        return SportCode.FOOTBALL;
    }

    @Override
    public void sync(SportCode sportCode, List<Match> inplayMatches) {
        // Bets View API 호출을 위하여 apiMatchId 를 추출한 리스트로 변환한다.
        List<String> apiMatchIds = inplayMatches.stream()
                .map(Match::getApiMatchId)
                .toList();

        // Bets View API 는 1회 요청당 최대 10개의 경기를 호출할 수 있기 때문에 해당 형식으로 변경한다.
        List<String> apiMatchIdBatches = createEventIdBatches(apiMatchIds);

        // Bets View API 를 호출
        List<BetsFootballViewResponse> responses = new ArrayList<>();
        for (String apiMatchIdBatch : apiMatchIdBatches) {
            try {
                log.info("📡[Football Event View API 호출] eventId : {}", apiMatchIdBatch);
                responses.add(betsApiClient.getFootballEventView(apiMatchIdBatch));
            } catch (Exception e) {
                log.warn("❌[Football Event View API 호출 실패] eventId : {}", apiMatchIdBatch, e);
            }
        }

        // 경기별 상세정보 형태로 변환한다. (Map<ApiMatchId, BetsFootballViewResponse.ViewResult>)
        Map<String, BetsFootballViewResponse.ViewResult> detailsByApiMatchId = responses.stream()
                .filter(footballViewResponse -> footballViewResponse != null && footballViewResponse.hasResults())
                .flatMap(footballViewResponse -> footballViewResponse.getResults().stream())
                .filter(viewResult -> viewResult != null && viewResult.getId() != null)
                .collect(Collectors.toMap(
                        BetsFootballViewResponse.ViewResult::getId,
                        Function.identity(),
                        (before, after) -> before
                ));

        // 라인업 정보는 모든 경기에 대해 한 번만 조회한 뒤 경기별 처리에서 재사용한다.
        Map<Long, MatchLineupDocument> matchLineupDocuments = detailsByApiMatchId.values().stream()
                .anyMatch(BetsFootballViewResponse.ViewResult::hasLineup)
                ? findMatchLineupDocuments(inplayMatches)
                : Map.of();

        // 각 Inplay 경기의 세부 정보 및 라인업-골 정보를 업데이트 한다. (축구의 경우 라인업-골 정보도 업데이트 필요함)
        for (Match inplayMatch : inplayMatches) {
            Long matchId = inplayMatch.getId();
            Long sportId = inplayMatch.getSportId();
            String apiMatchId = inplayMatch.getApiMatchId();
            BetsFootballViewResponse.ViewResult footballViewResult = detailsByApiMatchId.get(apiMatchId);

            if (footballViewResult == null) {
                log.info("축구 경기 [matchId:{}, apiMatchId:{}] 에 대한 View API 세부 정보가 존재하지 않습니다. ", matchId, apiMatchId);
                continue;
            }

            try {
                MatchDetailSyncStadium stadium = resolveStadium(footballViewResult.getExtra());
                FootballDetail normalizedFootballDetail = normalizer.normalize(footballViewResult);

                MatchDetailSyncSnapshot snapshot = new MatchDetailSyncSnapshot(
                        DataOrigin.BETS,
                        sportCode,
                        matchId,
                        footballViewResult.getSportId(),
                        footballViewResult.getHomeScore(),
                        footballViewResult.getAwayScore(),
                        MatchStatus.fromCode(footballViewResult.getTimeStatus()),
                        stadium,
                        normalizedFootballDetail
                );

                // 경기 세부 정보 업데이트
                matchDetailService.updateInplayMatchDetail(snapshot);

                // 라인업 정보가 제공 되는 경우 라인업-골 정보 업데이트
                if (footballViewResult.hasLineup()) {
                    // 라인업이 없는 경우, MatchLineupSyncScheduler 가 처리할 수 있게 레디스 큐에 삽입해준다.
                    if (!matchLineupDocuments.containsKey(matchId)) {
                        boolean isEnqueued = inplayRedisService.markSeenAndEnqueue(String.valueOf(matchId), String.valueOf(sportId));
                        log.info("LineupQueue 에 삽입 {} - matchId:{}", isEnqueued ? "성공" : "실패", matchId);
                        continue;
                    }

                    // 라인업-골 정보 업데이트
                    MatchLineupDocument matchLineupDocument = matchLineupDocuments.get(matchId);
                    matchLineupGoalsService.updateLineupGoals(matchLineupDocument, footballViewResult);
                }
            } catch (Exception e) {
                log.error("❌ INPLAY 경기 세부 정보 업데이트 처리 중 실패. sportCode:{}, matchId:{}, apiMatchId:{}",
                        sportCode,
                        matchId,
                        apiMatchId,
                        e
                );
            }
        }

    }

    /**
     * Inplay 인 경기 정보를 기반으로 MatchLineupDocument 를 조회한다. (라인업-골 정보 업데이트를 위하여 조회)
     */
    private Map<Long, MatchLineupDocument> findMatchLineupDocuments(List<Match> inplayMatches) {
        List<Long> matchIds = inplayMatches.stream().map(Match::getId).toList();

        return matchLineupRepository.findByIdIn(matchIds).stream()
                .collect(Collectors.toMap(
                        MatchLineupDocument::getId,
                        matchLineupDocument -> matchLineupDocument,
                        (existing, replacement) -> existing));
    }

    /**
     * 본 메서드의 로직은 아래와 같다.
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

    private MatchDetailSyncStadium resolveStadium(BetsFootballViewResponse.Extra extra) {
        if (extra == null || extra.getStadiumData() == null) {
            return null;
        }

        BetsFootballViewResponse.StadiumData stadiumData = extra.getStadiumData();
        return new MatchDetailSyncStadium(stadiumData.getId(), stadiumData.getName(), stadiumData.getCity());
    }
}
