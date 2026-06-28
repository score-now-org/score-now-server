package com.scorenow.scorenow_api.domain.player.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.scorenow.scorenow_api.domain.player.entity.PlayerTeamDetail;

public interface PlayerTeamDetailRepository extends JpaRepository<PlayerTeamDetail, Long> {

	@Query("""
			select ptd
			from PlayerTeamDetail ptd
			where ptd.team.id = :teamId
		""")
	List<PlayerTeamDetail> findByTeamId(@Param("teamId") Long teamId);

	@Query("""
			select ptd
			from PlayerTeamDetail ptd
			where ptd.player.id = :playerId
			  and ptd.team.id = :teamId
			  and ptd.league.id = :leagueId
		""")
	Optional<PlayerTeamDetail> findByPlayerIdAndTeamIdAndLeagueId(
		@Param("playerId") Long playerId,
		@Param("teamId") Long teamId,
		@Param("leagueId") Long leagueId
	);
}