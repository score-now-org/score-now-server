package com.scorenow.scorenow_api.domain.match.dto;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;

public record MatchSyncLeagueTarget(
        DataOrigin dataOrigin,
        String apiSportId,
        String apiLeagueId,
        Long internalSportId,
        SportCode sportCode,
        Long internalLeagueId,
        String leagueName
) {
}
