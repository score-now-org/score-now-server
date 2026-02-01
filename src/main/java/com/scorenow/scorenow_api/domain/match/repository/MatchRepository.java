package com.scorenow.scorenow_api.domain.match.repository;

import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface MatchRepository extends JpaRepository<Match, String> {
    List<Match> findByStatusCode(MatchStatus statusCode);
}