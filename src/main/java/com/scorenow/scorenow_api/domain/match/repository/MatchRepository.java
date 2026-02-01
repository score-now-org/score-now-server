package com.scorenow.scorenow_api.domain.match.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scorenow.scorenow_api.domain.match.entity.Match;

public interface MatchRepository extends JpaRepository<Match, String> {
}
