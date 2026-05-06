package com.scorenow.scorenow_api.domain.player.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scorenow.scorenow_api.domain.player.entity.Player;

public interface PlayerRepository extends JpaRepository<Player, Long> {

}
