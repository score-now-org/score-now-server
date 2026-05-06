package com.scorenow.scorenow_api.domain.player.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scorenow.scorenow_api.domain.player.entity.PlayerExternalMapping;
import com.scorenow.scorenow_api.external.common.ExternalProvider;

public interface PlayerExternalMappingRepository extends JpaRepository<PlayerExternalMapping, Long> {

	Optional<PlayerExternalMapping> findByProviderAndApiPlayerId(
		ExternalProvider provider,
		String apiPlayerId
	);
}
