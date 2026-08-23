package com.scorenow.scorenow_api.domain.league.service.standings.extractor;

import com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument;
import com.scorenow.scorenow_api.domain.league.service.standings.model.ExternalTeamStandingKey;
import com.scorenow.scorenow_api.domain.league.service.standings.model.TeamStandingSummary;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;

import java.util.List;
import java.util.Map;

public interface TeamStandingExtractor {
    SportCode sportCode();

    Map<ExternalTeamStandingKey, List<TeamStandingSummary>> extract(
            LeagueSeasonStandingsDataDocument leagueSeasonStandingsDataDocument,
            Map<String, LeagueSeasonStandingsDataDocument.GroupMapping> groupMappingByGroupKey);
}
