package com.scorenow.scorenow_api.domain.team.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scorenow.scorenow_api.domain.team.entity.TeamExternalMapping;
import com.scorenow.scorenow_api.external.common.ApiProvider;

public interface TeamExternalMappingRepository extends JpaRepository<TeamExternalMapping, Long> {

	Optional<TeamExternalMapping> findByProviderAndApiTeamId(
		ApiProvider provider,
		String apiTeamId
	);

	Optional<TeamExternalMapping> findByProviderAndInternalTeamId( // 메서드명 수정 findByProviderAndTeamId
		ApiProvider provider,
		Long internalTeamId
	);

}
