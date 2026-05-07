package com.scorenow.scorenow_api.domain.player.repository;

import java.util.Optional;

import com.scorenow.scorenow_api.external.common.ApiProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import com.scorenow.scorenow_api.domain.player.entity.PlayerExternalMapping;

public interface PlayerExternalMappingRepository extends JpaRepository<PlayerExternalMapping, Long> {

	Optional<PlayerExternalMapping> findByProviderAndApiPlayerId(
		ApiProvider provider,
		String apiPlayerId
	);
}
