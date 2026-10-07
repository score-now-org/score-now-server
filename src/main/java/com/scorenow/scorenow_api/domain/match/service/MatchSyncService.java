package com.scorenow.scorenow_api.domain.match.service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

import com.scorenow.scorenow_api.domain.match.dto.MatchSyncLeagueTarget;
import org.springframework.stereotype.Service;

import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsEventResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.scorenow.scorenow_api.domain.common.enums.DataOrigin.BETS;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchSyncService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final String UPCOMING = "예정";
    private static final String ENDED = "종료";

    private final BetsApiClient betsApiClient;

    private final MatchEventSyncService matchEventSyncService;
    private final MatchSyncLeagueTargetReader matchSyncLeagueTargetReader;

    /**
     * 예정 경기 동기화
     */
    public int syncUpcomingMatches(LocalDate utcDate) {
        List<MatchSyncLeagueTarget> leagueTargets = matchSyncLeagueTargetReader.findEnabledLeagueTargets(BETS);
        String resolvedUtcDay = resolveUtcDay(utcDate);

        return syncEventsForLeagueTargets(
                UPCOMING,
                resolvedUtcDay,
                leagueTargets,
                leagueTarget -> betsApiClient.getUpcomingEvents(
                        leagueTarget.apiSportId(),
                        leagueTarget.apiLeagueId(),
                        resolvedUtcDay
                )
        );
    }

    /**
     * 종료 경기 동기화 (전일 ~ 당일)
     * 참고: 추후 관리자용 수동 스케줄링 기능을 추가할 필요가 생기면 그때 syncEndedMatchesForDate(LocalDate utcDate) 를 개발하면 된다.
     */
    public int syncRecentEndedMatches() {
        List<MatchSyncLeagueTarget> leagueTargets = matchSyncLeagueTargetReader.findEnabledLeagueTargets(BETS);

        LocalDate utcToday = LocalDate.now(ZoneOffset.UTC);
        LocalDate utcYesterday = utcToday.minusDays(1);

        String resolvedUtcToday = resolveUtcDay(utcToday);
        String resolvedUtcYesterday = resolveUtcDay(utcYesterday);

        int totalSynced = 0;

        totalSynced += syncEventsForLeagueTargets(
                ENDED,
                resolvedUtcYesterday,
                leagueTargets,
                leagueTarget -> betsApiClient.getEndedEvents(
                        leagueTarget.apiSportId(),
                        leagueTarget.apiLeagueId(),
                        resolvedUtcYesterday
                )
        );

        totalSynced += syncEventsForLeagueTargets(
                ENDED,
                resolvedUtcToday,
                leagueTargets,
                leagueTarget -> betsApiClient.getEndedEvents(
                        leagueTarget.apiSportId(),
                        leagueTarget.apiLeagueId(),
                        resolvedUtcToday
                )
        );

        return totalSynced;
    }

    private int syncEventsForLeagueTargets(
            String logLabel,
            String day,
            List<MatchSyncLeagueTarget> leagueTargets,
            Function<MatchSyncLeagueTarget, BetsEventResponse> fetchEvents
    ) {
        log.info("🚀️{} 경기 동기화 시작. day={}", logLabel, day);

        int totalSynced = 0;

        for (MatchSyncLeagueTarget leagueTarget : leagueTargets) {
            try {
                BetsEventResponse response = fetchEvents.apply(leagueTarget);

                if (response == null || response.getResults() == null) {
                    continue;
                }

                for (BetsEventResponse.Event event : response.getResults()) {
                    try {
                        matchEventSyncService.syncEvent(leagueTarget, event);
                        totalSynced++;
                    } catch (Exception e) {
                        log.error("❌경기 동기화 실패 : " +
                                        "syncType={}, " +
                                        "day={}, " +
                                        "external=[api_sport_id:{}, api_league_id:{}], " +
                                        "internal=[sport_id:{}, sport_code:{}, league_id:{}, league_name:{}]",
                                logLabel,
                                day,
                                leagueTarget.apiSportId(), leagueTarget.apiLeagueId(),
                                leagueTarget.internalSportId(), leagueTarget.sportCode(), leagueTarget.internalLeagueId(), leagueTarget.leagueName(),
                                e);
                    }
                }

            } catch (Exception e) {
                log.error("❌리그 동기화 실패 : " +
                                "syncType={}, " +
                                "day={}, " +
                                "external=[api_sport_id:{}, api_league_id:{}], " +
                                "internal=[sport_id:{}, sport_code:{}, league_id:{}, league_name:{}]",
                        logLabel,
                        day,
                        leagueTarget.apiSportId(), leagueTarget.apiLeagueId(),
                        leagueTarget.internalSportId(), leagueTarget.sportCode(), leagueTarget.internalLeagueId(), leagueTarget.leagueName(),
                        e);
            }
        }
        log.info("🏁{} 경기 동기화 완료. day={}, count={}", logLabel, day, totalSynced);
        return totalSynced;
    }

    /**
     * YYYYMMDD 로 변경
     * 참고: BETS는 UTC 기반으로 동작하기 때문에 날짜 기반 요청을 보낼때에는 UTC 로 보내야지 정상적으로 처리가 가능하다.
     */
    private String resolveUtcDay(LocalDate utcDate) {
        return (utcDate == null
                ? LocalDate.now(ZoneOffset.UTC)
                : utcDate)
                .format(DATE_FORMAT);
    }
}
