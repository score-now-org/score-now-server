package com.scorenow.scorenow_api.domain.league.repository;

import java.util.List;
import java.util.Optional;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.league.entity.LeagueExternalMapping;

public interface LeagueExternalMappingRepository {
	Optional<LeagueExternalMapping> findByExternalInfo(DataOrigin dataOrigin, String apiLeagueId);

	LeagueExternalMapping save(LeagueExternalMapping leagueExternalMapping);

	Optional<LeagueExternalMapping> findByDataOriginAndInternalLeagueId(
		DataOrigin dataOrigin,
		Long internalLeagueId
	);

	boolean existsByExternalInfo(DataOrigin dataOrigin, String apiLeagueId);

	List<LeagueExternalMapping> findByDataOrigin(DataOrigin dataOrigin);

	List<LeagueExternalMapping> findByInternalLeagueIdIn(List<Long> internalLeagueIds);
}
