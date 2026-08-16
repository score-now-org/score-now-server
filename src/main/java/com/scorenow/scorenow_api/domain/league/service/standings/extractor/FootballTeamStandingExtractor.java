package com.scorenow.scorenow_api.domain.league.service.standings.extractor;

import com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument;
import com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument.GroupMapping;
import com.scorenow.scorenow_api.domain.league.document.standings.StandingsData;
import com.scorenow.scorenow_api.domain.league.document.standings.football.FootballStandingsData;
import com.scorenow.scorenow_api.domain.league.document.standings.football.FootballStandingsData.StandingsGroup;
import com.scorenow.scorenow_api.domain.league.service.standings.model.ExternalTeamStandingKey;
import com.scorenow.scorenow_api.domain.league.service.standings.model.TeamStandingSummary;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class FootballTeamStandingExtractor implements TeamStandingExtractor {

    @Override
    public SportCode sportCode() {
        return SportCode.FOOTBALL;
    }

    // visibleInMatchList=true 인 그룹에 대해서 순위 정보들을 추출
    public Map<ExternalTeamStandingKey, List<TeamStandingSummary>> extract(
            LeagueSeasonStandingsDataDocument leagueSeasonStandingsDataDocument,
            Map<String, GroupMapping> groupMappingByGroupKey) {

        if (groupMappingByGroupKey.isEmpty()) {
            return Map.of();
        }

        FootballStandingsData footballStandingsData = resolveFootballStandingData(leagueSeasonStandingsDataDocument.getData());

        Map<ExternalTeamStandingKey, List<TeamStandingSummary>> standingsByExternalTeamKey = new HashMap<>();

        if (footballStandingsData.getGroups() == null) {
            return standingsByExternalTeamKey;
        }

        for (StandingsGroup group : footballStandingsData.getGroups()) {
            GroupMapping groupMapping = groupMappingByGroupKey.get(group.getGroupKey());

            if (groupMapping == null || group.getStandings() == null) {
                continue;
            }

            group.getStandings().forEach(standing -> standingsByExternalTeamKey.computeIfAbsent(
                            new ExternalTeamStandingKey(
                                    leagueSeasonStandingsDataDocument.getLeagueId(),
                                    leagueSeasonStandingsDataDocument.getDataOrigin(),
                                    standing.getExternalTeamId()),
                            newExternalTeamId -> new ArrayList<>())
                    .add(new TeamStandingSummary(
                            groupMapping.getDisplayName(),
                            groupMapping.getDisplayOrder(),
                            standing.getRank())));
        }

        return standingsByExternalTeamKey;
    }

    private FootballStandingsData resolveFootballStandingData(StandingsData standingsData) {
        if (!(standingsData instanceof FootballStandingsData footballStandingsData)) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "축구 순위 데이터 형식이 아닙니다.");
        }

        return footballStandingsData;
    }

}
