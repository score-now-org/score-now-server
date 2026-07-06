package com.scorenow.scorenow_api.external.betsapi.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.entity.MatchPeriod;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BetsEventResponse {
    private final DataOrigin provider = DataOrigin.BETS;

    private int success;
    private BetsPager pager;
    private List<Event> results;

    @Getter
    @Setter
    public static class Event {
        private String id;

        @JsonProperty("sport_id")
        private String sportId;

        private String time;

        @JsonProperty("time_status")
        private String timeStatus;

        private BetsEventResponse.League league;
        private BetsEventResponse.Team home;
        private BetsEventResponse.Team away;
        private String ss;

        @JsonProperty("bet365_id")
        private String bet365Id;

        private Timer timer;

        public MatchDetailDocument.MatchClock toMatchClock(LocalDateTime startAt) {
            // timer 값이 아예 안내려오는 경우 (경기 시작 전, 경기 종료)
            if (timer == null) {
                return null;
            }

            return MatchDetailDocument.MatchClock.builder()
                    .startAt(startAt != null ? startAt.toString() : null)
                    .elapsedMinutes(timer.tm)
                    .elapsedSeconds(timer.ts)
                    .period(MatchPeriod.fromCode(timer.md))
                    .running("1".equals(timer.tt))
                    .additionalMinutes(timer.ta)
                    .build();
        }
    }

    @Getter
    @Setter
    public static class League {
        private String id;
        private String name;
        private String cc;
    }

    @Getter
    @Setter
    public static class Team {
        private String id;
        private String name;

        @JsonProperty("image_id")
        private String imageId;

        private String cc;
    }

    @Getter
    @Setter
    public static class Timer {
        private Integer tm;     // 경과 분
        private Integer ts;     // 경과 초
        private String tt;      // 타이머 상태 (0:정지,1:진행)
        private Integer ta;     // 추가시간
        private Integer md;     // 전후반 구분 (0:전반,1:후반)
    }

}
