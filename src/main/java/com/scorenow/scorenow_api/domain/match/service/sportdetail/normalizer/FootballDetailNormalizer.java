package com.scorenow.scorenow_api.domain.match.service.sportdetail.normalizer;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetailType;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.*;
import com.scorenow.scorenow_api.domain.match.service.MatchEventResolver;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsViewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FootballDetailNormalizer implements SportDetailNormalizer {

    private static final int MD_FIRST_HALF = 0;
    private static final int MD_SECOND_HALF = 1;
    private static final int MD_EXTRA_FIRST_HALF = 2;
    private static final int MD_EXTRA_SECOND_HALF = 3;
    private static final int MD_PENALTY_SHOOTOUT = 4;

    private static final Integer HALF_TIME_MINUTE = 45;
    private static final Integer FULL_TIME_MINUTE = 90;
    private static final Integer EXTRA_HALF_TIME_MINUTE = 105;
    private static final Integer EXTRA_FULL_TIME_MINUTE = 120;

    private final MatchEventResolver matchEventResolver;

    @Override
    public SportDetailType type() {
        return SportDetailType.FOOTBALL;
    }

    @Override
    public SportDetail normalize(BetsViewResponse.ViewResult viewResult) {
        FootballPhase phase = toPhase(viewResult);

        return FootballDetail.builder()
                .shootOutScore(FootballPhase.PENALTY_SHOOTOUT == phase ? normalizeShootOutScore(viewResult) : null)
                .homeStats(normalizeHomeStats(viewResult))
                .awayStats(normalizeAwayStats(viewResult))
                .clock(normalizeClock(viewResult, phase))
                .additionalTime(normalizeAdditionalTime(viewResult))
                .build();
    }

    private FootballShootOutScore normalizeShootOutScore(BetsViewResponse.ViewResult viewResult) {
        List<String> eventTexts = viewResult.getEventTexts();
        if (eventTexts == null || eventTexts.isEmpty()) {
            return null;
        }

        Integer homeScore = matchEventResolver.resolveShootOutScore(eventTexts, viewResult.resolveHomeTeamName());
        Integer awayScore = matchEventResolver.resolveShootOutScore(eventTexts, viewResult.resolveAwayTeamName());
        return FootballShootOutScore.builder()
                .homeScore(homeScore)
                .awayScore(awayScore)
                .build();
    }


    private FootballStats normalizeHomeStats(BetsViewResponse.ViewResult externalData) {

        BetsViewResponse.Stats stats = externalData.getStats();
        if (stats == null) {
            return null;
        }

        return FootballStats.builder()
                .yellowCards(stats.homeYellowCards())
                .redCards(stats.homeRedCards())
                .shots(stats.homeOffTarget())
                .shotsOnTarget(stats.homeOnTarget())
                .possession(stats.homePossession())
                .offsides(stats.homeOffsides())
                .fouls(stats.homeFouls())
                .corners(stats.homeCorners())
                .freeKicks(stats.homeFreeKicks())
                .build();
    }

    private FootballStats normalizeAwayStats(BetsViewResponse.ViewResult externalData) {
        BetsViewResponse.Stats stats = externalData.getStats();
        if (stats == null) {
            return null;
        }

        return FootballStats.builder()
                .yellowCards(stats.awayYellowCards())
                .redCards(stats.awayRedCards())
                .shots(stats.awayOffTarget())
                .shotsOnTarget(stats.awayOnTarget())
                .possession(stats.awayPossession())
                .offsides(stats.awayOffsides())
                .fouls(stats.awayFouls())
                .corners(stats.awayCorners())
                .freeKicks(stats.awayFreeKicks())
                .build();
    }

    private FootballClock normalizeClock(BetsViewResponse.ViewResult externalData, FootballPhase phase) {

        BetsViewResponse.Timer timer = externalData.getTimer();
        if (timer == null) {
            return null;
        }

        int elapsedMinutes = timer.getTm() != null ? timer.getTm() : 0;
        int elapsedSeconds = timer.getTs() != null ? timer.getTs() : 0;

        return FootballClock.builder()
                .clockSyncedAt(toProviderUpdatedAt(externalData.getInplayUpdatedAt()))
                .phaseElapsedSeconds(FootballClock.resolvePhaseElapsedSeconds(
                        elapsedMinutes * 60 + elapsedSeconds,
                        phase)
                )
                .phase(phase)
                .running(timer.isRunning())
                .providerUpdatedAt(toProviderUpdatedAt(externalData.getInplayUpdatedAt()))
                .build();
    }

    private FootballAdditionalTime normalizeAdditionalTime(BetsViewResponse.ViewResult externalData) {
        BetsViewResponse.Timer timer = externalData.getTimer();
        if (timer == null || timer.getMd() == null || timer.getTa() == null || timer.getTa() < 0) {
            return null;
        }

        if (!timer.isRunning()) {
            return null;
        }

        Integer md = timer.getMd();
        return switch (md) {
            case MD_FIRST_HALF -> FootballAdditionalTime.builder().firstHalf(timer.getTa()).build();
            case MD_SECOND_HALF -> FootballAdditionalTime.builder().secondHalf(timer.getTa()).build();
            case MD_EXTRA_FIRST_HALF -> FootballAdditionalTime.builder().extraFirstHalf(timer.getTa()).build();
            case MD_EXTRA_SECOND_HALF -> FootballAdditionalTime.builder().extraSecondHalf(timer.getTa()).build();
            default -> null;
        };
    }


    /**
     * Bets View API 응답을 기반으로, 시스템 내부에서 사용하는 축구 경기 시간 정보로 변환
     */
    private FootballPhase toPhase(BetsViewResponse.ViewResult externalData) {
        BetsViewResponse.Timer timer = externalData.getTimer();
        if (timer == null || timer.getMd() == null) {
            return null;
        }

        Integer md = timer.getMd();

        // 승부차기
        if (Integer.valueOf(MD_PENALTY_SHOOTOUT).equals(md)) {
            return FootballPhase.PENALTY_SHOOTOUT;
        }

        // 타이머가 흐르는 경우 (전반, 후반, 연장전반, 연장후반)
        if (timer.isRunning()) {
            return switch (md) {
                case MD_FIRST_HALF -> FootballPhase.FIRST_HALF;
                case MD_SECOND_HALF -> FootballPhase.SECOND_HALF;
                case MD_EXTRA_FIRST_HALF -> FootballPhase.EXTRA_FIRST_HALF;
                case MD_EXTRA_SECOND_HALF -> FootballPhase.EXTRA_SECOND_HALF;
                default -> null;
            };
        }

        return switch (md) {
            case MD_FIRST_HALF -> FootballPhase.FIRST_HALF;
            case MD_SECOND_HALF -> resolveStoppedSecondHalfPhase(timer);
            case MD_EXTRA_FIRST_HALF -> resolveStoppedExtraFirstHalfPhase(timer);
            case MD_EXTRA_SECOND_HALF -> resolveStoppedExtraSecondHalfPhase(timer);
            default -> null;
        };
    }

    /**
     * 하프타임, 후반전, 경기 종료
     */
    private FootballPhase resolveStoppedSecondHalfPhase(BetsViewResponse.Timer timer) {
        // 경기 종료 (연장전 가지 않고 끝나는 경우)
        if (FULL_TIME_MINUTE.equals(timer.getTm())) {
            return FootballPhase.FULL_TIME;
        }

        // 하프 타임 (전반전 종료 후 후반전 시작 전)
        if (HALF_TIME_MINUTE.equals(timer.getTm())) {
            return FootballPhase.HALF_TIME;
        }

        return FootballPhase.SECOND_HALF;
    }

    /**
     * 연장 대기, 연장 전반
     */
    private FootballPhase resolveStoppedExtraFirstHalfPhase(BetsViewResponse.Timer timer) {
        // 연장 대기 (후반전 종료 후 연장 전반 시작 전)
        if (FULL_TIME_MINUTE.equals(timer.getTm())) {
            return FootballPhase.EXTRA_TIME_WAITING;
        }

        return FootballPhase.EXTRA_FIRST_HALF;
    }

    /**
     * 연장 후반, 연장 종료, 경기 종료
     * 참고 : 연장 종료는 하지 판단하지 않는다. 이유는 Bets 에서 내려주는 응답값만으로는 승부차기와 연장 종료가 구분이 안간다.
     */
    private FootballPhase resolveStoppedExtraSecondHalfPhase(BetsViewResponse.Timer timer) {
        // 경기 종료 (승부차기 가지 않고 끝나는 경우)
        if (EXTRA_FULL_TIME_MINUTE.equals(timer.getTm())) {
            return FootballPhase.FULL_TIME;
        }

        return FootballPhase.EXTRA_SECOND_HALF;
    }

    private Instant toProviderUpdatedAt(String inplayUpdatedAt) {
        if (inplayUpdatedAt == null || inplayUpdatedAt.isBlank()) {
            return null;
        }

        try {
            return Instant.ofEpochSecond(Long.parseLong(inplayUpdatedAt));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
