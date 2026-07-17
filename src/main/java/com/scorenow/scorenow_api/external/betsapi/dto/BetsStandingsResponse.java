package com.scorenow.scorenow_api.external.betsapi.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BetsStandingsResponse {
    private final DataOrigin provider = DataOrigin.BETS;

    private Integer success;
    private List<Result> results;

    public boolean isSuccess() {
        return Integer.valueOf(1).equals(success);
    }

    public boolean hasResults() {
        return isSuccess() && results != null && !results.isEmpty();
    }

    @Getter
    @Setter
    public static class Result {
        private Season season;
        private Overall overall;
    }

    @Getter
    @Setter
    public static class Season {
        @JsonProperty("sport_id")
        private Integer sportId;

        @JsonProperty("start_time")
        private Long startTime;

        @JsonProperty("end_time")
        private Long endTime;

        private String name;
    }

    @Getter
    @Setter
    public static class Overall {
        private List<Table> tables;
    }

    @Getter
    @Setter
    public static class Table {
        @JsonProperty("name")
        private String name;

        @JsonProperty("groupname")
        private String groupName;

        @JsonProperty("currentround")
        private Integer currentRound;

        @JsonProperty("maxrounds")
        private Integer maxRounds;

        private List<Row> rows;
    }

    @Getter
    @Setter
    public static class Row {
        private Integer pos;            // 순위
        private Integer change;         // 순위 변동값 (ex: 1위→2위 = -1 / 3위→1위 = 2)
        private Integer win;            // 승
        private Integer draw;           // 무
        private Integer loss;           // 패

        @JsonProperty("goalsfor")
        private Integer goalsFor;       // 득점

        @JsonProperty("goalsagainst")
        private Integer goalsAgainst;   // 실점

        private Integer points;         // 승점
        private Promotion promotion;    // 승격,진출,강등 상태

        private Team team;              // 팀 정보

        public int played() {
            return valueOrZero(win) + valueOrZero(draw) + valueOrZero(loss);
        }

        public int goalDifference() {
            return valueOrZero(goalsFor) - valueOrZero(goalsAgainst);
        }

        public int safeWin() {
            return valueOrZero(win);
        }

        public int safeDraw() {
            return valueOrZero(draw);
        }

        public int safeLoss() {
            return valueOrZero(loss);
        }

        public int safeGoalsFor() {
            return valueOrZero(goalsFor);
        }

        public int safeGoalsAgainst() {
            return valueOrZero(goalsAgainst);
        }

        private int valueOrZero(Integer value) {
            return value != null ? value : 0;
        }
    }

    @Getter
    @Setter
    public static class Promotion {
        private String name;            // ex: Champions League, Europa League, Relegation

        @JsonProperty("shortname")
        private String shortName;       // ex: CL, UEFA, rel
    }

    @Getter
    @Setter
    @EqualsAndHashCode(of = "id")
    public static class Team {
        private String id;
        private String name;

        @JsonProperty("image_id")
        private String imageId;

        private String cc;
    }
}
