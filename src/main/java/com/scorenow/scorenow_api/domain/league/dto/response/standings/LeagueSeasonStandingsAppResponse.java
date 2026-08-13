package com.scorenow.scorenow_api.domain.league.dto.response.standings;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class LeagueSeasonStandingsAppResponse {

    private Long leagueId;
    private Long leagueSeasonId;

    private String leagueImageUrl;
    private String leagueName;
    private String seasonName;

    private LocalDate startAt;
    private LocalDate endAt;

    // 순위 관리 타입이 IMAGE 인 경우
    private String standingsImageUrl;

    // 순위 관리 타입이 EXTERNAL_DATA 인 경우
    private StandingsDataResponse standings;
}
