package com.scorenow.scorenow_api.domain.league.service.standings.extractor;

import com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument;
import com.scorenow.scorenow_api.domain.league.service.standings.model.ExternalTeamStandingKey;
import com.scorenow.scorenow_api.domain.league.service.standings.model.TeamStandingSummary;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TeamStandingExtractorRegistry {

    private final Map<SportCode, TeamStandingExtractor> extractors;

    public TeamStandingExtractorRegistry(List<TeamStandingExtractor> extractorList) {
        this.extractors = extractorList.stream()
                .collect(Collectors.toUnmodifiableMap(
                        TeamStandingExtractor::sportCode,
                        Function.identity()
                ));
    }

    public Map<ExternalTeamStandingKey, List<TeamStandingSummary>> extract(
            LeagueSeasonStandingsDataDocument leagueSeasonStandingsDataDocument,
            Map<String, LeagueSeasonStandingsDataDocument.GroupMapping> groupMappingByGroupKey) {

        TeamStandingExtractor teamStandingExtractor = extractors.get(leagueSeasonStandingsDataDocument.getSportCode());

        if (teamStandingExtractor == null) {
            log.warn(
                    "경기 목록용 팀 순위를 지원하지 않는 종목입니다. sportCode={}, leagueSeasonStandingsId={}",
                    leagueSeasonStandingsDataDocument.getSportCode(),
                    leagueSeasonStandingsDataDocument.getLeagueSeasonStandingsId());
            return Map.of();
        }

        try {
            return teamStandingExtractor.extract(leagueSeasonStandingsDataDocument, groupMappingByGroupKey);
        } catch (BusinessException e) {
            log.warn(
                    "경기 목록용 팀 순위 추출을 건너뜁니다. leagueSeasonStandingsId={}, reason={}",
                    leagueSeasonStandingsDataDocument.getLeagueSeasonStandingsId(),
                    e.getMessage());
            return Map.of();
        }
    }
}
