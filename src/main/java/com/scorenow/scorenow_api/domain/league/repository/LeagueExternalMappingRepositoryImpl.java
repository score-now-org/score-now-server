package com.scorenow.scorenow_api.domain.league.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.league.entity.LeagueExternalMapping;

import lombok.RequiredArgsConstructor;

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
	public Optional<LeagueExternalMapping> findByDataOriginAndInternalLeagueId(DataOrigin dataOrigin,
		Long internalLeagueId) {
		return jpaRepository.findByDataOriginAndInternalLeagueId(dataOrigin, internalLeagueId);
	}

	@Override
	public boolean existsByExternalInfo(DataOrigin dataOrigin, String apiLeagueId) {
		return jpaRepository.existsByDataOriginAndApiLeagueId(dataOrigin, apiLeagueId);
	}

	@Override
	public List<LeagueExternalMapping> findByDataOrigin(DataOrigin dataOrigin) {
		return jpaRepository.findByDataOrigin(dataOrigin);
	}
}
