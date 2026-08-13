package com.scorenow.scorenow_api.domain.team.service;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.domain.team.entity.TeamExternalMapping;
import com.scorenow.scorenow_api.domain.team.repository.TeamExternalMappingRepository;
import com.scorenow.scorenow_api.domain.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamExternalLookupService {

    private final TeamRepository teamRepository;
    private final TeamExternalMappingRepository teamExternalMappingRepository;


    public Map<String, Team> findTeamsByExternalInfo(DataOrigin dataOrigin, List<String> externalTeamIds) {

        if (externalTeamIds == null || externalTeamIds.isEmpty()) {
            return Map.of();
        }

        // 외부 API ID 를 통해 매핑 정보 일괄 조회
        List<TeamExternalMapping> teamExternalMappings = teamExternalMappingRepository
                .findAllByProviderAndApiTeamIdIn(dataOrigin, externalTeamIds);

        List<Long> internalTeamIds = teamExternalMappings.stream()
                .map(TeamExternalMapping::getInternalTeamId)
                .toList();

        // 매핑 정보를 통해 내부 팀 정보 일괄 조회
        List<Team> teams = teamRepository.findAllById(internalTeamIds);

        Map<Long, Team> teamsById = teams.stream()
                .collect(Collectors.toUnmodifiableMap(
                        Team::getId,
                        Function.identity()));

        return teamExternalMappings.stream()
                .collect(Collectors.toUnmodifiableMap(
                        TeamExternalMapping::getApiTeamId,
                        teamExternalMapping -> teamsById.get(teamExternalMapping.getInternalTeamId())
                ));
    }
}
