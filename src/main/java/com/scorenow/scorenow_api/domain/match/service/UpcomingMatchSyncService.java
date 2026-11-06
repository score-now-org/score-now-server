package com.scorenow.scorenow_api.domain.match.service;

import static com.scorenow.scorenow_api.domain.common.enums.DataOrigin.BETS;

import com.scorenow.scorenow_api.domain.match.dto.MatchSyncLeagueTarget;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsEventResponse;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpcomingMatchSyncService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final BetsApiClient betsApiClient;
    private final MatchSyncLeagueTargetReader matchSyncLeagueTargetReader;
    private final UpcomingMatchSyncProcessor upcomingMatchSyncProcessor;

    /**
     * 지정한 UTC 날짜의 예정 경기를 동기화한다.
     */
    public int sync(LocalDate utcDate) {
        String resolvedUtcDay = resolveUtcDay(utcDate);
        List<MatchSyncLeagueTarget> leagueTargets = matchSyncLeagueTargetReader.findEnabledLeagueTargets(BETS);

        log.info("🚀️예정 경기 동기화 시작. day={}", resolvedUtcDay);

        int totalSynced = 0;
        for (MatchSyncLeagueTarget leagueTarget : leagueTargets) {
            try {
                BetsEventResponse response = betsApiClient.getUpcomingEvents(
                        leagueTarget.apiSportId(),
                        leagueTarget.apiLeagueId(),
                        resolvedUtcDay
                );

                if (response == null || response.getResults() == null) {
                    continue;
                }

                for (BetsEventResponse.Event event : response.getResults()) {
                    try {
                        upcomingMatchSyncProcessor.process(leagueTarget, event);
                        totalSynced++;
                    } catch (Exception e) {
                        logEventSyncFailure(resolvedUtcDay, leagueTarget, e);
                    }
                }
            } catch (Exception e) {
                logLeagueSyncFailure(resolvedUtcDay, leagueTarget, e);
            }
        }

        log.info("🏁예정 경기 동기화 완료. day={}, count={}", resolvedUtcDay, totalSynced);
        return totalSynced;
    }

    private String resolveUtcDay(LocalDate utcDate) {
        return (utcDate == null ? LocalDate.now(ZoneOffset.UTC) : utcDate)
                .format(DATE_FORMAT);
    }

    private void logEventSyncFailure(
            String day,
            MatchSyncLeagueTarget leagueTarget,
            Exception exception
    ) {
        log.error(
                "❌예정 경기 동기화 실패. day={}, external=[api_sport_id:{}, api_league_id:{}], "
                        + "internal=[sport_id:{}, sport_code:{}, league_id:{}, league_name:{}]",
                day,
                leagueTarget.apiSportId(),
                leagueTarget.apiLeagueId(),
                leagueTarget.internalSportId(),
                leagueTarget.sportCode(),
                leagueTarget.internalLeagueId(),
                leagueTarget.leagueName(),
                exception
        );
    }

    private void logLeagueSyncFailure(
            String day,
            MatchSyncLeagueTarget leagueTarget,
            Exception exception
    ) {
        log.error(
                "❌예정 리그 동기화 실패. day={}, external=[api_sport_id:{}, api_league_id:{}], "
                        + "internal=[sport_id:{}, sport_code:{}, league_id:{}, league_name:{}]",
                day,
                leagueTarget.apiSportId(),
                leagueTarget.apiLeagueId(),
                leagueTarget.internalSportId(),
                leagueTarget.sportCode(),
                leagueTarget.internalLeagueId(),
                leagueTarget.leagueName(),
                exception
        );
    }
}
