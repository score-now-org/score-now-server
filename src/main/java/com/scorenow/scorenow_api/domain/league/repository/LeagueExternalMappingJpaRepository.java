package com.scorenow.scorenow_api.domain.league.repository;

import com.scorenow.scorenow_api.domain.league.entity.LeagueExternalMapping;
import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface LeagueExternalMappingJpaRepository extends JpaRepository<LeagueExternalMapping, Long> {
    @Query("select l from LeagueExternalMapping l " +
            "where l.provider = :provider " +
            "and l.apiLeagueId = :apiLeagueId")
    Optional<LeagueExternalMapping> findByExternalInfo(DataOrigin provider, String apiLeagueId);

    Optional<LeagueExternalMapping> findByProviderAndInternalLeagueId(DataOrigin provider, Long internalLeagueId);
}
