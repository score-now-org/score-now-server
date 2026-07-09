package com.scorenow.scorenow_api.domain.player.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

	@Query(value = """
			select ptd
			from PlayerTeamDetail ptd
			join fetch ptd.player p
			join fetch ptd.team t
			where ptd.squadOn = true
			  and (:playerId is null or p.id = :playerId)
			  and (:playerName is null
			       or p.kName like concat('%', :playerName, '%')
			       or lower(p.eName) like lower(concat('%', :playerName, '%')))
			  and (:teamId is null or t.id = :teamId)
			  and (:teamName is null
			       or t.kName like concat('%', :teamName, '%')
			       or lower(t.eName) like lower(concat('%', :teamName, '%')))
			order by ptd.id desc
		""",
			countQuery = """
			select count(ptd)
			from PlayerTeamDetail ptd
			join ptd.player p
			join ptd.team t
			where ptd.squadOn = true
			  and (:playerId is null or p.id = :playerId)
			  and (:playerName is null
			       or p.kName like concat('%', :playerName, '%')
			       or lower(p.eName) like lower(concat('%', :playerName, '%')))
			  and (:teamId is null or t.id = :teamId)
			  and (:teamName is null
			       or t.kName like concat('%', :teamName, '%')
			       or lower(t.eName) like lower(concat('%', :teamName, '%')))
		""")
	Page<PlayerTeamDetail> searchPlayers(
			@Param("playerId") Long playerId,
			@Param("playerName") String playerName,
			@Param("teamId") Long teamId,
			@Param("teamName") String teamName,
			Pageable pageable
	);
}