package com.scorenow.scorenow_api.domain.league.repository;

import com.scorenow.scorenow_api.domain.league.entity.LeagueExternalMapping;
import com.scorenow.scorenow_api.external.common.ApiProvider;

import java.util.Optional;

public interface LeagueExternalMappingRepository {
    Optional<LeagueExternalMapping> findByExternalInfo(ApiProvider provider, String apiLeagueId);

    LeagueExternalMapping save(LeagueExternalMapping leagueExternalMapping);

    Optional<LeagueExternalMapping> findByProviderAndInternalLeagueId(
            ApiProvider provider,
            Long internalLeagueId
    );
}
