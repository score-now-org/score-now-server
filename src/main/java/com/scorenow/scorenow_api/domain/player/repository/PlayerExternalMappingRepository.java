package com.scorenow.scorenow_api.domain.player.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import org.springframework.data.jpa.repository.JpaRepository;

import com.scorenow.scorenow_api.domain.player.entity.PlayerExternalMapping;

public interface PlayerExternalMappingRepository extends JpaRepository<PlayerExternalMapping, Long> {

	Optional<PlayerExternalMapping> findByProviderAndApiPlayerId(
		DataOrigin provider,
		String apiPlayerId
	);

	List<PlayerExternalMapping> findByProviderAndApiPlayerIdIn(
		DataOrigin provider,
		Set<String> apiPlayerIds
	);
}
