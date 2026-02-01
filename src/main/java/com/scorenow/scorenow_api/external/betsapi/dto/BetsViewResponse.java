package com.scorenow.scorenow_api.external.betsapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.model.MatchStats;
import lombok.Data;

import java.util.List;


@Data
public class BetsViewResponse {
    private List<ViewResult> results;


    public boolean hasResult() {
        return results != null && !results.isEmpty();
    }

    public MatchDetailDocument toDocument(String eventId) {
        ViewResult result = results.get(0);
        return MatchDetailDocument.builder()
                .id(eventId)
                .homeStats(result.toMatchStats(0))
                .awayStats(result.toMatchStats(1))
                .extraTime(result.toExtraTime())   // 추가시간
                .build();
    }

    @Data
    public static class ViewResult {
        private String id;
        private String ss;
        private Stats stats;
        private Timer timer;

        public MatchStats toMatchStats(int idx) {
            if (this.stats == null) return MatchStats.builder().build();
            return MatchStats.builder()
                    .yellowCards(parse(stats.yellowCards, idx))
                    .redCards(parse(stats.redCards, idx))
                    .shots(parse(stats.shots, idx))
                    .shotsOnTarget(parse(stats.shotsOnTarget, idx))
                    .possession(parse(stats.possession, idx))
                    .offsides(parse(stats.offsides, idx))
                    .fouls(parse(stats.fouls, idx))
                    .corners(parse(stats.corners, idx))
                    .freeKicks(parse(stats.freeKicks, idx))
                    .build();
        }

        public MatchDetailDocument.ExtraTime toExtraTime() {
            if (this.timer == null) return defaultExtraTime();

            return MatchDetailDocument.ExtraTime.builder()
                    .firstHalf(this.timer.getTa() != null ? this.timer.getTa() : 0)
                    .secondHalf(0)
                    .overTime(0)
                    .build();
        }

        private MatchDetailDocument.ExtraTime defaultExtraTime() {
            return MatchDetailDocument.ExtraTime.builder()
                    .firstHalf(0)
                    .secondHalf(0)
                    .overTime(0)
                    .build();
        }

        private Integer parse(List<String> list, int i) {
            try {
                return (list != null && list.size() > i) ? Integer.parseInt(list.get(i)) : 0;
            } catch (NumberFormatException e) { return 0; }
        }

        public Integer getHomeScore() {
    try {
        if (ss != null) {
            String[] parts = ss.split("-");
            if (parts.length >= 1 && !parts[0].isEmpty()) return Integer.parseInt(parts[0]);
        }
    } catch (Exception e) { }
    return 0;
}

public Integer getAwayScore() {
    try {
        if (ss != null) {
            String[] parts = ss.split("-");
            if (parts.length >= 2 && !parts[1].isEmpty()) return Integer.parseInt(parts[1]);
        }
    } catch (Exception e) { }
    return 0;
}
    }

    @Data
    public static class Stats {
        @JsonProperty("yellow_cards") private List<String> yellowCards;
        @JsonProperty("red_cards") private List<String> redCards;
        @JsonProperty("shott_total") private List<String> shots;
        @JsonProperty("shott_on_target") private List<String> shotsOnTarget;
        @JsonProperty("possession_rt") private List<String> possession;
        @JsonProperty("offsides") private List<String> offsides;
        @JsonProperty("fouls") private List<String> fouls;
        @JsonProperty("corners") private List<String> corners;
        @JsonProperty("free_kicks") private List<String> freeKicks;
    }

    @Data
    public static class Timer {
        private Integer tm;
        private Integer ts;
        private String tt;
        private Integer ta;
    }
}