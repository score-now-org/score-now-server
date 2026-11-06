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
public class EndedMatchSyncService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final BetsApiClient betsApiClient;
    private final MatchSyncLeagueTargetReader matchSyncLeagueTargetReader;
    private final EndedMatchSyncProcessor endedMatchSyncProcessor;

    /**
     * UTC 기준 전일과 당일의 Ended API를 조회하여 기존 경기의 최종 상태를 보정한다.
     */
    public int syncRecent() {
        List<MatchSyncLeagueTarget> leagueTargets = matchSyncLeagueTargetReader.findEnabledLeagueTargets(BETS);
        LocalDate utcToday = LocalDate.now(ZoneOffset.UTC);

        return syncForDate(utcToday.minusDays(1), leagueTargets)
                + syncForDate(utcToday, leagueTargets);
    }

    private int syncForDate(LocalDate utcDate, List<MatchSyncLeagueTarget> leagueTargets) {
        String resolvedUtcDay = utcDate.format(DATE_FORMAT);
        log.info("🚀️종료 경기 상태 동기화 시작. day={}", resolvedUtcDay);

        int totalUpdated = 0;
        for (MatchSyncLeagueTarget leagueTarget : leagueTargets) {
            try {
                BetsEventResponse response = betsApiClient.getEndedEvents(
                        leagueTarget.apiSportId(),
                        leagueTarget.apiLeagueId(),
                        resolvedUtcDay
                );

                if (response == null || response.getResults() == null) {
                    continue;
                }

                for (BetsEventResponse.Event event : response.getResults()) {
                    try {
                        if (endedMatchSyncProcessor.process(leagueTarget, event)) {
                            totalUpdated++;
                        }
                    } catch (Exception e) {
                        logEventSyncFailure(resolvedUtcDay, leagueTarget, e);
                    }
                }
            } catch (Exception e) {
                logLeagueSyncFailure(resolvedUtcDay, leagueTarget, e);
            }
        }

        log.info("🏁종료 경기 상태 동기화 완료. day={}, updatedCount={}", resolvedUtcDay, totalUpdated);
        return totalUpdated;
    }

    private void logEventSyncFailure(
            String day,
            MatchSyncLeagueTarget leagueTarget,
            Exception exception
    ) {
        log.error(
                "❌종료 경기 상태 동기화 실패. day={}, external=[api_sport_id:{}, api_league_id:{}], "
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
                "❌종료 리그 상태 동기화 실패. day={}, external=[api_sport_id:{}, api_league_id:{}], "
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
