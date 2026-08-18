package com.scorenow.scorenow_api.domain.league.controller;

import com.scorenow.scorenow_api.domain.league.dto.response.standings.LeagueSeasonStandingsAppResponse;
import com.scorenow.scorenow_api.domain.league.service.LeagueSeasonStandingsAppQueryService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/app/league-seasons")
@RequiredArgsConstructor
public class LeagueSeasonStandingsAppController {

    private final LeagueSeasonStandingsAppQueryService appQueryService;

    @GetMapping("/{leagueId}/standings")
    public ApiResponse<LeagueSeasonStandingsAppResponse> getStandings(@PathVariable Long leagueId) {
        return ApiResponse.success(appQueryService.getLeagueSeasonStandings(leagueId));
    }
}
