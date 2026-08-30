package com.scorenow.scorenow_api.domain.match.dto.response.statistics.football;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballStats;
import com.scorenow.scorenow_api.domain.match.dto.response.statistics.MatchStatisticsResponse;
import lombok.Getter;

@Getter
public class FootballStatisticsResponse implements MatchStatisticsResponse {
    private FootballStats homeStats;
    private FootballStats awayStats;

    public FootballStatisticsResponse(FootballStats homeStats, FootballStats awayStats) {
        this.homeStats = homeStats;
        this.awayStats = awayStats;
    }
}
