package com.scorenow.scorenow_api.domain.match.repository.jpa;

import java.util.List;
import java.util.Optional;

import com.scorenow.scorenow_api.domain.match.dto.response.MatchLineupPlayerResponse;

public interface MatchLineupSearchRepository {

	List<MatchLineupPlayerResponse> findPlayersByTeamId(
		Long teamId,
		int limit
	);

	List<MatchLineupPlayerResponse> searchSelectablePlayers(
		String keyword,
		int limit
	);

	Optional<MatchLineupPlayerResponse> findPlayerByIdAndTeamId(
		Long playerId,
		Long teamId
	);
}