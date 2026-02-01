package com.scorenow.scorenow_api.domain.team.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scorenow.scorenow_api.domain.team.entity.Team;

public interface TeamRepository extends JpaRepository<Team, String> {
}
