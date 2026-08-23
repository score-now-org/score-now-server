package com.scorenow.scorenow_api.domain.league.service.standings.normalizer;

import com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument;
import com.scorenow.scorenow_api.domain.league.dto.LeagueSeasonStandingsSyncTarget;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsStandingsResponse;

public interface StandingsNormalizer {

    SportCode sportCode();

    LeagueSeasonStandingsDataDocument normalize(
            BetsStandingsResponse.Result result,
            LeagueSeasonStandingsSyncTarget syncTarget);
}
