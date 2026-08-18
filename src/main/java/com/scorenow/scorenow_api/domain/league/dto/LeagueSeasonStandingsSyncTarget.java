package com.scorenow.scorenow_api.domain.league.dto;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LeagueSeasonStandingsSyncTarget {

    private final Long sportId;
    private final SportCode sportCode;
    private final Long leagueSeasonStandingsId;
    private final Long leagueSeasonId;
    private final Long leagueId;
    private final DataOrigin dataOrigin;
    private final String apiLeagueId;

}
