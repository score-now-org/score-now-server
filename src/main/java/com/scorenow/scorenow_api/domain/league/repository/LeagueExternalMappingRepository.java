package com.scorenow.scorenow_api.domain.league.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scorenow.scorenow_api.domain.league.entity.LeagueExternalMapping;
import com.scorenow.scorenow_api.external.common.ApiProvider;

public interface LeagueExternalMappingRepository extends JpaRepository<LeagueExternalMapping, Long> {
	Optional<LeagueExternalMapping> findByExternalInfo(ApiProvider provider, String externalLeagueId);

	LeagueExternalMapping save(LeagueExternalMapping leagueExternalMapping);

	Optional<LeagueExternalMapping> findByProviderAndInternalLeagueId(
		ApiProvider provider,
		Long internalLeagueId
	);
}
