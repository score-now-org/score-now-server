package com.scorenow.scorenow_api.external.betsapi.dto;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;

import lombok.Data;

@Data
public class BetsViewResponse {
    private final DataOrigin provider = DataOrigin.BETS;

    private List<ViewResult> results;

    public boolean hasResults() {
        return results != null && !results.isEmpty();
    }

    @Data
    public static class ViewResult {
        private String id;      // 경기 고유 ID

        @JsonProperty("sport_id")
        private String sportId; // 종목 ID
        private String time;    // 경기 시작 시간 (UNIX TIMESTAMP)

        @JsonProperty("time_status")
        private String timeStatus;

        private BetsViewResponse.League league;

        @JsonProperty("home")
        private BetsViewResponse.Team homeTeam;

        @JsonProperty("away")
        private BetsViewResponse.Team awayTeam;

        private String ss;      // 현재 스코어 정보 home:away
        private Stats stats;
        private Timer timer;
        private Extra extra;

        @JsonProperty("events")
        private List<EventText> events; // 경기 중 발생한 이벤트 리스트

        @JsonProperty("has_lineup")
        private Integer hasLineup;  // 라인업 제공 여부 (0:제공안함, 1:제공함)

        @JsonProperty("inplay_updated_at")
        private String inplayUpdatedAt;

        public Integer getHomeScore() {
            return getScorePart(0);
        }

        public Integer getAwayScore() {
            return getScorePart(1);
        }

        private Integer getScorePart(int index) {
            if (ss == null || ss.isBlank()) {
                return 0;
            }

            String[] parts = ss.split("-");
            if (parts.length <= index || parts[index].isBlank()) {
                return 0;
            }

            try {
                return Integer.parseInt(parts[index].trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        public boolean hasLineup() {
            return hasLineup != null && hasLineup == 1;
        }

        public List<String> getEventTexts() {
            if (events == null) {
                return List.of();
            }

            return events.stream()
                    .map(EventText::getText)
                    .filter(Objects::nonNull)
                    .toList();
        }

        public String resolveHomeTeamName() {
            return teamName(homeTeam);
        }

        public String resolveAwayTeamName() {
            return teamName(awayTeam);
        }

        private String teamName(Team team) {
            if (team == null || team.getName() == null || team.getName().isBlank()) {
                return null;
            }

            return team.getName().trim();
        }
    }

    @Data
    public static class Stats {
        private static final int HOME = 0;
        private static final int AWAY = 1;

        @JsonProperty("yellowcards")
        private List<String> yellowCards;
        @JsonProperty("redcards")
        private List<String> redCards;
        @JsonProperty("off_target")
        private List<String> offTarget;
        @JsonProperty("on_target")
        private List<String> onTarget;
        @JsonProperty("possession_rt")
        private List<String> possession;
        @JsonProperty("offsides")
        private List<String> offsides;
        @JsonProperty("fouls")
        private List<String> fouls;
        @JsonProperty("corners")
        private List<String> corners;
        @JsonProperty("freekicks")
        private List<String> freeKicks;

        public Integer homeYellowCards() {
            return parseDetailStats(getYellowCards(), HOME);
        }

        public Integer homeRedCards() {
            return parseDetailStats(getRedCards(), HOME);
        }

        public Integer homeOffTarget() {
            return parseDetailStats(getOffTarget(), HOME);
        }

        public Integer homeOnTarget() {
            return parseDetailStats(getOnTarget(), HOME);
        }

        public Integer homePossession() {
            return parseDetailStats(getPossession(), HOME);
        }

        public Integer homeOffsides() {
            return parseDetailStats(getOffsides(), HOME);
        }

        public Integer homeFouls() {
            return parseDetailStats(getFouls(), HOME);
        }

        public Integer homeCorners() {
            return parseDetailStats(getCorners(), HOME);
        }

        public Integer homeFreeKicks() {
            return parseDetailStats(getFreeKicks(), HOME);
        }

        public Integer awayYellowCards() {
            return parseDetailStats(getYellowCards(), AWAY);
        }

        public Integer awayRedCards() {
            return parseDetailStats(getRedCards(), AWAY);
        }

        public Integer awayOffTarget() {
            return parseDetailStats(getOffTarget(), AWAY);
        }

        public Integer awayOnTarget() {
            return parseDetailStats(getOnTarget(), AWAY);
        }

        public Integer awayPossession() {
            return parseDetailStats(getPossession(), AWAY);
        }

        public Integer awayOffsides() {
            return parseDetailStats(getOffsides(), AWAY);
        }

        public Integer awayFouls() {
            return parseDetailStats(getFouls(), AWAY);
        }

        public Integer awayCorners() {
            return parseDetailStats(getCorners(), AWAY);
        }

        public Integer awayFreeKicks() {
            return parseDetailStats(getFreeKicks(), AWAY);
        }

        private Integer parseDetailStats(List<String> detailStats, int teamIndex) {
            if (detailStats == null || detailStats.isEmpty() || detailStats.size() < 2) {
                return null;
            }

            try {
                return Integer.parseInt(detailStats.get(teamIndex));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    @Data
    public static class Timer {
        public static final String RUNNING = "1";
        private Integer tm;     // 경과 분
        private Integer ts;     // 경과 초
        private String tt;      // 타이머 상태 (0:정지,1:진행)
        private Integer ta;     // 추가시간
        private Integer md;     // 전후반 구분 (0:전반,1:후반)

        public boolean isRunning() {
            return RUNNING.equals(tt);
        }
    }

    @Data
    public static class EventText {
        private String id;
        private String text;
    }

    @Data
    public static class Extra {
        @JsonProperty("stadium_data")
        private StadiumData stadiumData;
    }

    @Data
    public static class StadiumData {
        private String id;              // 경기장 ID
        private String name;            // 경기장명
        private String city;            // 경기장이 위치한 도시
    }

    @Data
    public static class League {
        private String id;
        private String name;
    }

    @Data
    public static class Team {
        private String id;
        private String name;
    }
}
