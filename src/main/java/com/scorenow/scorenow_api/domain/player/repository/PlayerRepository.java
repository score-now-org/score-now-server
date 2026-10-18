package com.scorenow.scorenow_api.domain.player.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.scorenow.scorenow_api.domain.player.entity.Player;

public interface PlayerRepository extends JpaRepository<Player, Long> {

	@Query(
		value = """
			SELECT p
			FROM Player p
			JOIN FETCH p.sport
			WHERE (:sportId IS NULL OR p.sport.id = :sportId)
			  AND (
			      :playerKeyword IS NULL
			      OR LOWER(p.kName) LIKE LOWER(CONCAT('%', :playerKeyword, '%'))
			      OR LOWER(p.eName) LIKE LOWER(CONCAT('%', :playerKeyword, '%'))
			      OR p.id = :playerId
			  )
			  AND (
			      (:leagueId IS NULL AND :teamKeyword IS NULL)
			      OR EXISTS (
			          SELECT 1
			          FROM PlayerTeamDetail ptd
			          JOIN ptd.team t
			          WHERE ptd.player = p
			            AND (:leagueId IS NULL OR ptd.league.id = :leagueId)
			            AND (
			                :teamKeyword IS NULL
			                OR LOWER(t.kName) LIKE LOWER(CONCAT('%', :teamKeyword, '%'))
			                OR LOWER(t.eName) LIKE LOWER(CONCAT('%', :teamKeyword, '%'))
			                OR t.id = :teamId
			            )
			      )
			  )
			""",
		countQuery = """
			SELECT COUNT(p)
			FROM Player p
			WHERE (:sportId IS NULL OR p.sport.id = :sportId)
			  AND (
			      :playerKeyword IS NULL
			      OR LOWER(p.kName) LIKE LOWER(CONCAT('%', :playerKeyword, '%'))
			      OR LOWER(p.eName) LIKE LOWER(CONCAT('%', :playerKeyword, '%'))
			      OR p.id = :playerId
			  )
			  AND (
			      (:leagueId IS NULL AND :teamKeyword IS NULL)
			      OR EXISTS (
			          SELECT 1
			          FROM PlayerTeamDetail ptd
			          JOIN ptd.team t
			          WHERE ptd.player = p
			            AND (:leagueId IS NULL OR ptd.league.id = :leagueId)
			            AND (
			                :teamKeyword IS NULL
			                OR LOWER(t.kName) LIKE LOWER(CONCAT('%', :teamKeyword, '%'))
			                OR LOWER(t.eName) LIKE LOWER(CONCAT('%', :teamKeyword, '%'))
			                OR t.id = :teamId
			            )
			      )
			  )
			"""
	)
	Page<Player> searchAdminPlayers(
		@Param("sportId") Long sportId,
		@Param("leagueId") Long leagueId,
		@Param("playerKeyword") String playerKeyword,
		@Param("playerId") Long playerId,
		@Param("teamKeyword") String teamKeyword,
		@Param("teamId") Long teamId,
		Pageable pageable
	);
}
