package com.scorenow.scorenow_api.domain.league.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scorenow.scorenow_api.domain.league.entity.League;

public interface LeagueRepository extends JpaRepository<League, String> {
}
