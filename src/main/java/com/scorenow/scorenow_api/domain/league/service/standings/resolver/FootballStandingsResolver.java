package com.scorenow.scorenow_api.domain.league.service.standings.resolver;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument;
import com.scorenow.scorenow_api.domain.league.document.standings.football.FootballStandingsData;
import com.scorenow.scorenow_api.domain.league.dto.response.standings.football.FootballStandingsDataResponse;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.domain.team.service.TeamExternalLookupService;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.scorenow.scorenow_api.domain.league.document.LeagueSeasonStandingsDataDocument.*;
import static com.scorenow.scorenow_api.domain.league.dto.response.standings.football.FootballStandingsDataResponse.*;

@Component
@RequiredArgsConstructor
public class FootballStandingsResolver implements StandingsResolver {

    private final TeamExternalLookupService teamLookupService;

    @Override
    public SportCode sportCode() {
        return SportCode.FOOTBALL;
    }

    @Override
    public FootballStandingsDataResponse resolve(LeagueSeasonStandingsDataDocument document) {

        if (!(document.getData() instanceof FootballStandingsData footballStandingsData)) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "축구 순위 데이터 형식이 아닙니다.");
        }

        List<GroupMapping> groupMappings = document.getGroupMappings();
        Map<String, GroupMapping> mappingByGroupKey = toMappingByGroupKey(groupMappings);

        List<FootballStandingsGroup> resolvedGroups = footballStandingsData.getGroups().stream()
                .map(group -> {
                    GroupMapping groupMapping = mappingByGroupKey.get(group.getGroupKey());

                    return FootballStandingsGroup.builder()
                            .name(groupMapping.getDisplayName())
                            .order(groupMapping.getDisplayOrder())
                            .currentRound(group.getCurrentRound())
                            .maxRounds(group.getMaxRounds())
                            .standings(resolvedStandings(document.getDataOrigin(), group.getStandings()))
                            .build();
                })
                .toList();

        return FootballStandingsDataResponse.builder()
                .groups(resolvedGroups)
                .build();
    }

    private Map<String, GroupMapping> toMappingByGroupKey(List<GroupMapping> groupMappings) {
        if (groupMappings == null) {
            return Map.of();
        }

        return groupMappings.stream()
                .collect(Collectors.toUnmodifiableMap(
                        GroupMapping::getGroupKey,
                        groupMapping -> groupMapping));
    }

    private List<Standing> resolvedStandings(DataOrigin dataOrigin, List<FootballStandingsData.Standing> standings) {

        // 1. TeamLookUpService 를 통해 Map<externalTeamId, Team> 을 얻어온다.
        List<String> externalTeamIds = standings.stream()
                .map(FootballStandingsData.Standing::getExternalTeamId)
                .toList();

        Map<String, Team> teamsByExternalTeamId = teamLookupService.findTeamsByExternalInfo(dataOrigin, externalTeamIds);

        // 2. MongoDB에 저장한 (standings) 순위 정보를 순회하면서, 순위 응답을 생성한다.
        return standings.stream()
                .map(standing -> {
                    Team team = teamsByExternalTeamId.get(standing.getExternalTeamId());

                    if (team == null) {
                        throw new BusinessException(ErrorCode.TEAM_NOT_FOUND, "존재하지 않는 팀 입니다.");
                    }

                    return Standing.from(team, standing);
                })
                .toList();
    }

}
