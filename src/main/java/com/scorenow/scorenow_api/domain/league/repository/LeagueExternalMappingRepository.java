package com.scorenow.scorenow_api.domain.league.repository;

import com.scorenow.scorenow_api.domain.league.entity.LeagueExternalMapping;
import com.scorenow.scorenow_api.external.common.ExternalProvider;

import java.util.Optional;

public interface LeagueExternalMappingRepository {
    Optional<LeagueExternalMapping> findByExternalInfo(ExternalProvider provider, String externalLeagueId);

    LeagueExternalMapping save(LeagueExternalMapping leagueExternalMapping);
}
