package com.scorenow.scorenow_api.domain.team.repository;

import com.scorenow.scorenow_api.domain.team.entity.TeamExternalMapping;
import com.scorenow.scorenow_api.external.common.ExternalProvider;

import java.util.Optional;

public interface TeamExternalMappingRepository {
    Optional<TeamExternalMapping> findByExternalInfo(ExternalProvider provider, String externalTeamId);

    TeamExternalMapping save(TeamExternalMapping teamExternalMapping);
}
