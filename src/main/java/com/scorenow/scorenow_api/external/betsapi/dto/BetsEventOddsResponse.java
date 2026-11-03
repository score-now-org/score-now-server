package com.scorenow.scorenow_api.external.betsapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BetsEventOddsResponse(
        Integer success,
        Results results
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Results(
            Stats stats,
            Map<String, List<OddsRecord>> odds
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Stats(
            @JsonProperty("matching_dir") Integer matchingDir,
            @JsonProperty("odds_update") Map<String, Long> oddsUpdate
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OddsRecord(
            String id,
            @JsonProperty("home_od") String homeOdds,
            @JsonProperty("draw_od") String drawOdds,
            @JsonProperty("away_od") String awayOdds,
            String handicap,
            @JsonProperty("over_od") String overOdds,
            @JsonProperty("under_od") String underOdds,
            @JsonProperty("add_time") String addTime,
            String ss,
            @JsonProperty("time_str") String timeStr
    ) {
    }
}
