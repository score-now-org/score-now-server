package com.scorenow.scorenow_api.external.betsapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Data
public class BetsBaseballViewResponse {
    private final DataOrigin provider = DataOrigin.BETS;

    private List<ViewResult> results;

    public boolean hasResults() {
        return results != null && !results.isEmpty();
    }

    @Data
    public static class ViewResult {
        private static final String RUN_SCORE_KEY = "run";
        private static final String HIT_SCORE_KEY = "hit";
        private static final String EXTRA_INNING_SCORE_KEY = "ot";

        private String id;

        @JsonProperty("sport_id")
        private String sportId;

        private String time;

        @JsonProperty("time_status")
        private String timeStatus;

        private League league;

        /**
         * Bets 야구 응답은 home에 실제 원정팀, away에 실제 홈팀을 내려준다.
         * 시스템의 홈/원정 의미에 맞추기 위해 반대로 매핑한다.
         */
        @JsonProperty("away")
        private Team homeTeam;

        @JsonProperty("home")
        private Team awayTeam;

        /**
         * Bets 야구 응답의 순서는 "실제 원정팀-실제 홈팀"이다.
         * 일부 상태에서는 null일 수 있으므로 총점은 scores.run을 우선 사용한다.
         */
        private String ss;

        /**
         * 이닝 번호("1", "2", ...), run, hit, ot를 키로 가지는 점수 정보.
         * 관측되지 않은 연장 이닝도 받을 수 있도록 고정 필드 대신 Map으로 매핑한다.
         */
        private Map<String, Score> scores;

        private List<EventText> events;
        private Extra extra;

        @JsonProperty("has_lineup")
        private Integer hasLineup;

        @JsonProperty("inplay_created_at")
        private String inplayCreatedAt;

        @JsonProperty("inplay_updated_at")
        private String inplayUpdatedAt;

        @JsonProperty("confirmed_at")
        private String confirmedAt;

        @JsonProperty("bet365_id")
        private String bet365Id;

        public Integer getHomeScore() {
            return getTotalScore(true);
        }

        public Integer getAwayScore() {
            return getTotalScore(false);
        }

        public Score getInningScore(int inning) {
            if (inning < 1 || scores == null) {
                return null;
            }

            return scores.get(String.valueOf(inning));
        }

        public Score getRunScore() {
            return getScore(RUN_SCORE_KEY);
        }

        public Score getHitScore() {
            return getScore(HIT_SCORE_KEY);
        }

        public Score getExtraInningScore() {
            return getScore(EXTRA_INNING_SCORE_KEY);
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

        private Integer getTotalScore(boolean home) {
            Score runScore = getRunScore();
            String runScoreValue = null;
            if (runScore != null) {
                runScoreValue = home ? runScore.getHome() : runScore.getAway();
            }

            Integer score = parseScore(runScoreValue);
            return score != null ? score : getSummaryScorePart(home ? 1 : 0);
        }

        private Score getScore(String key) {
            return scores == null ? null : scores.get(key);
        }

        private Integer getSummaryScorePart(int index) {
            if (ss == null || ss.isBlank()) {
                return null;
            }

            String[] parts = ss.split("-");
            if (parts.length <= index) {
                return null;
            }

            return parseScore(parts[index]);
        }

        private Integer parseScore(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }

            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private String teamName(Team team) {
            if (team == null || team.getName() == null || team.getName().isBlank()) {
                return null;
            }

            return team.getName().trim();
        }
    }

    /**
     * Bets 야구 scores의 home은 실제 원정팀, away는 실제 홈팀을 의미한다.
     * 사용하는 쪽에서는 일반적인 홈/원정 의미로 접근할 수 있도록 반대로 매핑한다.
     */
    @Data
    public static class Score {
        @JsonProperty("away")
        private String home;

        @JsonProperty("home")
        private String away;
    }

    @Data
    public static class EventText {
        private String id;
        private String text;
    }

    @Data
    public static class Extra {
        /** Bets의 home/away 방향을 시스템의 실제 홈/원정 방향으로 보정한다. */
        @JsonProperty("away_pos")
        private String homePosition;

        @JsonProperty("home_pos")
        private String awayPosition;

        @JsonProperty("numberofperiods")
        private String numberOfPeriods;

        private String round;

        @JsonProperty("stadium_data")
        private StadiumData stadiumData;
    }

    @Data
    public static class StadiumData {
        private String id;
        private String name;
        private String city;
        private String country;
        private String capacity;

        @JsonProperty("googlecoords")
        private String googleCoordinates;
    }

    @Data
    public static class League {
        private String id;
        private String name;
        private String cc;
    }

    @Data
    public static class Team {
        private String id;
        private String name;

        @JsonProperty("image_id")
        private String imageId;

        private String cc;
    }
}
