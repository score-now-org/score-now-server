package com.scorenow.scorenow_api.domain.league.repository;

import com.scorenow.scorenow_api.domain.league.entity.LeagueExternalMapping;
import com.scorenow.scorenow_api.external.common.ApiProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface LeagueExternalMappingJpaRepository extends JpaRepository<LeagueExternalMapping, Long> {
    @Query("select l from LeagueExternalMapping l " +
            "where l.provider = :provider " +
            "and l.apiLeagueId = :apiLeagueId")
    Optional<LeagueExternalMapping> findByExternalInfo(ApiProvider provider, String apiLeagueId);

    Optional<LeagueExternalMapping> findByProviderAndInternalLeagueId(ApiProvider provider, Long internalLeagueId);
}
