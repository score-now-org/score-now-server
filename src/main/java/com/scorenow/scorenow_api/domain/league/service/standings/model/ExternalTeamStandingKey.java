package com.scorenow.scorenow_api.domain.league.service.standings.model;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class ExternalTeamStandingKey {
    private final Long leagueId;
    private final DataOrigin dataOrigin;
    private final String externalTeamId;
}
