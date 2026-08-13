package com.scorenow.scorenow_api.domain.league.service.standings.resolver;

import com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument;
import com.scorenow.scorenow_api.domain.league.dto.response.standings.StandingsDataResponse;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;

public interface StandingsResolver {

    SportCode sportCode();

    StandingsDataResponse resolve(LeagueSeasonStandingsDataDocument document);
}
