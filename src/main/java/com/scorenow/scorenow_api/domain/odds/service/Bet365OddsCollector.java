package com.scorenow.scorenow_api.domain.odds.service;

import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import com.scorenow.scorenow_api.domain.odds.entity.MatchOddsHistory;
import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsEventOddsResponse;
import com.scorenow.scorenow_api.external.betsapi.normalizer.BetsEventOddsNormalizer;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class Bet365OddsCollector {

    private final MatchRepository matchRepository;
    private final Bet365OddsPersistenceService persistenceService;
    private final BetsApiClient betsApiClient;
    private final BetsEventOddsNormalizer normalizer;

    /**
     * 경기의 배당률을 조회하고 새로운 배당률 데이터를 저장한다.
     * 외부 API 호출은 트랜잭션이 적용되는 persistenceService 호출 사이에서 수행된다.
     */
    public int collect(Long matchId) {
        Match match = findMatchWithSport(matchId);
        String apiMatchId = requireApiMatchId(match);
        SportCode sportCode = requireSportCode(match);

        Instant sinceTime = persistenceService.resolveSinceTime(matchId);
        BetsEventOddsResponse response = betsApiClient.getBet365EventOdds(apiMatchId, sinceTime);
        List<MatchOddsHistory> histories = normalizer.normalize(matchId, sportCode, response);

        int savedCount = persistenceService.saveNewHistories(matchId, histories);
        log.info(
                "🟢Bet365 배당률 수집 완료. matchId:{}, apiMatchId:{}, sinceTime:{}, normalizedCount:{}, savedCount:{}",
                matchId,
                apiMatchId,
                sinceTime,
                histories.size(),
                savedCount
        );

        return savedCount;
    }

    private Match findMatchWithSport(Long matchId) {
        return matchRepository.findByIdWithSport(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));
    }

    private String requireApiMatchId(Match match) {
        String apiMatchId = match.getApiMatchId();
        if (apiMatchId == null || apiMatchId.isBlank()) {
            throw new IllegalArgumentException("BetsAPI 경기 ID가 없는 경기는 배당률을 수집할 수 없습니다.");
        }
        return apiMatchId.trim();
    }

    private SportCode requireSportCode(Match match) {
        Sport sport = match.getSport();
        if (sport == null || sport.getSportCode() == null) {
            throw new IllegalArgumentException("경기 종목 정보가 없어 배당률을 수집할 수 없습니다.");
        }
        return sport.getSportCode();
    }
}
