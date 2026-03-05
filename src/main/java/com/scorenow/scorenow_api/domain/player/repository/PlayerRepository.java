package com.scorenow.scorenow_api.domain.player.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.scorenow.scorenow_api.domain.player.entity.Player;

public interface PlayerRepository extends JpaRepository<Player, String> {

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update Player p set p.squadOn = false where p.teamId = :teamId")
	int setSquadOffByTeamId(@Param("teamId") String teamId);
}
