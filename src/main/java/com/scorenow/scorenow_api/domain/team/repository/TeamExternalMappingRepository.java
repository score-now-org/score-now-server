package com.scorenow.scorenow_api.domain.team.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.scorenow.scorenow_api.domain.team.entity.TeamExternalMapping;
import com.scorenow.scorenow_api.external.common.ExternalProvider;

public interface TeamExternalMappingRepository extends JpaRepository<TeamExternalMapping, Long> {

	Optional<TeamExternalMapping> findByProviderAndApiTeamId(
		ExternalProvider provider,
		String apiTeamId
	);

	@Query("""
			select tem
			from TeamExternalMapping tem
			where tem.provider = :provider
			  and tem.team.id = :teamId
		""")
	Optional<TeamExternalMapping> findByProviderAndTeamId(
		@Param("provider") ExternalProvider provider,
		@Param("teamId") Long teamId
	);
import com.scorenow.scorenow_api.domain.team.entity.TeamExternalMapping;
import com.scorenow.scorenow_api.external.common.ExternalProvider;

import java.util.Optional;

public interface TeamExternalMappingRepository {
    Optional<TeamExternalMapping> findByExternalInfo(ExternalProvider provider, String externalTeamId);

    TeamExternalMapping save(TeamExternalMapping teamExternalMapping);
}
