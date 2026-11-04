package com.scorenow.scorenow_api.domain.odds.scheduler;

import com.scorenow.scorenow_api.domain.odds.service.Bet365OddsCollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

import static com.scorenow.scorenow_api.domain.match.constant.MatchConstants.SEOUL_TIME_ZONE;
import static com.scorenow.scorenow_api.domain.match.constant.MatchConstants.SEOUL_TIME_ZONE_ID;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class Bet365OddsCollectionScheduler {

    private final Bet365OddsCollectionService collectionService;

    /**
     * 경기 시작 48시간 전부터 1시간 전까지 30분 간격으로 수집한다.
     */
    @Scheduled(cron = "0 0/30 * * * *", zone = SEOUL_TIME_ZONE)
    public void collectDistantPrematchOdds() {
        try {
            log.info("📅 배당률 수집 시작 (경기 시작 48시간 전 ~ 1시간 전)");
            collectionService.collectDistantPrematchOdds(ZonedDateTime.now(SEOUL_TIME_ZONE_ID));
        } catch (Exception exception) {
            log.error("Bet365 원거리 프리매치 배당률 수집 작업 실패", exception);
        }
    }

    /**
     * 경기 시작 1시간 전부터 경기 시작 직전까지 5분 간격으로 수집한다.
     */
    @Scheduled(cron = "0 0/5 * * * *", zone = SEOUL_TIME_ZONE)
    public void collectNearStartPrematchOdds() {
        try {
            log.info("📅 배당률 수집 시작 (경기 시작 1시간 전 ~ )");
            collectionService.collectNearStartPrematchOdds(ZonedDateTime.now(SEOUL_TIME_ZONE_ID));
        } catch (Exception exception) {
            log.error("Bet365 경기 임박 프리매치 배당률 수집 작업 실패", exception);
        }
    }
}
