package com.scorenow.scorenow_api.domain.odds.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;

import static com.scorenow.scorenow_api.domain.match.constant.MatchConstants.SEOUL_TIME_ZONE_ID;

@Slf4j
@Service
@RequiredArgsConstructor
public class Bet365OddsCollectionService {

    private static final int NEAR_START_HOURS = 1;
    private static final int COLLECTION_LOOKAHEAD_HOURS = 48;

    private final PrematchOddsCollectionTargetReader targetReader;
    private final Bet365OddsCollector collector;

    /**
     * 경기 시작까지 1시간 초과 48시간 이하로 남은 경기를 수집한다.
     */
    public void collectDistantPrematchOdds(ZonedDateTime standardTime) {
        ZonedDateTime now = toSeoulTime(standardTime);
        List<Long> matchIds = targetReader.findTargets(
                now.plusHours(NEAR_START_HOURS).toLocalDateTime(),
                now.plusHours(COLLECTION_LOOKAHEAD_HOURS).toLocalDateTime()
        );

        collectMatches("1시간 초과~48시간 이하", matchIds);
    }

    /**
     * 경기 시작 전부터 1시간 이하로 남은 경기를 수집한다.
     */
    public void collectNearStartPrematchOdds(ZonedDateTime standardTime) {
        ZonedDateTime now = toSeoulTime(standardTime);
        List<Long> matchIds = targetReader.findTargets(
                now.toLocalDateTime(),
                now.plusHours(NEAR_START_HOURS).toLocalDateTime()
        );

        collectMatches("1시간 이하", matchIds);
    }

    private void collectMatches(String targetRange, List<Long> matchIds) {
        int succeededCount = 0;
        int failedCount = 0;
        int savedCount = 0;

        for (Long matchId : matchIds) {
            try {
                savedCount += collector.collect(matchId);
                succeededCount++;
            } catch (Exception exception) {
                failedCount++;
                log.error("🔴Bet365 배당률 수집 실패. matchId:{}", matchId, exception);
            }
        }

        log.info(
                "🟢Bet365 배당률 수집 작업 완료. targetRange:{}, 타겟 경기 수:{}, "
                        + "처리 성공 경기 수:{}, 처리 실패 경기 수:{}, 배당률 저장 성공 건수:{}",
                targetRange,
                matchIds.size(),
                succeededCount,
                failedCount,
                savedCount
        );
    }

    private ZonedDateTime toSeoulTime(ZonedDateTime standardTime) {
        if (standardTime == null) {
            throw new IllegalArgumentException("배당률 수집 기준 시각은 필수입니다.");
        }
        return standardTime.withZoneSameInstant(SEOUL_TIME_ZONE_ID);
    }
}
