package com.scorenow.scorenow_api.domain.league.repository;

import com.scorenow.scorenow_api.domain.league.entity.LeagueExternalMapping;
import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LeagueExternalMappingRepositoryImpl implements LeagueExternalMappingRepository {

    private final LeagueExternalMappingJpaRepository jpaRepository;

    @Override
    public Optional<LeagueExternalMapping> findByExternalInfo(DataOrigin dataOrigin, String apiLeagueId) {
        return jpaRepository.findByExternalInfo(dataOrigin, apiLeagueId);
    }

    @Override
    public LeagueExternalMapping save(LeagueExternalMapping leagueExternalMapping) {
        return jpaRepository.save(leagueExternalMapping);
    }

    @Override
    public Optional<LeagueExternalMapping> findByDataOriginAndInternalLeagueId(DataOrigin dataOrigin, Long internalLeagueId) {
        return jpaRepository.findByDataOriginAndInternalLeagueId(dataOrigin, internalLeagueId);
    }

    @Override
    public boolean existsByExternalInfo(DataOrigin dataOrigin, String apiLeagueId) {
        return jpaRepository.existsByDataOriginAndApiLeagueId(dataOrigin, apiLeagueId);
    }
}
