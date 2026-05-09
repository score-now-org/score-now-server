package com.scorenow.scorenow_api.domain.player.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scorenow.scorenow_api.domain.player.entity.PlayerExternalMapping;
import com.scorenow.scorenow_api.external.common.ApiProvider;

public interface PlayerExternalMappingRepository extends JpaRepository<PlayerExternalMapping, Long> {

	Optional<PlayerExternalMapping> findByProviderAndApiPlayerId(
		ApiProvider provider,
		String apiPlayerId
	);

	List<PlayerExternalMapping> findByProviderAndApiPlayerIdIn(
		ApiProvider provider,
		Set<String> apiPlayerIds
	);
}
