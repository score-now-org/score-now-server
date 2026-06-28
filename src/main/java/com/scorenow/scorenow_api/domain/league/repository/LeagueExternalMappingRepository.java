package com.scorenow.scorenow_api.domain.league.repository;

import com.scorenow.scorenow_api.domain.league.entity.LeagueExternalMapping;
import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;

import java.util.Optional;

public interface LeagueExternalMappingRepository {
    Optional<LeagueExternalMapping> findByExternalInfo(DataOrigin dataOrigin, String apiLeagueId);

    LeagueExternalMapping save(LeagueExternalMapping leagueExternalMapping);

    Optional<LeagueExternalMapping> findByDataOriginAndInternalLeagueId(
            DataOrigin dataOrigin,
            Long internalLeagueId
    );

    boolean existsByExternalInfo(DataOrigin dataOrigin, String apiLeagueId);
}
