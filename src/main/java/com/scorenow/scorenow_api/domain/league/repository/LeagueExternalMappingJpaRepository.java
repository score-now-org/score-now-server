package com.scorenow.scorenow_api.domain.league.repository;

import com.scorenow.scorenow_api.domain.league.entity.LeagueExternalMapping;
import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface LeagueExternalMappingJpaRepository extends JpaRepository<LeagueExternalMapping, Long> {
    @Query("select l from LeagueExternalMapping l " +
            "where l.dataOrigin = :dataOrigin " +
            "and l.apiLeagueId = :apiLeagueId")
    Optional<LeagueExternalMapping> findByExternalInfo(DataOrigin dataOrigin, String apiLeagueId);

    Optional<LeagueExternalMapping> findByDataOriginAndInternalLeagueId(DataOrigin dataOrigin, Long internalLeagueId);

    boolean existsByDataOriginAndApiLeagueId(DataOrigin dataOrigin, String apiLeagueId);
}
