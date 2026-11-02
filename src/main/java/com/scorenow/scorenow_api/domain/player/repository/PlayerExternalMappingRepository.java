package com.scorenow.scorenow_api.domain.player.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
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

	@Query("""
		SELECT m
		FROM PlayerExternalMapping m
		WHERE m.player.id IN :playerIds
		ORDER BY m.id ASC
		""")
	List<PlayerExternalMapping> findAllByPlayerIds(
		@Param("playerIds") List<Long> playerIds
	);
}
