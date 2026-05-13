package com.scorenow.scorenow_api.domain.match.repository.jpa;

import java.util.List;

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
}