package com.scorenow.scorenow_api.domain.league.repository;

import com.scorenow.scorenow_api.domain.league.entity.LeagueExternalMapping;
import com.scorenow.scorenow_api.external.common.ApiProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LeagueExternalMappingRepositoryImpl implements LeagueExternalMappingRepository {

    private final LeagueExternalMappingJpaRepository jpaRepository;

    @Override
    public Optional<LeagueExternalMapping> findByExternalInfo(ApiProvider provider, String apiLeagueId) {
        return jpaRepository.findByExternalInfo(provider, apiLeagueId);
    }

    @Override
    public LeagueExternalMapping save(LeagueExternalMapping leagueExternalMapping) {
        return jpaRepository.save(leagueExternalMapping);
    }

    @Override
    public Optional<LeagueExternalMapping> findByProviderAndInternalLeagueId(ApiProvider provider, Long internalLeagueId) {
        return jpaRepository.findByProviderAndInternalLeagueId(provider, internalLeagueId);
    }
}
