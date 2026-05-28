package com.scorenow.scorenow_api.external.betsapi.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.entity.MatchPeriod;
import com.scorenow.scorenow_api.domain.match.model.MatchStats;
import com.scorenow.scorenow_api.external.common.ApiProvider;

import lombok.Data;

@Data
public class BetsViewResponse {
    private final ApiProvider provider = ApiProvider.BETS;

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

        public MatchDetailDocument toDocument(Long matchId, LocalDateTime startAt) {
            return MatchDetailDocument.builder()
                    .id(matchId)
                    .homeScore(getHomeScore())
                    .homeStats(toMatchStats(0))
                    .awayScore(getAwayScore())
                    .awayStats(toMatchStats(1))
                    .matchClock(toMatchClock(startAt))
                    .additionalTime(toAdditionalTime())
                    .build();
        }

        public MatchStats toMatchStats(int idx) {
            if (this.stats == null)
                return MatchStats.builder().build();

            return MatchStats.builder()
                    .yellowCards(parse(stats.yellowCards, idx))
                    .redCards(parse(stats.redCards, idx))
                    .shots(parse(stats.offTarget, idx))
                    .shotsOnTarget(parse(stats.onTarget, idx))
                    .possession(parse(stats.possession, idx))
                    .offsides(parse(stats.offsides, idx))
                    .fouls(parse(stats.fouls, idx))
                    .corners(parse(stats.corners, idx))
                    .freeKicks(parse(stats.freeKicks, idx))
                    .build();
        }

        public MatchDetailDocument.MatchClock toMatchClock(LocalDateTime startAt) {
            // timer 값이 아예 안내려오는 경우 (경기 시작 전, 경기 종료)
            if (this.timer == null) {
                return null;
            }

            return MatchDetailDocument.MatchClock.builder()
                    .startAt(startAt != null ? startAt.toString() : null)
                    .elapsedMinutes(timer.tm)
                    .elapsedSeconds(timer.ts)
                    .period(MatchPeriod.fromCode(timer.md))
                    .running(Timer.RUNNING.equals(timer.tt))
                    .providerUpdatedAt(toProviderUpdatedAt())
                    .build();
        }

        private Instant toProviderUpdatedAt() {
            if (inplayUpdatedAt == null || inplayUpdatedAt.isBlank()) {
                return null;
            }

            try {
                return Instant.ofEpochSecond(Long.parseLong(inplayUpdatedAt));
            } catch (Exception e) {
                return Instant.now();
            }
        }

        public MatchDetailDocument.AdditionalTime toAdditionalTime() {
            // timer 값이 아예 안내려오는 경우 (경기 시작 전, 경기 종료)
            if (this.timer == null) {
                return null;
            }

            return MatchDetailDocument.AdditionalTime.builder()
                    .firstHalf(getFirstHalfAdditionalTime())
                    .secondHalf(getSecondHalfAdditionalTime())
                    .build();
        }

        private Integer getFirstHalfAdditionalTime() {
            if (timer.isFirstHalf()) {
                return timer.ta != null ? timer.ta : null;
            }
            return null;
        }

        private Integer getSecondHalfAdditionalTime() {
            if (timer.isSecondHalf()) {
                return timer.ta != null ? timer.ta : null;
            }
            return null;
        }

        private Integer parse(List<String> list, int i) {
            try {
                return (list != null && list.size() > i) ? Integer.parseInt(list.get(i)) : 0;
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        public Integer getHomeScore() {
            try {
                if (ss != null) {
                    String[] parts = ss.split("-");
                    if (parts.length >= 1 && !parts[0].isEmpty())
                        return Integer.parseInt(parts[0]);
                }
            } catch (Exception e) {
            }
            return 0;
        }

        public Integer getAwayScore() {
            try {
                if (ss != null) {
                    String[] parts = ss.split("-");
                    if (parts.length >= 2 && !parts[1].isEmpty())
                        return Integer.parseInt(parts[1]);
                }
            } catch (Exception e) {
            }
            return 0;
        }

        public boolean hasLineup() {
            return hasLineup != null && hasLineup == 1;
        }
    }

    @Data
    public static class Stats {
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
    }

    @Data
    public static class Timer {
        public static final String RUNNING = "1";
        private Integer tm;     // 경과 분
        private Integer ts;     // 경과 초
        private String tt;      // 타이머 상태 (0:정지,1:진행)
        private Integer ta;     // 추가시간
        private Integer md;     // 전후반 구분 (0:전반,1:후반)

        public boolean isFirstHalf() {
            if (this.md == null) return false;
            return this.md == 0;
        }

        public boolean isSecondHalf() {
            if (this.md == null) return false;
            return this.md == 1;
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
}