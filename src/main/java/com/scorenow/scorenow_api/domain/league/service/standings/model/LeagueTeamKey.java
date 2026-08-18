package com.scorenow.scorenow_api.domain.league.service.standings.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class LeagueTeamKey {
    private final Long leagueId;
    private final Long teamId;
}
