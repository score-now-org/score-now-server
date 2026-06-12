package com.scorenow.scorenow_api.domain.league.repository;

import com.scorenow.scorenow_api.domain.league.entity.LeagueExternalMapping;
import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;

import java.util.Optional;

public interface LeagueExternalMappingRepository {
    Optional<LeagueExternalMapping> findByExternalInfo(DataOrigin provider, String apiLeagueId);

    LeagueExternalMapping save(LeagueExternalMapping leagueExternalMapping);

    Optional<LeagueExternalMapping> findByProviderAndInternalLeagueId(
            DataOrigin provider,
            Long internalLeagueId
    );
}
