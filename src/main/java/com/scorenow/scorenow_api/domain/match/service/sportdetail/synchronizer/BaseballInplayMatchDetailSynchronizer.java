package com.scorenow.scorenow_api.domain.match.service.sportdetail.synchronizer;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballDetail;
import com.scorenow.scorenow_api.domain.match.dto.sync.MatchDetailSyncSnapshot;
import com.scorenow.scorenow_api.domain.match.dto.sync.MatchDetailSyncStadium;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.service.MatchDetailService;
import com.scorenow.scorenow_api.domain.match.service.sportdetail.normalizer.BaseballDetailNormalizer;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsBaseballViewResponse;
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
public class BaseballInplayMatchDetailSynchronizer implements InplayMatchDetailSynchronizer {
    private static final int BATCH_SIZE = 10;

    private final BetsApiClient betsApiClient;

    private final MatchDetailService matchDetailService;

    private final BaseballDetailNormalizer normalizer;

    @Override
    public SportCode type() {
        return SportCode.BASEBALL;
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
        List<BetsBaseballViewResponse> responses = new ArrayList<>();
        for (String apiMatchIdBatch : apiMatchIdBatches) {
            try {
                log.info("📡[Baseball Event View API 호출] eventId : {}", apiMatchIdBatch);
                responses.add(betsApiClient.getBaseballEventView(apiMatchIdBatch));
            } catch (Exception e) {
                log.warn("❌[Baseball Event View API 호출 실패] eventId : {}", apiMatchIdBatch, e);
            }
        }

        // 경기별 상세정보 형태로 변환한다. (Map<ApiMatchId, BetsBaseballViewResponse.ViewResult>)
        Map<String, BetsBaseballViewResponse.ViewResult> detailsByApiMatchId = responses.stream()
                .filter(response -> response != null && response.hasResults())
                .flatMap(response -> response.getResults().stream())
                .filter(viewResult -> viewResult != null && viewResult.getId() != null)
                .collect(Collectors.toMap(
                        BetsBaseballViewResponse.ViewResult::getId,
                        Function.identity(),
                        (before, after) -> before
                ));

        // 각 Inplay 경기의 세부 정보를 업데이트 한다.
        for (Match inplayMatch : inplayMatches) {
            Long matchId = inplayMatch.getId();
            String apiMatchId = inplayMatch.getApiMatchId();
            BetsBaseballViewResponse.ViewResult baseballViewResult = detailsByApiMatchId.get(apiMatchId);

            if (baseballViewResult == null) {
                log.info("야구 경기 [matchId:{}, apiMatchId:{}] 에 대한 View API 세부 정보가 존재하지 않습니다. ", matchId, apiMatchId);
                continue;
            }

            try {
                MatchDetailSyncStadium stadium = resolveStadium(baseballViewResult.getExtra());
                BaseballDetail normalizedBaseballDetail = normalizer.normalize(baseballViewResult);

                MatchDetailSyncSnapshot snapshot = new MatchDetailSyncSnapshot(
                        DataOrigin.BETS,
                        sportCode,
                        matchId,
                        baseballViewResult.getSportId(),
                        baseballViewResult.getHomeScore(),
                        baseballViewResult.getAwayScore(),
                        MatchStatus.fromCode(baseballViewResult.getTimeStatus()),
                        stadium,
                        normalizedBaseballDetail
                );

                matchDetailService.updateInplayMatchDetail(snapshot);
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

    private MatchDetailSyncStadium resolveStadium(BetsBaseballViewResponse.Extra extra) {
        if (extra == null || extra.getStadiumData() == null) {
            return null;
        }

        BetsBaseballViewResponse.StadiumData stadiumData = extra.getStadiumData();
        return new MatchDetailSyncStadium(stadiumData.getId(), stadiumData.getName(), stadiumData.getCity());
    }
}
