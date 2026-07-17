package com.scorenow.scorenow_api.domain.league.scheduler;

import com.scorenow.scorenow_api.domain.league.entity.LeagueSeasonStandingsType;
import com.scorenow.scorenow_api.domain.league.repository.LeagueSeasonStandingsRepository;
import com.scorenow.scorenow_api.domain.league.service.LeagueSeasonStandingsSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeagueSeasonStandingsSyncScheduler {

    private final LeagueSeasonStandingsRepository leagueSeasonStandingsRepository;
    private final LeagueSeasonStandingsSyncService leagueSeasonStandingsSyncService;

    /**
     * 리그 순위 데이터 동기화 (외부 API)
     * 작업 주기: 08:30 / 20:30 (매일 2회, KST)
     */
    @Scheduled(cron = "0 30 8,20 * * *", zone = "Asia/Seoul")
    public void syncExternalStandingsData() {
        // 동기화 대상 조회
        List<Long> leagueSeasonIds = leagueSeasonStandingsRepository
                .findCurrentLeagueSeasonIdsByStandingType(LeagueSeasonStandingsType.EXTERNAL_DATA);

        log.info("📅리그 시즌 순위 동기화 시작 - 대상 {}건", leagueSeasonIds.size());

        // 동기화
        for (Long leagueSeasonId : leagueSeasonIds) {
            try {
                leagueSeasonStandingsSyncService.sync(leagueSeasonId);
            } catch (Exception e) {
                log.error("리그 시즌 순위 동기화 실패 - leagueSeasonId={}", leagueSeasonId, e);
            }
        }

        log.info("📅리그 시즌 순위 동기화 종료");
    }
}
