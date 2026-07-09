package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchPeriod;
import com.scorenow.scorenow_api.domain.match.entity.MatchResult;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * MatchDetailDocument 의 MatchClock 정보를 활용하여,
 * 앱에서 조회되는 경기 시간 정보 표시 문구를 생성한다.
 */

@Component
public class MatchDisplayTextResolver {
    private static final DateTimeFormatter SCHEDULED_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private static final int SECOND_HALF_BASE_MINUTE = 45;
    private static final int EXTRA_FIRST_HALF_BASE_MINUTE = 90;
    private static final int EXTRA_SECOND_HALF_BASE_MINUTE = 105;

    public String resolveInPlayDisplayText(MatchDetailDocument.MatchClock matchClock) {

        if (matchClock == null) {
            return null;
        }

        // 경기 구간 전환 문구 반환 (전반 종료, 후반 종료, 연장 전반 종료, 승부차기)
        String periodBreakText = resolvePeriodBreakText(matchClock);
        if (periodBreakText != null) {
            return periodBreakText;
        }

        MatchPeriod period = matchClock.getPeriod();
        Integer displayElapsedMinutes = resolvePeriodElapsedMinutes(matchClock);

        // 구간과 경과 시간 모두 있는 경우 (예: 전반 15 / 후반 20)
        if (period != null && displayElapsedMinutes != null) {
            return period.getDescription() + " " + displayElapsedMinutes;
        }

        // 구간만 있는 경우 (예: 전반)
        if (period != null) {
            return period.getDescription();
        }

        return displayElapsedMinutes != null ? String.valueOf(displayElapsedMinutes) : null;
    }

    public String resolveScheduledDisplayText(Match match) {
        if (match.getStartAt() == null) {
            return null;
        }

        return match.getStartAt().format(SCHEDULED_TIME_FORMATTER);
    }

    public String resolveEndedDisplayText(MatchResult matchResult) {
        return switch (matchResult) {
            case HOME_WIN -> "홈팀 승";
            case AWAY_WIN -> "홈팀 패";
            case DRAW -> "무승부";
            case UNKNOWN -> null;
        };
    }

    /**
     * 각 구간 종료 및 다음 구간 시작 전 문구 생성
     */
    private String resolvePeriodBreakText(MatchDetailDocument.MatchClock matchClock) {
        if (!Boolean.FALSE.equals(matchClock.getRunning())) {
            return null;
        }

        Integer elapsedMinutes = matchClock.getElapsedMinutes();
        Integer elapsedSeconds = matchClock.getElapsedSeconds();
        Integer additionalMinutes = matchClock.getAdditionalMinutes();
        MatchPeriod period = matchClock.getPeriod();

        if (!Integer.valueOf(0).equals(elapsedSeconds) || !Integer.valueOf(0).equals(additionalMinutes)) {
            return null;
        }

        // 전반전 종료 + 후반전 시작 전
        if (period == MatchPeriod.SECOND_HALF && Integer.valueOf(45).equals(elapsedMinutes)) {
            return "전반 종료";
        }
        // 후반전 종료 + 연장 전반 시작 전
        if (period == MatchPeriod.EXTRA_FIRST_HALF && Integer.valueOf(90).equals(elapsedMinutes)) {
            return "후반 종료";
        }
        // 연장 전반 종료 + 연장 후반 시작 전
        if (period == MatchPeriod.EXTRA_SECOND_HALF && Integer.valueOf(105).equals(elapsedMinutes)) {
            return "연장 전반 종료";
        }
        // 연장 후반 종료 + 승부차기 시작 전 ()
        if (period == MatchPeriod.PENALTY_SHOOTOUT && Integer.valueOf(120).equals(elapsedMinutes)) {
            return "승부차기";
        }

        return null;
    }

    /**
     * 각 경기 구간 별 경과 시간 재계산
     * 1. 구간명: 전반 / 누적 경과 시간: 30       => 전반 30
     * 2. 구간명: 후반 / 누적 경과 시간: 60       => 후반 15
     * 3. 구간명: 연장 전반 / 누적 경과 시간: 103  => 연장 전반 13
     */
    public Integer resolvePeriodElapsedMinutes(MatchDetailDocument.MatchClock matchClock) {
        MatchPeriod period = matchClock.getPeriod();
        Integer elapsedMinutes = matchClock.getElapsedMinutes();

        if (period == null || elapsedMinutes == null) {
            return null;
        }

        return switch (period) {
            case FIRST_HALF -> elapsedMinutes;
            case SECOND_HALF -> Math.max(elapsedMinutes - SECOND_HALF_BASE_MINUTE, 0);
            case EXTRA_FIRST_HALF -> Math.max(elapsedMinutes - EXTRA_FIRST_HALF_BASE_MINUTE, 0);
            case EXTRA_SECOND_HALF -> Math.max(elapsedMinutes - EXTRA_SECOND_HALF_BASE_MINUTE, 0);
            case PENALTY_SHOOTOUT -> null;
        };
    }

}
